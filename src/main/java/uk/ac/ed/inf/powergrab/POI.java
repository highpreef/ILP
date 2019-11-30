package uk.ac.ed.inf.powergrab;

/**
 * This class is used to represent instances of features from the target maps.
 * 
 * @author David Jorge (s1712653)
 *
 */
public class POI {
	/**
	 * It keeps 7 public attributes describing feature characteristics. It stores
	 * the ID, the latitude and longitude, the value of coins and power, the symbol
	 * and the colour of a feature instance.
	 */
	public double latitude;
	public double longitude;
	public String id;
	public double coins;
	public double power;
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
	 * @param colour    This is the colour of the feature instance.
	 */
	public POI(String id, double latitude, double longitude, double coins, double power, String symbol, String colour) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.id = id;
		this.coins = coins;
		this.power = power;
		this.symbol = symbol;
		this.color = colour;
	}
}
