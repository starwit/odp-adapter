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
import de.starwit.odp.model.OnStreetParkingDto;
import de.starwit.odp.model.OnStreetParkingParser;
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

    @Value("${odp.servicepath}")
    private String servicePath;

    @Value("${odp.parkingarea.defaulttotal}")
    private int parkingSpaceDefaultTotalSpots;

    @Value("${config.autostart}")
    private boolean sendUpdates;

    @Value("${analytics.observation_area_prefix}")
    private String observationAreaPrefix;

    @Value("${analytics.disabled_area_prefix}")
    private String disabledAreaPrefix;

    private OnStreetParkingDto onStreetParkingDto;

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
        onStreetParkingDto = getDataFromODP();
    }

    private void getAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
        map.add("realm", "default");
        map.add("client_id", "api");
        map.add("scope", "entity:read entity:write"); 
        map.add("grant_type", "password"); 
        map.add("username", username.trim());
        map.add("password", password.trim());        

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
                if(onStreetParkingDto != null && onStreetParkingDto.isSynched()) {
                    updateAvailableParkingSpots();
                    updateDisabledParkingSpots();
                }
            } else {
                log.info("No valid token, can't update ODP");
            }
        }
    }

    private void updateAvailableParkingSpots() {
        if(onStreetParkingDto.getTotalSpotNumber() - repository.getParkedCars(observationAreaPrefix) >= 0) {
            int parkedCars = repository.getParkedCars(observationAreaPrefix);
            onStreetParkingDto.setAvailableParkingSpots(onStreetParkingDto.getTotalSpotNumber() - parkedCars);
        } else {
            onStreetParkingDto.setAvailableParkingSpots(0);
        }
        sendNumber("availableSpotNumber", onStreetParkingDto.getAvailableParkingSpots());
    }

    private void updateDisabledParkingSpots() {
        if(onStreetParkingDto.getDisabledSpotNumber() - repository.getParkedCars(disabledAreaPrefix) >= 0) {
            int parkedCars = repository.getParkedCars(disabledAreaPrefix);
            onStreetParkingDto.setOccupiedDisabledSpotNumber(parkedCars);
        } else {
            onStreetParkingDto.setOccupiedDisabledSpotNumber(0);
        }
        sendNumber("occupiedDisabledSpotNumber", onStreetParkingDto.getOccupiedDisabledSpotNumber());
    }

    private void sendNumber(String fieldname, int availableSpots) {
        String odpID = onStreetParkingDto.getOdpID();
        if (odpID == null) {
            log.error("ODP ID is null, cannot update parking space for field " + fieldname + " with value " + availableSpots);
            return;
        }
        HttpHeaders headers = getHeaders();
        String body = createAvailableSpotsRequestBody(fieldname, availableSpots);
        log.debug("Request body: " + body);
        HttpEntity<String> request = new HttpEntity<String>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(parkingSpaceUrl + "/" + odpID + "/attrs", HttpMethod.PATCH, request, String.class);
        log.info("Updated parking space with response code " + response.getStatusCode());
        if(!response.getStatusCode().is2xxSuccessful()) {
            log.error("Can't update parking space " + odpID + " with value " + availableSpots + " with response code " + response.getStatusCode());
        } 
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("fiware-ServicePath", servicePath);
        headers.set("fiware-service","Wolfsburg");
        headers.set("Authorization","Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String createAvailableSpotsRequestBody(String fieldname, int availableSpots) {
        return """
            {
                "##FIELDNAME##": {
                    "type": "Integer",
                    "value": ##VALUE##
                }
            }""".replace("##FIELDNAME##", fieldname).replace("##VALUE##", "" + availableSpots);
    }

    /* ********************** Synching with ODP ************************ */
    @Scheduled(fixedRateString = "${odp.data_sync_frequency}")
    private void syncWithODP() {
        checkIfTokenIsStillValid();
        if(token != null) {
            onStreetParkingDto = getDataFromODP();
        } else {
            log.info("No valid token, can't update ODP");
        }
    }

    private OnStreetParkingDto getDataFromODP() {
        OnStreetParkingDto onStreetParking = new OnStreetParkingDto();
        onStreetParking.setSynched(false);

        HttpHeaders headers = new HttpHeaders();
        headers.set("fiware-ServicePath", servicePath);
        headers.set("fiware-service","Wolfsburg");
        headers.set("Authorization","Bearer " + token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            log.debug("Get update from " + parkingSpaceUrl);
            ResponseEntity<String> response = restTemplate.exchange(parkingSpaceUrl, HttpMethod.GET, request, String.class);    
            onStreetParking = OnStreetParkingParser.extractOnstreetParking(response.getBody());
            onStreetParking.setSynched(true);
            log.info("Get data from ODP for service path " + servicePath + " - " + onStreetParking.toString());
        } catch (HttpClientErrorException e) {
            log.info("Can't get parking space data for service path " + servicePath + " with response " + e.getStatusCode());
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