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

    EntityManager getEntityManager() {
        return entityManager;
    }    

    public OccupancyDTO getLatestOccupancy(String parkingAreaName) {
        String query = "SELECT "
                        + "	areaoccupancy.occupancy_time as occupancyTime, "
                        + "	areaoccupancy.count, "
                        + "	metadata.name,"
                        + "	metadata.center_longitude as longitude, "
                        + "	metadata.center_latitude as latitude  "
                        + "FROM areaoccupancy "
                        + "JOIN metadata ON metadata_id = metadata.id "
                        + "where areaoccupancy.object_class_id = 2 AND metadata.name=?1 "
                        + "order by areaoccupancy.occupancy_time desc limit 1";

        Query q = entityManager.createNativeQuery(query);
        q.setParameter(1, parkingAreaName);
        OccupancyDTO result = (OccupancyDTO) q.getSingleResult();
        
        return result;
    }
}
