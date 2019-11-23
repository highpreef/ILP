package uk.ac.ed.inf.powergrab;

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Logger;

public abstract class Drone {
	protected Position currentPosition;
	protected float coins;
	protected float power;
	protected Random randNumGen;
	protected int move;
	protected ArrayList<POI> inRange = new ArrayList<>();
	protected static Logger logger;
	
	protected Drone(Position initialPosition, Random randNumGen) {
		this.currentPosition = initialPosition;
		this.coins = 0;
		this.power = 250;
		this.randNumGen = randNumGen;
		this.move = 0;
		setupLogger();
	}
	
	private static void setupLogger() {
		logger = Logger.getLogger("App.Drone");
		return;
	}
	
	protected void getInRange() {
		for (POI feature : App.POIs) {
			double distance = euclideanDist(feature.latitude, feature.longitude, currentPosition.latitude, currentPosition.longitude);
			if (distance <= 0.00025) {
				inRange.add(feature);
				logger.finest(String.format("Station id %s in range during move %d", feature.id, move));
			}
		}
		return;
	}
	
	abstract Direction makeMove();
	
	protected void updateStatus() {
		POI closestPOI = null;
		double minDist = 0.00025;
		for (POI feature : inRange) {
			double dist = euclideanDist(feature.latitude, feature.longitude, currentPosition.latitude, currentPosition.longitude);
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
				logger.finer(String.format("Gained %f coins from id %s during move %d", closestPOI.coins, closestPOI.id, move));
				closestPOI.coins = 0;
				closestPOI.power = 0;
			} else if (closestPOI.symbol.equals("danger")) {
				double coinDif = this.coins + closestPOI.coins;
				double powerDif = this.power + closestPOI.power;
				logger.finer(String.format("Lost %f coins from id %s during move %d", -closestPOI.coins, closestPOI.id, move));
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
	
	public boolean hasPower() {
		return this.power > 0;
	}
	
	protected double euclideanDist(double xLat, double xLong, double yLat, double yLong) {
		return Math.sqrt((xLat - yLat) * (xLat - yLat) + (xLong - yLong) * (xLong - yLong));
	}

}
