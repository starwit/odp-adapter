package de.starwit.odp.analytics;

import java.math.BigDecimal;
import java.time.Instant;

public class OccupancyDTO {

    private Instant occupancyTime;
    private Integer count;
    private BigDecimal longitude;
    private BigDecimal latitude;

    public OccupancyDTO(Instant occupancyTime, Integer count, BigDecimal longitude, BigDecimal latitude) {
        this.occupancyTime = occupancyTime;
        this.count = count;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public Instant getOccupancyTime() {
        return occupancyTime;
    }

    public void setOccupancyTime(Instant occupancyTime) {
        this.occupancyTime = occupancyTime;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }
}
