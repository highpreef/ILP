package uk.ac.ed.inf.powergrab;

import java.util.ArrayList;
import java.util.Random;

public class Stateful extends Drone {
	private POI target;
	private Direction previousDir;
	private int threshold = 0;
	private Position prevPos = new Position(0, 0);
	private boolean hasJustCharged = false;
	private ArrayList<POI> unvisitedPOIs = new ArrayList<>();

	public Stateful(Position initialPosition, Random randNumGen) {
		super(initialPosition, randNumGen);
		loadTargets();
		getNextTarget();
	}

	private void loadTargets() {
		for (POI feature : App.POIs) {
			if (feature.symbol.equals("lighthouse") && (feature.coins > 0 || feature.power > 0))
				unvisitedPOIs.add(feature);
		}
		return;
	}

	private void getNextTarget() {
		POI nearestPOIs = null;
		double minDist = Integer.MAX_VALUE;

		for (POI feature : unvisitedPOIs) {
			double distance = euclideanDist(currentPosition.latitude, currentPosition.longitude, feature.latitude,
					feature.longitude);
			
			if (distance <= minDist) {
				minDist = distance;
				nearestPOIs = feature;
			}
		}
		target = nearestPOIs;
		unvisitedPOIs.remove(target);
		return;
	}

	private Direction getRandomMove() {
		ArrayList<Direction> safeMoves = new ArrayList<>();
		ArrayList<Direction> validMoves = new ArrayList<>();

		for (Direction d : Direction.values()) {
			Position nextPos = currentPosition.nextPosition(d);
			boolean danger = false;
			
			if (nextPos.inPlayArea()) {
				for (POI feature : App.POIs) {
					double distanceToFeature = euclideanDist(feature.latitude, feature.longitude, nextPos.latitude,
							nextPos.longitude);
					
					if (distanceToFeature <= 0.00025 && feature.symbol.equals("danger")
							&& (feature.coins < 0 || feature.power < 0)) {
						danger = true;
					}
				}
				if (!danger)
					safeMoves.add(d);
				validMoves.add(d);
			}
		}
		if (!safeMoves.isEmpty()) {
			return updateState(safeMoves);
		} else {
			return updateState(validMoves);
		}
	}

	private Direction updateState(ArrayList<Direction> moveList) {
		Direction nextDir = moveList.get(randNumGen.nextInt(moveList.size()));

		if (previousDir != null && hasTarget() && threshold < 3) {
			if (nextDir.angle == ((previousDir.angle + 180) % 361))
				threshold++;
		} else if (threshold == 3) {
			POI temp = target;
			getNextTarget();
			unvisitedPOIs.add(temp);
			threshold = 0;
		}
		previousDir = nextDir;
		prevPos = currentPosition;

		currentPosition = currentPosition.nextPosition(nextDir);
		power -= 2.5;
		getInRange();
		updateStatus();

		if (hasTarget()) {
			double distanceToTarget = euclideanDist(target.latitude, target.longitude, currentPosition.latitude,
					currentPosition.longitude);		
			if (distanceToTarget < 0.00025 && !(target.coins > 0 && target.power > 0)) {
				getNextTarget();
				hasJustCharged = true;
			} else if (distanceToTarget > 0.00025) {
				double closestLighthouse = Integer.MAX_VALUE;
				POI closestPOI = null;
				
				for (POI feature : App.POIs) {
					double distanceToFeature = euclideanDist(feature.latitude, feature.longitude,
							currentPosition.latitude, currentPosition.longitude);
					if (distanceToFeature <= 0.00025 && !feature.id.equals(target.id)) {
						if (distanceToFeature < closestLighthouse) {
							closestLighthouse = distanceToFeature;
							closestPOI = feature;
						}
					}
				}
				if (closestPOI != null)
					unvisitedPOIs.remove(closestPOI);
				hasJustCharged = false;
			}
		}
		return nextDir;
	}

	private boolean hasTarget() {
		return target != null;
	}

	@Override
	public Direction makeMove() {
		if (!hasTarget()) {
			return getRandomMove();
		}

		double minSafeDistToTarget = Integer.MAX_VALUE;
		ArrayList<Direction> nextPossibleMoves = new ArrayList<>();
		ArrayList<Direction> randomValidMoves = new ArrayList<>();

		for (Direction d : Direction.values()) {
			Position nextPos = currentPosition.nextPosition(d);
			double nextDistToTarget = euclideanDist(target.latitude, target.longitude, nextPos.latitude,
					nextPos.longitude);
			boolean danger = false;
			boolean lighthouse = false;

			double closestLighthouse = nextDistToTarget;
			String closestLighthouseId = target.id;
			double closestDanger = Integer.MAX_VALUE;

			if (nextPos.inPlayArea()) {
				for (POI feature : App.POIs) {
					double distanceToFeature = euclideanDist(feature.latitude, feature.longitude, nextPos.latitude,
							nextPos.longitude);
					if (distanceToFeature <= 0.00025 && feature.symbol.equals("lighthouse")
							&& (feature.coins > 0 || feature.power > 0)) {
						lighthouse = true;
						if (distanceToFeature <= closestLighthouse) {
							closestLighthouse = distanceToFeature;
							closestLighthouseId = feature.id;
						}
					} else if (distanceToFeature <= 0.00025 && feature.symbol.equals("danger")
							&& (feature.coins < 0 || feature.power < 0)) {
						danger = true;
						if (distanceToFeature <= closestDanger) {
							closestDanger = distanceToFeature;
						}
					}
				}

				if ((!danger || (danger && lighthouse && (closestLighthouse < closestDanger)))
						&& (!(nextDistToTarget <= 0.00025) || closestLighthouseId.equals(target.id))
						&& !(!hasJustCharged && nextPos.latitude == prevPos.latitude
								&& nextPos.longitude == prevPos.longitude)) {
					if (nextDistToTarget < minSafeDistToTarget) {
						minSafeDistToTarget = nextDistToTarget;
						nextPossibleMoves.clear();
						nextPossibleMoves.add(d);
					} else if (nextDistToTarget == minSafeDistToTarget) {
						nextPossibleMoves.add(d);
					}
				}
				randomValidMoves.add(d);
			}
		}

		if (nextPossibleMoves.isEmpty()) {
			return updateState(randomValidMoves);
		} else {
			return updateState(nextPossibleMoves);
		}
	}
}
