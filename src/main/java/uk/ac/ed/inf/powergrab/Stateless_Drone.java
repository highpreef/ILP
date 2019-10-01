package uk.ac.ed.inf.powergrab;

import java.util.ArrayList;
import java.util.Random;

public class Stateless_Drone {
	private Position currentPosition;
	private double coins;
	private double power;
	private int move;
	private Random randNumbGen;
	private ArrayList<Feature> inRange = new ArrayList<>();
	private ArrayList<Feature> inMoveRange = new ArrayList<>();
	
	public Stateless_Drone(Position initialPosition, Random randNumGen) {
		this.currentPosition = initialPosition;
		this.coins = 0;
		this.power = 250;
		this.move = 0;
		this.randNumbGen = randNumGen;
	}
	
	public void getInRange() {
		for (Feature POI : App.POI) {
			double distance = EuclideanDist(POI.latitude, POI.longitude, currentPosition.latitude, currentPosition.longitude);
			if (distance <= 0.00025)
				inRange.add(POI);
			else if (distance > 0.00025 && distance <= 0.00055)
				inMoveRange.add(POI);
		}
		return;
	}
	
	public void updateStatus() {
		for (Feature POI : inRange) {
			if (POI.symbol.equals("lighthouse")) {
				this.coins += POI.coins;
				this.power += POI.power;
				POI.coins = 0;
				POI.power = 0;
			} else if (POI.symbol.equals("danger")) {
				double coinDif = this.coins + POI.coins;
				double powerDif = this.power + POI.power;
				if (coinDif < 0) {
					this.coins = 0;
					POI.coins -= coinDif;
				} else {
					this.coins -= coinDif;
					POI.coins = 0;
				}
				if (powerDif < 0) {
					this.power = 0;
					POI.power -= powerDif;
				} else {
					this.power -= powerDif;
					POI.power = 0;
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
			for (Feature POI : inMoveRange) {
				double distance = EuclideanDist(POI.latitude, POI.longitude, nextPos.latitude, nextPos.longitude);
				if (distance <= 0.00025 && POI.symbol.equals("lighthouse") && (POI.coins > 0 || POI.power > 0)) {
					lighthousesInMoveRange.add(d);
				} else if (distance <= 0.00025 && POI.symbol.equals("danger") && (POI.coins < 0 || POI.power < 0)) {
					break;
				} else {
					possibleMoves.add(d);
				}
			}
		}
		
		if (!lighthousesInMoveRange.isEmpty()) {
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
