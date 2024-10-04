package de.starwit.odp;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.starwit.odp.analytics.AnalyticsRepository;
import de.starwit.odp.analytics.OccupancyDTO;
import de.starwit.odp.model.OffStreetParking;

@RestController
@RequestMapping("${rest.base-path}")
public class AdapterAPI {

    @Autowired
    private AnalyticsRepository repository;

    private Logger log = LoggerFactory.getLogger(AdapterAPI.class);

    @Autowired
    ODPFunctions functions;
    
    /**
     * This service will use initial list of parking IDs, to import data from ODP.
     */
    @GetMapping("/initial")
    public void getParkingSpacesFromOdp(){
        functions.importParkingSpaceData();
    }

    /**
     * This service enables updating parking status to ODP.
     * @return
     */
    @PutMapping("/")
    public boolean startDataTransfer() {
        functions.setSendUpdates(true);
        log.debug("Starting transfer to ODP");
        return true;
    }

    /**
     * This service disables updating parking status to ODP.
     * @return
     */
    @DeleteMapping("/")
    public boolean stopDataTransfer() {
        functions.setSendUpdates(false);
        log.debug("Stopping transfer to ODP");
        return true;
    }
    
    /**
     * List of parking spaces, that are updated to ODP.
     * @return
     */
    @GetMapping("/parkingspaces")
    public List<OffStreetParking> getParkingSpaces(){
        return functions.getParkingSpaces();
    }

    @GetMapping("/test")
    public void test() {
        var area = repository.getLatestOccupancy("american-mattress-parking-area");
        log.info("Got " + area.toString());

    }
}
