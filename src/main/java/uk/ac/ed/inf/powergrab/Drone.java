package uk.ac.ed.inf.powergrab;

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Logger;

/**
 * This abstract class implements the abstract representation of a drone. It’s
 * the superclass for the Stateless and Stateful classes, consisting of
 * non-static methods which are shared by the two subclasses and the public
 * abstract method makeMove() that is called by the App class to compute the
 * next drone move.
 * 
 * @author David Jorge (s1712653)
 *
 */
public abstract class Drone {
	/**
	 * This class has 7 protected attributes: a Position object, currentPosition,
	 * representing the drone’s current position, 2 double variables, coins and
	 * power, representing the drone’s current coin and power values respectively, a
	 * final Random object, randNumGen, representing the pseudo-random number
	 * generator used by the drone, an ArrayList of type POI, inRange, representing
	 * the features in charging range of the drone’s current position, and a Logger
	 * object, logger, used by the Drone class and its subclasses.
	 */
	protected Position currentPosition;
	protected double coins;
	protected double power;
	protected final Random randNumGen;
	protected int move;
	protected ArrayList<POI> inRange = new ArrayList<>();
	protected static Logger logger;

	/**
	 * Constructor for the abstract Drone class. It is called by the subclass
	 * Constructor, initialising all variables shared by both subclasses and setting
	 * up a subclass logger of the logger initialised in the App class. Its inputs
	 * are a Position object, representing the initial latitude and longitude of the
	 * drone, and a pseudo-random number generator object.
	 * 
	 * @param initialPosition This is the Position object representing the initial
	 *                        latitude and longitude of the drone.
	 * @param randNumGen      This is the pseudo-random number generator object.
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
	 * The abstract declaration of the makeMove() method is done in this class. This
	 * method will be called by the App class every time the drone is required to
	 * compute its next move. The concrete definition of the method is implemented
	 * by the subclasses. It returns a Direction object representing one of the 16
	 * cardinal directions the drone chose to take for the move.
	 * 
	 * @return One of the 16 cardinal directions the drone took during the move
	 *         computation.
	 */
	public abstract Direction makeMove();

	/**
	 * This method updates the drone’s current coin and power values if there is a
	 * station in charging range. It only carries out the transaction with the
	 * closest station in charging range to the drone’s current position.
	 * 
	 * If no feature is found to be in charging range of the drone’s current
	 * position this method does nothing, otherwise if the closest feature is a
	 * lighthouse, the method adds the coin and power values to the drone’s own,
	 * setting the feature’s coin and power values to 0. If the closest feature is a
	 * danger it subtracts the feature’s coin and power values from the drone’s own.
	 * The drone’s value of coins and power can’t be negative, so any excess is kept
	 * by the feature, otherwise the feature’s coin and power values are set to 0.
	 * 
	 */
	protected void updateStatus() {
		POI closestPOI = null;
		double minDist = Integer.MAX_VALUE;
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
				logger.fine(String.format("Gained %.2f coins from id %s during move %d", closestPOI.coins,
						closestPOI.id, move));
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
