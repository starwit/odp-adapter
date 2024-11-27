package de.starwit.odp.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OnStreetParkingFunctions {

    private static Logger log = LoggerFactory.getLogger(OnStreetParkingFunctions.class);
    
    public static OnStreetParking extractOnstreetParking(String jsonString) {
        OnStreetParking result = new OnStreetParking();
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
                result.setOdpName(valueNode.asText());
            }
            result.setSynched(true);
        } catch (JsonProcessingException e) {
            log.info("Can't parse available parking spots " + e.getMessage());
            result.setSynched(false);
        } 

        return result;
    }
}
