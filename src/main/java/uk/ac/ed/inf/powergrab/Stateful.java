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
		POI nearestPOI = null;
		double minDist = Integer.MAX_VALUE;

		for (POI feature : unvisitedPOIs) {
			double distance = euclideanDist(currentPosition.latitude, currentPosition.longitude, feature.latitude,
					feature.longitude);
			
			if (distance <= minDist) {
				minDist = distance;
				nearestPOI = feature;
			}
		}
		if (nearestPOI != null) {
			logger.finer(String.format("Next target is id %s from move %d", nearestPOI.id, move));
			target = nearestPOI;
			unvisitedPOIs.remove(target);
		} else if (nearestPOI == null) {
			target = null;
			logger.finer(String.format("Target list empty from move %d", move));
		}
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
			logger.finer(String.format("No safe directions detected during move %d", move));
			return updateState(validMoves);
		}
	}

	private Direction updateState(ArrayList<Direction> moveList) {
		Direction nextDir = moveList.get(randNumGen.nextInt(moveList.size()));
		logger.finer(String.format("Drone went in direction %s during move %d", nextDir, move));

		if (previousDir != null && hasTarget() && threshold < 3) {
			logger.finest(String.format("Backtracking detected during move %d", move));
			if (nextDir.angle == ((previousDir.angle + 180) % 361))
				threshold++;
		} else if (threshold == 3) {
			logger.finer(String.format("Continuous backtracking to previous move exceeded threshold during move %d", move));
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
				logger.finer(String.format("Drone charged from target during move %d", move));
			} else if (distanceToTarget > 0.00025) {
				double closestLighthouse = Integer.MAX_VALUE;
				POI closestPOI = null;
				
				for (POI feature : inRange) {
					double distanceToFeature = euclideanDist(feature.latitude, feature.longitude,
							currentPosition.latitude, currentPosition.longitude);
					if (distanceToFeature <= 0.00025 && !feature.id.equals(target.id)) {
						if (distanceToFeature < closestLighthouse) {
							closestLighthouse = distanceToFeature;
							closestPOI = feature;
						}
					}
				}
				if (closestPOI != null) {
					unvisitedPOIs.remove(closestPOI);
					logger.finer(String.format("Drone charged from non-target id %s during move %d", closestPOI, move));
				}
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
		move++;
		
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
					logger.finest(String.format("Detected 'safe' direction %s during move %d", d, move));
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
			logger.finer(String.format("No safe directions detected during move %d", move));
			return updateState(randomValidMoves);
		} else {
			return updateState(nextPossibleMoves);
		}
	}
}
