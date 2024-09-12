package de.starwit.odp;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

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
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.starwit.AuthTokenResponse;
import de.starwit.OffStreetParkingFunctions;
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
    
    @Value("${odp.parking.meckauer.id}")
    private String parkingSpaceId;

    private LocalDateTime tokenTimeStamp;
    private String token = "";

    // TODO List + getting from database
    private OffStreetParking offStreetParking;

    @Autowired
    private RestTemplate restTemplate;
    private ObjectMapper mapper;

    @PostConstruct
    public void init() {
        log.info("Starting ODP Adapter, connecting to " + parkingSpaceUrl);
        getAccessToken();
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
        HttpEntity<String> response = restTemplate.postForEntity(authUrl, request, String.class);

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

    @Scheduled(fixedDelay = 5000)
    private void updateParkingState() {
        checkToken();
        if(token != null) {
            log.debug("Getting update from ODP");
            getLatestOffStreetParking();
            updateParkingData();
            log.debug("Updating ODP");
            sendOffStreetParkingUpdate();
        } else {
            log.info("No valid token, can't update ODP");
        }
    }

    private void checkToken() {
        LocalDateTime now = LocalDateTime.now();
        long diff = ChronoUnit.MILLIS.between(tokenTimeStamp, now);
        log.debug("Token age " + diff);
        // no token present or token is too old, try again to aqcuire one
        if(token == null | diff > 2590000) {
            log.debug("Token too old, get a new one");
            getAccessToken();
        }
    }

    private void getLatestOffStreetParking() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("fiware-ServicePath", "/ParkingManagement");
        headers.set("fiware-service","Wolfsburg");
        headers.set("Authorization","Bearer " + token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(parkingSpaceUrl + "/" + parkingSpaceId, HttpMethod.GET, request, String.class);
        offStreetParking = OffStreetParkingFunctions.extractAvailableSpots(response.getBody());
        log.info("Get data from ODP for " + parkingSpaceId + " - " + offStreetParking.toString());
    }

    private void updateParkingData() {
        log.info("yet only fake random data");
        Random ran = new Random();
        int newAvailableSpots = ran.nextInt(offStreetParking.getTotalSpotNumber());
        offStreetParking.setAvailableParkingSpots(newAvailableSpots);
    }

    private void sendOffStreetParkingUpdate() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("fiware-ServicePath", "/ParkingManagement");
        headers.set("fiware-service","Wolfsburg");
        headers.set("Authorization","Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<String>(createAvailableSpotsRequestBody(), headers);
        ResponseEntity<String> response = restTemplate.exchange(parkingSpaceUrl + "/" + parkingSpaceId + "/attrs", HttpMethod.PATCH, request, String.class);
        log.info("Updated ODP with response code " + response.getStatusCode());
    }

    private String createAvailableSpotsRequestBody() {
        return """
            {
                "availableSpotNumber": {
                    "type": "Integer",
                    "value": ##
                }
            }""".replace("##", ""+offStreetParking.getAvailableParkingSpots());
        
    }
}