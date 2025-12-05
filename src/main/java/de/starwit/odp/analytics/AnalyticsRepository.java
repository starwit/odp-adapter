package de.starwit.odp.analytics;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
@Transactional(readOnly = true)
public class AnalyticsRepository {

    @PersistenceContext
    EntityManager entityManager;
    
    private List<String> getObservationConfigs(String areaPrefix) {
        
        String query = "SELECT name FROM public.metadata where (classification = 'observe' OR classification = 'flow') AND name like :areaPrefix";
        Query q = entityManager.createNativeQuery(query, String.class);

        q.setParameter("areaPrefix", areaPrefix + "%");
        List<String> result = q.getResultList();
        return result;
    }

    private int getLatestOccupancy(List<String> parkingAreaNames) {
        int result = 0;

        String query = "SELECT count FROM areaoccupancy "
                + "JOIN metadata ON metadata_id = metadata.id "
                + "where object_class_id = 2 AND metadata.name=:areaName "
                + "order by occupancy_time desc limit 1";
        Query q = entityManager.createNativeQuery(query, Integer.class);

        for (String name : parkingAreaNames) {
            q.setParameter("areaName", name);
            Integer occupancy = (Integer) q.getSingleResult();
            result += (occupancy != null ? occupancy : 0);
        }
        return result;
    }

    public int getParkedCars(String areaPrefix) {
        List<String> areas = getObservationConfigs(areaPrefix);
        return getLatestOccupancy(areas);
    }
}
