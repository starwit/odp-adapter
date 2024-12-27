package de.starwit.odp;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.starwit.odp.analytics.AnalyticsRepository;
import de.starwit.odp.model.AuthTokenResponse;
import de.starwit.odp.model.OnStreetParking;
import de.starwit.odp.model.OnStreetParkingFunctions;
import jakarta.annotation.PostConstruct;

@Service
public class ODPFunctions {

    private Logger log = LoggerFactory.getLogger(ODPFunctions.class);

    @Value("${odp.auth.username}")
    private String username;

    @Value("${odp.auth.password}")
    private String password;
    
    @Value("${odp.auth.url}")
    private String authUrl;

    @Value("${odp.parking.url}")
    private String parkingSpaceUrl;

    @Value("${odp.parkingareaid}")
    private String parkingSpaceId;

    @Value("${odp.parkingareaid.defaulttotal}")
    private int parkingSpaceDefaultTotalSpots;

    @Value("${config.autostart}")
    private boolean sendUpdates;

    private OnStreetParking ofs;

    private LocalDateTime tokenTimeStamp;
    private String token = null;

    @Autowired
    private RestTemplate restTemplate;
    private ObjectMapper mapper;

    @Autowired
    AnalyticsRepository repository;

    @PostConstruct
    public void init() {
        log.info("Starting ODP Adapter, connecting to " + parkingSpaceUrl);
        log.debug("loading mapping data");
        getAccessToken();
        ofs = getDataFromODP(parkingSpaceId);
    }

    private void getAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
        map.add("realm", "default");
        map.add("client_id", "api");
        map.add("scope", "entity:read entity:write"); 
        map.add("grant_type", "password"); 
        map.add("username", username);
        map.add("password", password);        

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
        HttpEntity<String> response;
        try {
            response = restTemplate.postForEntity(authUrl, request, String.class);
        } catch (HttpClientErrorException e) {
            log.error("Can't get access token for user " + username + " with error: " + e.getMessage());
            token = null;
            return;
        }

        mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            AuthTokenResponse authResponse = mapper.readValue(response.getBody(), AuthTokenResponse.class);
            token = authResponse.getAccessToken();
            tokenTimeStamp = LocalDateTime.now();
            log.debug("Token succesfully loaded");
        } catch (JsonProcessingException e) {
            log.error("Can't parse auth response " + e.getMessage());
        }
    }

    private void checkIfTokenIsStillValid() {
        if(token == null) {
            getAccessToken();
        } else {
            LocalDateTime now = LocalDateTime.now();
            long diff = ChronoUnit.MILLIS.between(tokenTimeStamp, now);
            log.debug("Token age " + diff);
            // token is too old, try again to aqcuire one
            if(diff > 2590000) {
                log.debug("Token too old, get a new one");
                getAccessToken();
            }
        }
    }

    /* ********************** Updating ODP ************************ */
    @Scheduled(fixedRateString = "${odp.update_frequency}")
    private void updateParkingState() {
        if(sendUpdates) {
            checkIfTokenIsStillValid();
            if(token != null) {
                if(ofs.isSynched()) {
                    if(ofs.getTotalSpotNumber() - repository.getParkedCars() >= 0) {
                        ofs.setAvailableParkingSpots(ofs.getTotalSpotNumber() - repository.getParkedCars());
                    } else {
                        ofs.setAvailableParkingSpots(0);
                    }
                    
                    sendOnStreetParkingUpdate(ofs);
                }
            } else {
                log.info("No valid token, can't update ODP");
            }
        }
    }

    private void sendOnStreetParkingUpdate(OnStreetParking ofs) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("fiware-ServicePath", "/ParkingManagement");
        headers.set("fiware-service","Wolfsburg");
        headers.set("Authorization","Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<String>(createAvailableSpotsRequestBody(ofs), headers);
        ResponseEntity<String> response = restTemplate.exchange(parkingSpaceUrl + "/" + ofs.getOdpID() + "/attrs", HttpMethod.PATCH, request, String.class);
        log.info("Updated parking space " + ofs.getOdpID() + " with remaining spaces " + ofs.getAvailableParkingSpots() + " out of " + ofs.getTotalSpotNumber() + " with response code " + response.getStatusCode());
        if(!response.getStatusCode().is2xxSuccessful()) {
            log.error("Can't update parking space " + ofs.getOdpID() + " with value " + ofs.getAvailableParkingSpots() + " with response code " + response.getStatusCode());
        } 
    }

    private String createAvailableSpotsRequestBody(OnStreetParking ofs) {
        return """
            {
                "availableSpotNumber": {
                    "type": "Integer",
                    "value": ##
                }
            }""".replace("##", "" + ofs.getAvailableParkingSpots());
    }

    /* ********************** Synching with ODP ************************ */

    private OnStreetParking getDataFromODP(String parkingSpaceId) {
        OnStreetParking onStreetParking = new OnStreetParking();
        onStreetParking.setOdpID(parkingSpaceId);
        onStreetParking.setSynched(false);

        HttpHeaders headers = new HttpHeaders();
        headers.set("fiware-ServicePath", "/ParkingManagement");
        headers.set("fiware-service","Wolfsburg");
        headers.set("Authorization","Bearer " + token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(parkingSpaceUrl + "/" + parkingSpaceId, HttpMethod.GET, request, String.class);    
            onStreetParking = OnStreetParkingFunctions.extractOnstreetParking(response.getBody());
            onStreetParking.setOdpID(parkingSpaceId);
            onStreetParking.setSynched(true);
            log.info("Get data from ODP for " + parkingSpaceId + " - " + onStreetParking.toString());
        } catch (HttpClientErrorException e) {
            log.info("Can't get parking space data for " + parkingSpaceId + " with response " + e.getStatusCode());
            onStreetParking.setTotalSpotNumber(parkingSpaceDefaultTotalSpots);
        }
        return onStreetParking;
    }

    public boolean isSendUpdates() {
        return sendUpdates;
    }

    public void setSendUpdates(boolean sendUpdates) {
        this.sendUpdates = sendUpdates;
    } 
}