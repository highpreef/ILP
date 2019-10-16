package uk.ac.ed.inf.powergrab;

import java.util.ArrayList;
import java.util.Random;

public class Stateful extends Drone {
	private POI target;
	private ArrayList<POI> unvisitedPOIs = new ArrayList<>();
	
	public Stateful(Position initialPosition, Random randNumGen) {
		super(initialPosition, randNumGen);
		loadTargets();
	}
	
	private void loadTargets() {
		for (POI feature : App.POIs) {
			double distance = euclideanDist(feature.latitude, feature.longitude, currentPosition.latitude, currentPosition.longitude);
			if (distance > 0.00025 && feature.symbol.equals("lighthouse") && (feature.coins > 0 || feature.power > 0))
				unvisitedPOIs.add(feature);
		}
		return;
	}
	
	private void getNextTarget() {
		ArrayList<POI> nearestPOIs = new ArrayList<>();
		double minDist = -1;
		
		for (POI feature : unvisitedPOIs) {
			double distance = euclideanDist(currentPosition.latitude, currentPosition.longitude, feature.latitude, feature.longitude);
			if (minDist == -1) {
				minDist = distance;
				nearestPOIs.add(feature);
			} else {
				if (distance < minDist) {
					minDist = distance;
					nearestPOIs.clear();
					nearestPOIs.add(feature);
				} else if (distance == minDist) {
					nearestPOIs.add(feature);
				}
			}
		}		
		target = nearestPOIs.get(randNumGen.nextInt(nearestPOIs.size()));
		unvisitedPOIs.remove(target);
		return;
	}
	
	public Direction makeMove() {
		move++;
		
		double distanceToTarget = euclideanDist(target.latitude, target.longitude, currentPosition.latitude, currentPosition.longitude);
		ArrayList<Direction> nextPossibleMoves = new ArrayList<>();
		
		for (Direction d : Direction.values()) {
			Position nextPos = currentPosition.nextPosition(d);
			double nextDistToTarget = euclideanDist(target.latitude, target.longitude, nextPos.latitude, nextPos.longitude);
			boolean danger = false;
			
			if (nextPos.inPlayArea()) {
				for (POI feature : App.POIs) {
					double distanceToFeature = euclideanDist(feature.latitude, feature.longitude, nextPos.latitude, nextPos.longitude);
					if (distanceToFeature <= 0.00025 && feature.symbol.equals("danger") && (feature.coins < 0 || feature.power < 0)) {
						danger = true;
					}
				}
				
				if (!danger && (nextDistToTarget < distanceToTarget))
					nextPossibleMoves.add(d);
			}
		}
		
		if (nextPossibleMoves.isEmpty()) {
			Direction nextDir = Direction.values()[randNumGen.nextInt(16)];
			currentPosition = currentPosition.nextPosition(nextDir);
			power -= 2.5;
			updateStatus();
			if (distanceToTarget < 0.00025) {
				getNextTarget();
			}
			return nextDir;
		} else {
			Direction nextDir = nextPossibleMoves.get(randNumGen.nextInt(nextPossibleMoves.size()));
			currentPosition = currentPosition.nextPosition(nextDir);
			power -= 2.5;
			updateStatus();
			if (distanceToTarget < 0.00025) {
				getNextTarget();
			}
			return nextDir;
		}		
	}
}
