package uk.ac.ed.inf.powergrab;

import java.util.ArrayList;
import java.util.Random;

public class Stateless extends Drone {
	private ArrayList<POI> inMoveRange = new ArrayList<>();
	
	public Stateless(Position initialPosition, Random randNumGen) {
		super(initialPosition, randNumGen);
	}
	
	private void getInRange() {
		for (POI feature : App.POIs) {
			double distance = euclideanDist(feature.latitude, feature.longitude, currentPosition.latitude, currentPosition.longitude);
			if (distance <= 0.00025)
				inRange.add(feature);
			else if (distance > 0.00025 && distance <= 0.00055)
				inMoveRange.add(feature);
		}
		return;
	}
	
	public Direction makeMove() {	
		move++;
		
		ArrayList<Direction> randomValidMoves = new ArrayList<>();
		ArrayList<Direction> safeMoves = new ArrayList<>();
		ArrayList<Direction> lighthousesInMoveRange = new ArrayList<>();
		
		for (Direction d : Direction.values()) {
			Position nextPos = currentPosition.nextPosition(d);
			boolean danger = false;
			boolean lighthouse = false;
			
			double closestLighthouse = 0.00025;
			double closestDanger = 0.00026;
			
			if (nextPos.inPlayArea()) {
				for (POI feature : inMoveRange) {
					double distance = euclideanDist(feature.latitude, feature.longitude, nextPos.latitude, nextPos.longitude);
					if (distance <= 0.00025 && feature.symbol.equals("lighthouse") && (feature.coins > 0 || feature.power > 0)) {
						lighthouse = true;
						if (distance <= closestLighthouse) {
							closestLighthouse = distance;
						}
					} else if (distance <= 0.00025 && feature.symbol.equals("danger") && (feature.coins < 0 || feature.power < 0)) {
						danger = true;
						if (distance <= closestDanger) {
							closestDanger = distance;
						}
					}
				}
				if (lighthouse && danger) {
					if (closestLighthouse < closestDanger) {
						lighthousesInMoveRange.add(d);
					} 
				} else if (lighthouse) {
					lighthousesInMoveRange.add(d);
				}
				if (!danger)
					safeMoves.add(d);
				randomValidMoves.add(d);
			}
		}
		
		this.inMoveRange = new ArrayList<>();
		this.inRange = new ArrayList<>();
		
		if (!lighthousesInMoveRange.isEmpty()) {
			Direction nextDir = lighthousesInMoveRange.get(randNumGen.nextInt(lighthousesInMoveRange.size()));
			currentPosition = currentPosition.nextPosition(nextDir);
			power -= 2.5;
			getInRange();
			updateStatus();
			return nextDir;
		} else if (lighthousesInMoveRange.isEmpty() && !safeMoves.isEmpty()) {
			Direction nextDir = safeMoves.get(randNumGen.nextInt(safeMoves.size()));
			currentPosition = currentPosition.nextPosition(nextDir);
			power -= 2.5;		
			getInRange();
			updateStatus();
			return nextDir;
		} else {
			Direction nextDir = randomValidMoves.get(randNumGen.nextInt(randomValidMoves.size()));
			currentPosition = currentPosition.nextPosition(nextDir);
			power -= 2.5;
			getInRange();
			updateStatus();
			return nextDir;
		}
	}
}
