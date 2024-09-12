package de.starwit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import de.starwit.odp.OffStreetParking;

public class AppTest {

    @Test
    public void parseODPResult() throws Exception {
        ClassPathResource odpResultRes = new ClassPathResource("SampleOffStreetParking.json");
        byte[] binaryData = FileCopyUtils.copyToByteArray(odpResultRes.getInputStream());
        String strJson = new String(binaryData, StandardCharsets.UTF_8);
        OffStreetParking osp = OffStreetParkingFunctions.extractAvailableSpots(strJson);

        assertTrue(osp.getAvailableParkingSpots() == 52);
        assertEquals("Meckauer Weg", osp.getName());
        assertTrue(osp.getTotalSpotNumber() == 70);
        assertTrue(osp.getLastUpdate().getYear() == 2024);
    }
}
