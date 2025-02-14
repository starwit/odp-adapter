package de.starwit.odp.model;

import java.time.LocalDateTime;

public class OnStreetParking {
	/**
	 * ID in analytics DB.
	 */
	private int analyticsId;

	/**
	 * Name in Open Data Platform.
	 */
	private String odpName;

	/**
	 * ID in Open Data Platform.
	 */
	private String odpID;
	private int availableParkingSpots;
	private int totalSpotNumber;
	private boolean synched = false;

	public int getAnalyticsId() {
		return analyticsId;
	}

	public void setAnalyticsId(int analyticsId) {
		this.analyticsId = analyticsId;
	}	
	
	public String getOdpID() {
		return odpID;
	}

	public void setOdpID(String odpID) {
		this.odpID = odpID;
	}

	public String getOdpName() {
		return odpName;
	}

	public void setOdpName(String name) {
		this.odpName = name;
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

	public boolean isSynched() {
		return synched;
	}

	public void setSynched(boolean synched) {
		this.synched = synched;
	}	

	@Override
	public String toString() {
		return "OnStreetParking [name=" + odpName + ", availableParkingSpots=" + availableParkingSpots
				+ ", totalSpotNumber=" + totalSpotNumber + "]";
	}

	public void copyContent(OnStreetParking tmp) {
		if(tmp != null && tmp.synched) {
			analyticsId = tmp.analyticsId;
			odpName = tmp.odpName;
			odpID = tmp.odpID;
			availableParkingSpots = tmp.availableParkingSpots;
			totalSpotNumber = tmp.totalSpotNumber;
			synched = tmp.synched;
		}
	}	
}
