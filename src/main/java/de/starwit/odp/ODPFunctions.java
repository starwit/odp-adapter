package de.starwit.odp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.starwit.odp.analytics.AnalyticsRepository;
import de.starwit.odp.analytics.OccupancyDTO;
import de.starwit.odp.model.AuthTokenResponse;
import de.starwit.odp.model.OffStreetParking;
import de.starwit.odp.model.OffStreetParkingFunctions;
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

    @Value("${config.autostart}")
    private boolean autostart;

    private List<OffStreetParking> parkingSpaces = new ArrayList<>();

    private boolean sendUpdates = false;

    private LocalDateTime tokenTimeStamp;
    private String token = null;

    @Autowired
    private RestTemplate restTemplate;
    private ObjectMapper mapper;

    @PostConstruct
    public void init() {
        log.info("Starting ODP Adapter, connecting to " + parkingSpaceUrl);
        log.debug("loading mapping data");
        parseParkingSpaceMapping("ParkingSpaceMapping.json");
        if(autostart) {
            importParkingSpaceData();
            sendUpdates = true;
        }
    }

    public void parseParkingSpaceMapping(String mappingFilename) {
        ClassPathResource odpResultRes = new ClassPathResource(mappingFilename);
        byte[] binaryData;
        try {
            binaryData = FileCopyUtils.copyToByteArray(odpResultRes.getInputStream());
            String strJson = new String(binaryData, StandardCharsets.UTF_8);
            ObjectMapper om = new ObjectMapper();
            OffStreetParking[] parkingSpaceMapping = om.readValue(strJson,OffStreetParking[].class);
            parkingSpaces = Arrays.asList(parkingSpaceMapping);
        } catch (IOException e) {
            log.error("Can't parse initial mapping. Not a lot will work. " + e.getMessage());
        }     
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
                
            }
        }
    }

    /* ********************** Updating ODP ************************ */
    @Scheduled(fixedDelay = 5000)
    private void updateParkingState() {
        if(sendUpdates) {
            log.debug("send updates to ODP");
            checkIfTokenIsStillValid();
            if(token != null) {
                for (OffStreetParking ofs : parkingSpaces) {
                    if(ofs.isSynched()) {
                        updateParkingData(ofs);
                        sendOffStreetParkingUpdate(ofs);
                    }
                }
            } else {
                log.info("No valid token, can't update ODP");
            }
        }
    }

    /**
     * To be replaced by getting actual data.
     * @param ofs
     */
    private void updateParkingData(OffStreetParking ofs) {
        log.info("yet only fake random data");
        Random ran = new Random();
        int newAvailableSpots = ran.nextInt(ofs.getTotalSpotNumber());
        ofs.setAvailableParkingSpots(newAvailableSpots);
    }

    private void sendOffStreetParkingUpdate(OffStreetParking ofs) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("fiware-ServicePath", "/ParkingManagement");
        headers.set("fiware-service","Wolfsburg");
        headers.set("Authorization","Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<String>(createAvailableSpotsRequestBody(ofs), headers);
        ResponseEntity<String> response = restTemplate.exchange(parkingSpaceUrl + "/" + ofs.getOdpID() + "/attrs", HttpMethod.PATCH, request, String.class);
        log.debug("Updated parking space " + ofs.getOdpID() + " with response code " + response.getStatusCode());
    }

    private String createAvailableSpotsRequestBody(OffStreetParking ofs) {
        return """
            {
                "availableSpotNumber": {
                    "type": "Integer",
                    "value": ##
                }
            }""".replace("##", "" + ofs.getAvailableParkingSpots());
    }

    /* ********************** Synching with ODP ************************ */

    public void importParkingSpaceData() {
        checkIfTokenIsStillValid();
        for (OffStreetParking ofs : parkingSpaces) {
            OffStreetParking tmp = getDataFromODP(ofs.getOdpID());
            ofs.copyContent(tmp);
        }
    }

    private OffStreetParking getDataFromODP(String parkingSpaceId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("fiware-ServicePath", "/ParkingManagement");
        headers.set("fiware-service","Wolfsburg");
        headers.set("Authorization","Bearer " + token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(parkingSpaceUrl + "/" + parkingSpaceId, HttpMethod.GET, request, String.class);    
            OffStreetParking offStreetParking = OffStreetParkingFunctions.extractOffstreetParking(response.getBody());
            offStreetParking.setOdpID(parkingSpaceId);
            log.info("Get data from ODP for " + parkingSpaceId + " - " + offStreetParking.toString());
            return offStreetParking;
        } catch (HttpClientErrorException e) {
            log.info("Can't get parking space data for " + parkingSpaceId + " with response " + e.getStatusCode());
        }
        return null;
    }

    /* ********************** Managing Parking Spaces ************************ */

    public void addParkingSpace(OffStreetParking ofs) {
        parkingSpaces.add(ofs);
    }

    public void removeParkingSpace(String parkingSpaceId) {
        for (OffStreetParking ofs : parkingSpaces) {
            if(ofs.getOdpID().equals(parkingSpaceId)) {
                parkingSpaces.remove(ofs);
            }
        }
    }    

    public List<OffStreetParking> getParkingSpaces() {
        return parkingSpaces;
    }

    public boolean isSendUpdates() {
        return sendUpdates;
    }

    public void setSendUpdates(boolean sendUpdates) {
        this.sendUpdates = sendUpdates;
    } 
}