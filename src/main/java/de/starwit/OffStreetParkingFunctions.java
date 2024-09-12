package de.starwit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.starwit.odp.OffStreetParking;

public class OffStreetParkingFunctions {

    private static Logger log = LoggerFactory.getLogger(OffStreetParkingFunctions.class);
    
    public static OffStreetParking extractAvailableSpots(String jsonString) {
        OffStreetParking result = new OffStreetParking();
        ObjectMapper om = new ObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = om.readTree(jsonString);
            JsonNode spotNode = rootNode.get("availableSpotNumber");
            if(spotNode != null) {
                JsonNode valueNode = spotNode.get("value");
                result.setAvailableParkingSpots(Integer.parseInt(valueNode.asText()));
            }
            JsonNode totalSpots = rootNode.get("totalSpotNumber");
            if(totalSpots != null) {
                JsonNode valueNode = totalSpots.get("value");
                result.setTotalSpotNumber(Integer.parseInt(valueNode.asText()));
            }
            JsonNode nameNode = rootNode.get("name");
            if(totalSpots != null) {
                JsonNode valueNode = nameNode.get("value");
                result.setName(valueNode.asText());
            }
            JsonNode timestampNode = rootNode.get("dateObserved");
            if(totalSpots != null) {
                JsonNode valueNode = timestampNode.get("value");
                LocalDateTime lastUpdate = LocalDateTime.parse(valueNode.asText(), DateTimeFormatter.ofPattern ("yyyy-MM-dd'T'HH:mm:ss'Z'"));
                result.setLastUpdate(lastUpdate);
            }                      
        } catch (JsonProcessingException e) {
            log.info("Can't parse available parking spots " + e.getMessage());
        } 

        return result;
    }
}
