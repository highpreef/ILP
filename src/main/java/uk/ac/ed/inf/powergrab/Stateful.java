package uk.ac.ed.inf.powergrab;

import java.util.ArrayList;
import java.util.Random;

/**
 * This class extends the abstract Drone class. It represents the Stateful drone
 * type. It implements the makeMove method and defines any necessary support
 * methods.
 * 
 * @author David Jorge (s1712653)
 *
 */
public class Stateful extends Drone {
	private POI target;
	private Direction previousDir;
	private int threshold = 0;
	private Position prevPos = new Position(0, 0);
	private boolean hasJustCharged = false;
	private ArrayList<POI> unvisitedPOIs = new ArrayList<>();

	/**
	 * Constructor of the Stateful drone class. It is executed when a new instance
	 * of the Stateful class is initialised, calling the superclass constructor and
	 * the loadTargets and getNextTarget methods, which initialise the starting
	 * target. Its inputs are a Position object, representing the initial latitude
	 * and longitude of the drone, and a random number generator object.
	 * 
	 * @param initialPosition This is the Position object representing the initial
	 *                        latitude and longitude of the drone.
	 * @param randNumGen      This is the random number generator object.
	 */
	public Stateful(Position initialPosition, Random randNumGen) {
		super(initialPosition, randNumGen);
		loadTargets();
		getNextTarget();
	}

	/**
	 * This method loads the targets (lighthouses with a positive value of either
	 * coins or power). The POIs ArrayList is iterated to check for all lighthouses
	 * with positive values of either coins or power, adding any features that pass
	 * the condition to the unvisitedPOIs ArrayList, representing the targets that
	 * are still yet to be visited.
	 */
	private void loadTargets() {
		for (POI feature : App.POIs) {
			if (feature.symbol.equals("lighthouse") && (feature.coins > 0 || feature.power > 0))
				unvisitedPOIs.add(feature);
		}
		return;
	}

	/**
	 * This method sets the current target of the drone to the closest target. It
	 * first iterates through the features in the unvisitedPOIs ArrayList,
	 * representing all targets yet to be visited, and sets the current target to
	 * the closest target to the current position of the drone if there are still
	 * targets left, otherwise the current target is set to null.
	 */
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

	/**
	 * This method computes the next direction the drone should should take focusing
	 * only on avoiding danger stations. Two ArrayLists are initialised: safeMoves,
	 * representing a set of directions where the drone won't charge to a danger
	 * station after its move, and validMoves, representing a set of directions that
	 * are valid for the drone to take.
	 * 
	 * The 16 cardinal directions are then iterated in a for loop. For each
	 * direction the next position the drone would be in is computed and then all
	 * features in the POIs ArrayList are iterated if the next position is within
	 * the playing area. For each feature it is checked whether that feature is
	 * danger and is within 0.00025 degrees of the next position, asserting a
	 * boolean in that case. For each direction its value is added to the safeMoves
	 * ArrayList if the drone doesn't charge from a danger after taking that
	 * direction, and the validMoves if the drone is still within the playing area
	 * after taking that direction.
	 * 
	 * The updateState method is then called with one of the 2 ArrayLists. If the
	 * safeMoves ArrayList is not empty the updateStatus method is called with it as
	 * its input, otherwise if it's empty then the updateStatus method is called
	 * with the validMoves ArrayList as its input.
	 * 
	 * @return
	 */
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

	/**
	 * This method is responsible for updating all aspects of the drone after it has
	 * made a move. It takes an ArrayList moveList, representing the possible
	 * directions the drone can take dictated by the makeMove or getRandomMove
	 * methods, as an input. It outputs a direction, which represents the direction
	 * the drone has been made to take. It is also responsible for checking whether
	 * the drone has gone back and forth from the same positions multiple times,
	 * changing the current target in that case. This method is called by the
	 * makeMove method.
	 * 
	 * If the size of the input moveList ArrayList is larger than 1 the pseudo
	 * random number generator is used to randomly choose which direction the drone
	 * takes. If the drone is not in its first move then this method also checks
	 * whether the drone has gone back and forth between the same 2 positions 3
	 * times. If that happens the current target is put back into the unvisitedPOIs
	 * ArrayList and the next closest target is selected. The chosen direction and
	 * position are then stored, followed by updating the next position of the drone
	 * with the nextPosition method of the Position class, the power value is
	 * deducted for the move, and the getInRange and updateStatus methods are called
	 * to update the stations now visible by the drone and charge from the closest
	 * station in range if one exists.
	 * 
	 * If the drone currently has a target then this method check if the target has
	 * been charged from during the move, if so it calls the getNextTarget method
	 * and asserts the hasJustCharged boolean to signal that the drone has just
	 * charged from its target, otherwise it checks if any other lighthouses that
	 * are not the target have been charged from during the move, if so it removes
	 * that lighthouse from the unvisitedPOIs ArrayList and sets the hasJustCharged
	 * boolean to false signalling that the current target has not yet been charged
	 * from.
	 * 
	 * @param moveList This is the ArrayList of directions representing the possible
	 *                 directions the drone can take dictated by the makeMove or the
	 *                 getRandomMove methods.
	 * @return The direction the drone has been made to take.
	 */
	private Direction updateState(ArrayList<Direction> moveList) {
		Direction nextDir = moveList.get(randNumGen.nextInt(moveList.size()));
		logger.finer(String.format("Drone went in direction %s during move %d", nextDir, move));

		if (previousDir != null && hasTarget() && threshold < 3) {
			logger.finest(String.format("Backtracking detected during move %d", move));
			if (nextDir.angle == ((previousDir.angle + 180) % 361))
				threshold++;
		} else if (threshold == 3) {
			logger.finer(
					String.format("Continuous backtracking to previous move exceeded threshold during move %d", move));
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

	/**
	 * This method
	 * @return
	 */
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
