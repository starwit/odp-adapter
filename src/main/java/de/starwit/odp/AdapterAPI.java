package de.starwit.odp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.base-path}")
public class AdapterAPI {

    private Logger log = LoggerFactory.getLogger(AdapterAPI.class);

    @Autowired
    ODPFunctions functions;
    
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
}
