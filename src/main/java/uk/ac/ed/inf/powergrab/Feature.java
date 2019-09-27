package uk.ac.ed.inf.powergrab;

public class Feature {
	private double latitude;
	private double longitude;
	private String id;
	private double coins;
	private double power;
	private String symbol;
	private String color;
	
	public Feature(String id, double latitude, double longitude, double coins, double power, String symbol, String color) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.id = id;
		this.coins = coins;
		this.power = power;
		this.symbol = symbol;
		this.color = color;
	}
	
	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public String getId() {
		return id;
	}

	public double getCoins() {
		return coins;
	}

	public double getPower() {
		return power;
	}

	public String getSymbol() {
		return symbol;
	}

	public String getColor() {
		return color;
	}

	public String toString() {
		String feature = String.format("ID: %s\nSymbol: %s\nCoordinates: [%f.4,%f.4]\nCoins: %f.4\nPower: %f.4\nColor: %s", id, symbol, latitude, longitude, coins, power, color);
		return feature;
	}

}
