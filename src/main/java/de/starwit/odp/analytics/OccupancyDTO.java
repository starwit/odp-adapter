package de.starwit.odp.analytics;

import java.time.ZonedDateTime;

public class OccupancyDTO {

    private ZonedDateTime occupancyTime;
    private Integer count;
    private String name;
    private double longitude;
    private double latitude;

    public ZonedDateTime getOccupancyTime() {
        return occupancyTime;
    }
    public void setOccupancyTime(ZonedDateTime occupancyTime) {
        this.occupancyTime = occupancyTime;
    }
    public Integer getCount() {
        return count;
    }
    public void setCount(Integer count) {
        this.count = count;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public double getLongitude() {
        return longitude;
    }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    public double getLatitude() {
        return latitude;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    @Override
    public String toString() {
        return "OccupancyDTO [occupancyTime=" + occupancyTime + ", count=" + count + ", name=" + name + ", longitude=" + longitude + ", latitude=" + latitude + "]";
    }    
}
