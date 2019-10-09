package uk.ac.ed.inf.powergrab;

import java.util.ArrayList;
import java.util.Random;

public class Drone {
	protected Position currentPosition;
	protected float coins;
	protected float power;
	protected int move;
	protected Random randNumGen;
	protected ArrayList<POI> inRange = new ArrayList<>();
	
	public Drone(Position initialPosition, Random randNumGen) {
		this.currentPosition = initialPosition;
		this.coins = 0;
		this.power = 250;
		this.move = 0;
		this.randNumGen = randNumGen;
	}
	
	protected void updateStatus() {
		for (POI feature : inRange) {
			if (feature.symbol.equals("lighthouse")) {
				this.coins += feature.coins;
				this.power += feature.power;
				feature.coins = 0;
				feature.power = 0;
			} else if (feature.symbol.equals("danger")) {
				double coinDif = this.coins + feature.coins;
				double powerDif = this.power + feature.power;
				if (coinDif < 0) {
					this.coins = 0;
					feature.coins -= coinDif;
				} else {
					this.coins = this.coins + feature.coins;
					feature.coins = 0;
				}
				if (powerDif < 0) {
					this.power = 0;
					feature.power -= powerDif;
				} else {
					this.power = this.power + feature.power;
					feature.power = 0;
				}
			}
		}
		return;
	}
	
	public boolean hasPower() {
		return this.power > 0;
	}
	
	protected double EuclideanDist(double xLat, double xLong, double yLat, double yLong) {
		return Math.sqrt(Math.pow(xLat - yLat, 2) + Math.pow(xLong - yLong, 2));
	}

}
