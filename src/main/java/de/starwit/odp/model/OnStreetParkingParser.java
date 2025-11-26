package de.starwit.odp.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OnStreetParkingParser {

    private static Logger log = LoggerFactory.getLogger(OnStreetParkingParser.class);
    
    public static OnStreetParkingDto extractOnstreetParking(String jsonString) {
        OnStreetParkingDto result = new OnStreetParkingDto();
        ObjectMapper om = new ObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = om.readTree(jsonString);
            if (rootNode == null) {
                log.info("No data found in ODP response");
                result.setSynched(false);
                return result;
            }

            if (rootNode.isArray() && rootNode.size() > 0) {
                rootNode = rootNode.get(0);
            }

            JsonNode spotNode = rootNode.get("id");
            if(spotNode != null) {
                result.setOdpID(spotNode.asText());
            } else {
                log.info("No ID found for onstreet parking in ODP response");
                throw new JsonProcessingException("No ID found for onstreet parking in ODP response") {
                };
            }

            spotNode = rootNode.get("availableSpotNumber");
            if(spotNode != null) {
                JsonNode valueNode = spotNode.get("value");
                result.setAvailableParkingSpots(Integer.parseInt(valueNode.asText()));
            }
            spotNode = rootNode.get("totalSpotNumber");
            if(spotNode != null) {
                JsonNode valueNode = spotNode.get("value");
                result.setTotalSpotNumber(Integer.parseInt(valueNode.asText()));
            }

            spotNode = rootNode.get("disabledSpotNumber");
            if(spotNode != null) {
                JsonNode valueNode = spotNode.get("value");
                result.setDisabledSpotNumber(Integer.parseInt(valueNode.asText()));
            }

            spotNode = rootNode.get("occupiedDisabledSpotNumber");
            if(spotNode != null) {
                JsonNode valueNode = spotNode.get("value");
                result.setOccupiedDisabledSpotNumber(Integer.parseInt(valueNode.asText()));
            }

            spotNode = rootNode.get("name");
            if(spotNode != null) {
                JsonNode valueNode = spotNode.get("value");
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
