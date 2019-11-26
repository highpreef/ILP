package uk.ac.ed.inf.powergrab;

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Logger;

/**
 * This abstract class represents an instance of a Drone. It is responsible for
 * holding method definitions shared by the two drone types as well as defining
 * the abstract method makeMove, which is implemented in both subclasses and
 * will be called by the App class to compute the next move of the drone.
 * 
 * @author David Jorge (s1712653)
 *
 */
public abstract class Drone {
	protected Position currentPosition;
	protected float coins;
	protected float power;
	protected Random randNumGen;
	protected int move;
	protected ArrayList<POI> inRange = new ArrayList<>();
	protected static Logger logger;

	/**
	 * Constructor for the abstract Drone class. It is called by the subclass
	 * Constructor, initialising all variables shared by both subclasses and setting
	 * up a subclass logger of the logger initialised in the App class. Its inputs
	 * are a Position object, representing the initial latitude and longitude of the
	 * drone, and a random number generator object.
	 * 
	 * @param initialPosition This is the Position object representing the initial
	 *                        latitude and longitude of the drone.
	 * @param randNumGen      This is the random number generator object.
	 */
	protected Drone(Position initialPosition, Random randNumGen) {
		this.currentPosition = initialPosition;
		this.coins = 0;
		this.power = 250;
		this.randNumGen = randNumGen;
		this.move = 0;
		setupLogger();
	}

	/**
	 * This method is responsible for initialising a subclass logger of the logger
	 * class initialised in the App class. This logger object will be used for
	 * debugging and information message logging in the Drone class and its
	 * subclasses.
	 */
	private static void setupLogger() {
		logger = Logger.getLogger("App.Drone");
		return;
	}

	/**
	 * This method gets all features that are within 0.00025 degrees of the drone.
	 * It cycles through all features in the POIs ArrayList and adds all features
	 * that are 0.00025 degrees away from the current position of the drone to the
	 * inRange ArrayList. The inRange ArrayList is cleared every time this method is
	 * called.
	 */
	protected void getInRange() {
		this.inRange.clear();
		for (POI feature : App.POIs) {
			double distance = euclideanDist(feature.latitude, feature.longitude, currentPosition.latitude,
					currentPosition.longitude);
			if (distance <= 0.00025) {
				inRange.add(feature);
				logger.finest(String.format("Station id %s in range during move %d", feature.id, move));
			}
		}
		return;
	}

	/**
	 * Abstract declaration of the makeMove method. This method will be defined in
	 * the subclasses of the Drone class. It computes a move taken by the drone and
	 * outputs the direction the drone took.
	 * 
	 * @return One of the 16 cardinal directions the drone took during the move
	 *         computation.
	 */
	abstract Direction makeMove();

	/**
	 * This method updates the value of coins and power the drone has if there is a
	 * station in range. It only considers the closest station to the current
	 * position of the drone and either adds or subtracts to the value of the
	 * drone's coins and power depending on the type of station.
	 * 
	 * The closest station, represented by a POI object, to the current drone
	 * position is first calculated. If there are no stations in range this method
	 * does nothing, otherwise it either adds or subtracts the coins and power held
	 * by the station to the drones's depending on whether it's a lighthouse or
	 * danger respectively. The transaction of coins is reflected on the POI object.
	 * If the feature is a lighthouse, the value of its coins and power is set to 0.
	 * If the feature is a danger then first the coin and power difference between
	 * the feature and the drone is calculated. Then the feature's value of coins or
	 * power is set to 0 is the difference is positive (drone has more coins or
	 * power than the feature), or it is set to the value minus the difference if
	 * its negative (drone has less coins or power than the feature).
	 */
	protected void updateStatus() {
		POI closestPOI = null;
		double minDist = 0.00025;
		for (POI feature : inRange) {
			double dist = euclideanDist(feature.latitude, feature.longitude, currentPosition.latitude,
					currentPosition.longitude);
			if (dist <= minDist) {
				minDist = dist;
				closestPOI = feature;
			}
		}
		if (closestPOI != null) {
			logger.finer(String.format("Charging from id %s during move %d", closestPOI.id, move));
			if (closestPOI.symbol.equals("lighthouse")) {
				this.coins += closestPOI.coins;
				this.power += closestPOI.power;
				logger.fine(String.format("Gained %.2f coins from id %s during move %d", closestPOI.coins, closestPOI.id,
						move));
				closestPOI.coins = 0;
				closestPOI.power = 0;
			} else if (closestPOI.symbol.equals("danger")) {
				double coinDif = this.coins + closestPOI.coins;
				double powerDif = this.power + closestPOI.power;
				logger.fine(String.format("Lost %.2f coins from id %s during move %d", -closestPOI.coins, closestPOI.id,
						move));
				if (coinDif < 0) {
					this.coins = 0;
					closestPOI.coins -= coinDif;
				} else {
					this.coins = this.coins + closestPOI.coins;
					closestPOI.coins = 0;
				}
				if (powerDif < 0) {
					this.power = 0;
					closestPOI.power -= powerDif;
				} else {
					this.power = this.power + closestPOI.power;
					closestPOI.power = 0;
				}
			}
		}
		return;
	}

	/**
	 * This method returns a boolean value representing whether the drone has a
	 * power value greater than or equal to 2.5 (power necessary to make a move) or
	 * not.
	 * 
	 * @return true if the drone a power value greater than or equal to 2.5 or false
	 *         if the drone has a power value less than 2.5.
	 */
	public boolean hasPower() {
		return this.power >= 2.5;
	}

	/**
	 * Calculates the euclidean distance (L2 norm) between two 2D points.
	 * 
	 * @param xLat  This the latitude of the first point.
	 * @param xLong This is the longitude of the first point.
	 * @param yLat  This is the latitude of the second point.
	 * @param yLong This is the longitude of the second point.
	 * @return A double value representing the euclidean distance (L2 norm) between
	 *         the 2 input 2D points.
	 */
	protected double euclideanDist(double xLat, double xLong, double yLat, double yLong) {
		return Math.sqrt((xLat - yLat) * (xLat - yLat) + (xLong - yLong) * (xLong - yLong));
	}

}
