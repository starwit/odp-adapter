package de.starwit.odp;

import java.time.LocalDateTime;

public class OffStreetParking {
	private String name;
	private int availableParkingSpots;
	private int totalSpotNumber;
	private LocalDateTime lastUpdate;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAvailableParkingSpots() {
		return availableParkingSpots;
	}

	public void setAvailableParkingSpots(int availableParkingSpots) {
		this.availableParkingSpots = availableParkingSpots;
	}

	public int getTotalSpotNumber() {
		return totalSpotNumber;
	}

	public void setTotalSpotNumber(int totalSpotNumber) {
		this.totalSpotNumber = totalSpotNumber;
	}

	public LocalDateTime getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(LocalDateTime lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	@Override
	public String toString() {
		return "OffStreetParking [name=" + name + ", availableParkingSpots=" + availableParkingSpots
				+ ", totalSpotNumber=" + totalSpotNumber + ", lastUpdate=" + lastUpdate + "]";
	}
}
