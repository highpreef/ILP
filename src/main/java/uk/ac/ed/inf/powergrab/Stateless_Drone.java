package uk.ac.ed.inf.powergrab;

import java.util.ArrayList;
import java.util.Random;

public class Stateless_Drone {
	public Position currentPosition;
	public float coins;
	public float power;
	public int move;
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
	
	public Direction makeMove() {
		if (this.move == 0) {
			getInRange();
			updateStatus();
		}
		
		this.move++;
		ArrayList<Direction> randomValidMoves = new ArrayList<>();
		ArrayList<Direction> safeMoves = new ArrayList<>();
		ArrayList<Direction> lighthousesInMoveRange = new ArrayList<>();
		
		for (Direction d : Direction.values()) {
			Position nextPos = this.currentPosition.nextPosition(d);
			if (nextPos.inPlayArea())
				randomValidMoves.add(d);
			boolean danger = false;
			
			for (POI feature : inMoveRange) {
				double distance = EuclideanDist(feature.latitude, feature.longitude, nextPos.latitude, nextPos.longitude);
				if (distance <= 0.00025 && feature.symbol.equals("lighthouse") && (feature.coins > 0 || feature.power > 0)) {
					if (nextPos.inPlayArea())
						lighthousesInMoveRange.add(d);
				} else if (distance <= 0.00025 && feature.symbol.equals("danger") && (feature.coins < 0 || feature.power < 0)) {
					danger = true;
				}	
			}
			if (!danger && nextPos.inPlayArea())
				safeMoves.add(d);
		}
		
		this.inMoveRange = new ArrayList<>();
		this.inRange = new ArrayList<>();
		
		if (!lighthousesInMoveRange.isEmpty()) {
			//choose lighthouse based on benefit
			Direction nextDir = lighthousesInMoveRange.get((int) Math.round(this.randNumbGen.nextDouble() * (lighthousesInMoveRange.size() - 1)));
			this.currentPosition = this.currentPosition.nextPosition(nextDir);
			this.power -= 2.5;
			getInRange();
			updateStatus();
			return nextDir;
		} else if (lighthousesInMoveRange.isEmpty() && !safeMoves.isEmpty()) {
			Direction nextDir = safeMoves.get((int) Math.round(this.randNumbGen.nextDouble() * (safeMoves.size() - 1)));
			this.currentPosition = this.currentPosition.nextPosition(nextDir);
			this.power -= 2.5;		
			getInRange();
			updateStatus();
			return nextDir;
		} else {
			Direction nextDir = randomValidMoves.get((int) Math.round(this.randNumbGen.nextDouble() * (randomValidMoves.size() - 1)));
			this.currentPosition = this.currentPosition.nextPosition(nextDir);
			this.power -= 2.5;
			getInRange();
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
