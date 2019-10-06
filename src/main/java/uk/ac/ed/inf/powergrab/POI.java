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
}
