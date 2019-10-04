package uk.ac.ed.inf.powergrab;

import java.util.ArrayList;
import java.util.Random;

public class Stateless_Drone {
	private Position currentPosition;
	private double coins;
	private double power;
	private int move;
	private Random randNumbGen;
	private ArrayList<POI> inRange = new ArrayList<>();
	private ArrayList<POI> inMoveRange = new ArrayList<>();
	
	public Stateless_Drone(Position initialPosition, Random randNumGen) {
		this.currentPosition = initialPosition;
		this.coins = 0;
		this.power = 250;
		this.move = 0;
		this.randNumbGen = randNumGen;
	}
	
	public void getInRange() {
		for (POI feature : App.POIs) {
			double distance = EuclideanDist(feature.latitude, feature.longitude, currentPosition.latitude, currentPosition.longitude);
			if (distance <= 0.00025)
				inRange.add(feature);
			else if (distance > 0.00025 && distance <= 0.00055)
				inMoveRange.add(feature);
		}
		return;
	}
	
	public void updateStatus() {
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
					this.coins -= coinDif;
					feature.coins = 0;
				}
				if (powerDif < 0) {
					this.power = 0;
					feature.power -= powerDif;
				} else {
					this.power -= powerDif;
					feature.power = 0;
				}
			}
		}
		return;
	}
	
	public Direction makeMove() {
		getInRange();
		if (this.move == 0)
			updateStatus();
		ArrayList<Direction> possibleMoves = new ArrayList<>();
		ArrayList<Direction> lighthousesInMoveRange = new ArrayList<>();
		for (Direction d : Direction.values()) {
			Position nextPos = this.currentPosition.nextPosition(d);
			for (POI feature : inMoveRange) {
				double distance = EuclideanDist(feature.latitude, feature.longitude, nextPos.latitude, nextPos.longitude);
				if (distance <= 0.00025 && feature.symbol.equals("lighthouse") && (feature.coins > 0 || feature.power > 0)) {
					lighthousesInMoveRange.add(d);
				} else if (distance <= 0.00025 && feature.symbol.equals("danger") && (feature.coins < 0 || feature.power < 0)) {
					break;
				} else {
					possibleMoves.add(d);
				}
			}
		}
		
		if (!lighthousesInMoveRange.isEmpty()) {
			//choose lighthouse based on benefit
			Direction nextDir = lighthousesInMoveRange.get((int) Math.round(this.randNumbGen.nextDouble() * lighthousesInMoveRange.size()));
			this.currentPosition = this.currentPosition.nextPosition(nextDir);
			updateStatus();
			return nextDir;
		} else if (lighthousesInMoveRange.isEmpty() && !possibleMoves.isEmpty()) {
			Direction nextDir = possibleMoves.get((int) Math.round(this.randNumbGen.nextDouble() * possibleMoves.size()));
			this.currentPosition = this.currentPosition.nextPosition(nextDir);
			updateStatus();
			return nextDir;
		} else {
			Direction nextDir = Direction.values()[((int) Math.round(this.randNumbGen.nextDouble() * 16))];
			this.currentPosition = this.currentPosition.nextPosition(nextDir);
			updateStatus();
			return nextDir;
		}
	}
	
	public boolean hasPower() {
		return this.power > 0;
	}
	
	public double EuclideanDist(double xLat, double xLong, double yLat, double yLong) {
		return Math.sqrt(Math.pow(xLat - yLat, 2) + Math.pow(xLong - yLong, 2));
	}
}
