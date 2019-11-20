package uk.ac.ed.inf.powergrab;

import java.util.ArrayList;
import java.util.Random;

public abstract class Drone {
	protected Position currentPosition;
	protected float coins;
	protected float power;
	protected Random randNumGen;
	protected ArrayList<POI> inRange = new ArrayList<>();
	
	protected Drone(Position initialPosition, Random randNumGen) {
		this.currentPosition = initialPosition;
		this.coins = 0;
		this.power = 250;
		this.randNumGen = randNumGen;
	}
	
	protected void getInRange() {
		for (POI feature : App.POIs) {
			double distance = euclideanDist(feature.latitude, feature.longitude, currentPosition.latitude, currentPosition.longitude);
			if (distance <= 0.00025)
				inRange.add(feature);
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
			if (closestPOI.symbol.equals("lighthouse")) {
				this.coins += closestPOI.coins;
				this.power += closestPOI.power;
				closestPOI.coins = 0;
				closestPOI.power = 0;
			} else if (closestPOI.symbol.equals("danger")) {
				double coinDif = this.coins + closestPOI.coins;
				double powerDif = this.power + closestPOI.power;
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
