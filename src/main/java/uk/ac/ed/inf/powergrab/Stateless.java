package uk.ac.ed.inf.powergrab;

import java.util.ArrayList;
import java.util.Random;

public class Stateless extends Drone {
	private ArrayList<POI> inMoveRange = new ArrayList<>();

	public Stateless(Position initialPosition, Random randNumGen) {
		super(initialPosition, randNumGen);
		getInRange();
	}

	@Override
	protected void getInRange() {
		this.inRange.clear();
		this.inMoveRange.clear();
		for (POI feature : App.POIs) {
			double distance = euclideanDist(feature.latitude, feature.longitude, currentPosition.latitude,
					currentPosition.longitude);
			if (distance <= 0.00025) {
				inRange.add(feature);
				logger.finest(String.format("Station id %s in range during move %d", feature.id, move));
			} else if (distance > 0.00025 && distance <= 0.00055) {
				inMoveRange.add(feature);
				logger.finest(String.format("Station id %s in move range during move %d", feature.id, move));
			}
		}
		return;
	}

	private Direction updateState(ArrayList<Direction> moveList) {
		Direction nextDir = moveList.get(randNumGen.nextInt(moveList.size()));
		logger.finer(String.format("Drone went in direction %s during move %d", nextDir, move));
		currentPosition = currentPosition.nextPosition(nextDir);
		power -= 2.5;
		getInRange();
		updateStatus();
		return nextDir;
	}

	@Override
	public Direction makeMove() {
		move++;
		
		ArrayList<Direction> randomValidMoves = new ArrayList<>();
		ArrayList<Direction> safeMoves = new ArrayList<>();
		ArrayList<Direction> lighthousesInMoveRange = new ArrayList<>();

		for (Direction d : Direction.values()) {
			Position nextPos = currentPosition.nextPosition(d);
			boolean danger = false;
			boolean lighthouse = false;

			double closestLighthouse = Integer.MAX_VALUE;
			double closestDanger = Integer.MAX_VALUE;

			if (nextPos.inPlayArea()) {
				for (POI feature : inMoveRange) {
					double distance = euclideanDist(feature.latitude, feature.longitude, nextPos.latitude,
							nextPos.longitude);
					if (distance <= 0.00025 && feature.symbol.equals("lighthouse")
							&& (feature.coins > 0 || feature.power > 0)) {
						lighthouse = true;
						if (distance <= closestLighthouse) {
							closestLighthouse = distance;
						}
					} else if (distance <= 0.00025 && feature.symbol.equals("danger")
							&& (feature.coins < 0 || feature.power < 0)) {
						danger = true;
						if (distance <= closestDanger) {
							closestDanger = distance;
						}
					}
				}
				if ((lighthouse && danger && closestLighthouse < closestDanger) || (lighthouse && !danger)) {
					logger.finest(String.format("Detected 'safe' lighthouse(s) in direction %s during move %d", d, move));
					lighthousesInMoveRange.add(d);
				} else if (!danger) {
					safeMoves.add(d);
				}
				randomValidMoves.add(d);
			}
		}

		if (!lighthousesInMoveRange.isEmpty()) {
			return updateState(lighthousesInMoveRange);
		} else if (lighthousesInMoveRange.isEmpty() && !safeMoves.isEmpty()) {
			return updateState(safeMoves);
		} else {
			logger.finer(String.format("No safe directions detected during move %d", move));
			return updateState(randomValidMoves);
		}
	}
}
