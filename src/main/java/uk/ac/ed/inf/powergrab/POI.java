package uk.ac.ed.inf.powergrab;

/**
 * This class is used for representing and holding the values of a feature
 * instance described by the target map.
 * 
 * @author David Jorge (s1712653)
 *
 */
public class POI {
	public double latitude;
	public double longitude;
	public String id;
	public float coins;
	public float power;
	public String symbol;
	public String color;

	/**
	 * Constructor for the POI class. It stores the ID, the latitude and longitude,
	 * the value of coins and power, the symbol and the colour of the feature
	 * instance.
	 * 
	 * @param id        This is the ID of the feature instance.
	 * @param latitude  This is the latitude of the feature instance.
	 * @param longitude This is the longitude of the feature instance.
	 * @param coins     This is the coins held by the feature instance.
	 * @param power     This is the power held by the feature instance.
	 * @param symbol    This is the symbol of the feature instance.
	 * @param color     This is the colour of the feature instance.
	 */
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
