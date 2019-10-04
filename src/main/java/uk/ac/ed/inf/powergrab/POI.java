package uk.ac.ed.inf.powergrab;

public class POI {
	public double latitude;
	public double longitude;
	public String id;
	public float coins;
	public float power;
	public String symbol;
	public String color;
	
	public POI(String id, double latitude, double longitude, float coins, float power, String symbol, String color) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.id = id;
		this.coins = coins;
		this.power = power;
		this.symbol = symbol;
		this.color = color;
	}
	
	public String toString() {
		String feature = String.format("ID: %s\nSymbol: %s\nCoordinates: [%f.4,%f.4]\nCoins: %f.4\nPower: %f.4\nColor: %s", id, symbol, latitude, longitude, coins, power, color);
		return feature;
	}
}
