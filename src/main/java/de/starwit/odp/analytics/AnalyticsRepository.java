package de.starwit.odp.analytics;

import java.math.BigInteger;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
@Transactional(readOnly = true)
public class AnalyticsRepository {

    private Logger log = LoggerFactory.getLogger(AnalyticsRepository.class);

    @Value("${analytics.observation_area}")
    private String observationArea;

    @PersistenceContext
    EntityManager entityManager;

    EntityManager getEntityManager() {
        return entityManager;
    }
    
    private List<String> getAllObservationConfigs() {
        String query = "SELECT name FROM public.metadata where classification = 'observe' AND observation_area_id=?1";
        Query q = entityManager.createNativeQuery(query, String.class);
        log.info(observationArea);
        
        q.setParameter(1, BigInteger.valueOf(Long.parseLong(observationArea)));
        List<String> result = (List<String>) q.getResultList();
        return result;
    }

    private int getLatestOccupancy(List<String> parkingAreaNames) {
        int result = 0;
        String query = "SELECT "
                        + "	areaoccupancy.occupancy_time as occupancyTime, "
                        + "	areaoccupancy.count as count, "
                        + "	metadata.center_longitude as longitude, "
                        + "	metadata.center_latitude as latitude  "
                        + "FROM areaoccupancy "
                        + "JOIN metadata ON metadata_id = metadata.id "
                        + "where areaoccupancy.object_class_id = 2 AND metadata.observation_area_id=?1 AND metadata.name=?2 "
                        + "order by areaoccupancy.occupancy_time desc limit 1";

        Query q = entityManager.createNativeQuery(query, OccupancyDTO.class);
        q.setParameter(1, BigInteger.valueOf(Long.parseLong(observationArea)));
        for (String name : parkingAreaNames) {
            q.setParameter(2, name);
            OccupancyDTO areaData = (OccupancyDTO) q.getSingleResult();
            result += areaData.getCount();
        }
        return result;
    }

    public int getParkedCars() {
        List<String> areas = getAllObservationConfigs();
        return getLatestOccupancy(areas);
    }
}
