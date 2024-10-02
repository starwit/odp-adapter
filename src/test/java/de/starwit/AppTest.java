package de.starwit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import de.starwit.odp.ODPFunctions;
import de.starwit.odp.model.OffStreetParking;
import de.starwit.odp.model.OffStreetParkingFunctions;

public class AppTest {

    @Test
    public void parseODPResult() throws Exception {
        ClassPathResource odpResultRes = new ClassPathResource("SampleOffStreetParking.json");
        byte[] binaryData = FileCopyUtils.copyToByteArray(odpResultRes.getInputStream());
        String strJson = new String(binaryData, StandardCharsets.UTF_8);
        OffStreetParking osp = OffStreetParkingFunctions.extractOffstreetParking(strJson);

        assertTrue(osp.getAvailableParkingSpots() == 52);
        assertEquals("Meckauer Weg", osp.getOdpName());
        assertTrue(osp.getTotalSpotNumber() == 70);
        assertTrue(osp.getLastUpdate().getYear() == 2024);
    }

    @Test
    public void parseMappingConfig() throws Exception {
        ODPFunctions of = new ODPFunctions();
        of.parseParkingSpaceMapping("ParkingSpaceMapping.json");
        assertEquals(2,of.getParkingSpaces().size());
    }
}
