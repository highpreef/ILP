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
	private Position prevPos = new Position(0, 0);
	private boolean hasJustCharged = false;
	private ArrayList<POI> unvisitedPOIs = new ArrayList<>();
	private int stuckCounter = 0;

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
	 * direction, and the validMove ArrayList if the drone is still within the
	 * playing area after taking that direction.
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
	 * takes. If the drone hasn't reached its target in 20 moves, a new randomly
	 * chosen target will be loaded. The chosen direction and position are then
	 * stored, followed by updating the next position of the drone with the
	 * nextPosition method of the Position class, the power value is deducted for
	 * the move, and the getInRange and updateStatus methods are called to update
	 * the stations now visible by the drone and charge from the closest station in
	 * range if one exists.
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

		if (stuckCounter == 20 && !unvisitedPOIs.isEmpty()) {
			POI temp = target;
			target = unvisitedPOIs.get(randNumGen.nextInt(unvisitedPOIs.size()));
			unvisitedPOIs.add(temp);
			stuckCounter = 0;
			logger.finer(String.format("Drone wasn't able to charge from target %s in 20 moves, switching to new target %s", temp.id, target.id));
		}
		prevPos = currentPosition;

		currentPosition = currentPosition.nextPosition(nextDir);
		power -= 2.5;
		getInRange();
		updateStatus();

		if (hasTarget()) {
			if (!(target.coins > 0 && target.power > 0)) {
				getNextTarget();
				hasJustCharged = true;
				stuckCounter = 0;
				logger.finer(String.format("Drone charged from target during move %d", move));
			} else {
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
				stuckCounter += 1;
			}
		}
		return nextDir;
	}

	/**
	 * This method returns a boolean value representing whether the drone currently
	 * has a target or not.
	 * 
	 * @return true if the drone target is not null or false if the drone target is
	 *         null.
	 */
	private boolean hasTarget() {
		return target != null;
	}

	/**
	 * This method implements the abstract method declared in the superclass. It
	 * computes the direction the drone takes during its next move. If the drone
	 * currently doesn't have a target it returns the value from the getRandomMove
	 * method, otherwise 2 ArrayLists are initialised: nextPossibleMoves,
	 * representing a set of directions that minimise the distance from the drone to
	 * the target while ensuring that the drone doesn't charge from a danger station
	 * after its move, and randomValidMoves, representing a set of directions that
	 * are valid for the drone to take.
	 * 
	 * The 16 cardinals directions are then iterated in a for loop. For each
	 * direction the next position the drone would be in is computed and then all
	 * features in the POIs ArrayList are iterated if the next position is within
	 * the playing area. For each feature it is checked whether that feature is a
	 * lighthouse or a danger and is within 0.00025 degrees of the next position,
	 * asserting a respective boolean in that case. For each direction the distance
	 * to the closest lighthouse and closest danger is also computed along with ID
	 * of the closest lighthouse. For each direction the nextPossibleMoves ArrayList
	 * is updated if a set of conditions is passed: There is no danger stations in
	 * range or there are danger(s) and lighthouse(s) in range but the closest one
	 * to the drone is a lighthouse, and if the target is in range the closest
	 * lighthouse is the target, and if the drone hasn't just charged from its
	 * target it doesn't backtrack to the position in its previous move. If these
	 * conditions are passed then that direction is added to the nextPossibleMoves
	 * ArrayList if the distance to the target is equal to the current shortest
	 * distance, otherwise if the distance to the target is smaller the
	 * nextPossibleMoves ArrayList is cleared first before adding to it. The
	 * direction is also added to the randomValidMoves ArrayList if the drone is
	 * still within the playing area after taking that direction.
	 * 
	 * The updateState method is then called with one of the 2 ArrayLists. If the
	 * nextPossibleMoves ArrayList is not empty the updateStatus method is called
	 * with it as its input, otherwise the updateStatus method is called with the
	 * randomValidMoves ArrayList as its input.
	 */
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
