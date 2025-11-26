package de.starwit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import de.starwit.odp.model.OnStreetParkingDto;
import de.starwit.odp.model.OnStreetParkingParser;

public class AppTest {

    @Test
    public void parseODPResult() throws Exception {
        ClassPathResource odpResultRes = new ClassPathResource("SampleOnStreetParking.json");
        byte[] binaryData = FileCopyUtils.copyToByteArray(odpResultRes.getInputStream());
        String strJson = new String(binaryData, StandardCharsets.UTF_8);
        OnStreetParkingDto osp = OnStreetParkingParser.extractOnstreetParking(strJson);

        assertTrue(osp.getAvailableParkingSpots() == 39);
        assertEquals("38444039", osp.getOdpName());
        assertTrue(osp.getTotalSpotNumber() == 70);
    }
}
