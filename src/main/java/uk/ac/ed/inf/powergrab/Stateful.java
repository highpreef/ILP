package uk.ac.ed.inf.powergrab;

import java.util.ArrayList;
import java.util.Random;

/**
 * This class extends the abstract Drone class. It represents the stateful drone
 * and consists of non-static methods that implement its behaviour. It also
 * defines the abstract method makeMove() in the superclass.
 * 
 * @author David Jorge (s1712653)
 *
 */
public class Stateful extends Drone {
	/**
	 * This class has 5 private attributes: a POI object, target, representing the
	 * lighthouse the drone is currently aiming to charge from, a Position object,
	 * prevPos, representing the drone’s previous position, a Boolean,
	 * hasJustCharged, representing whether or not the drone has charged from its
	 * current target in its previous move, an ArrayList, unvisitedPOIs, of type POI
	 * representing the lighthouses the drone hasn’t charged from yet, and an int,
	 * stuckCounter, representing the amount of moves the drone has taken to reach
	 * the current target.
	 */
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
	 * and longitude of the drone, and a pseudo-random number generator object.
	 * 
	 * @param initialPosition This is the Position object representing the initial
	 *                        latitude and longitude of the drone.
	 * @param randNumGen      This is the pseudo-random number generator object.
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
	 * This private method sets the class attribute representing the drone’s current
	 * target with the feature from the unvisitedPOIs ArrayList, representing the
	 * lighthouses the drone hasn’t charged from yet, that is closest to the drone’s
	 * current position. If the unvisitedPOIs class attribute is empty, this method
	 * sets the value of the attribute representing the drone’s current target to
	 * null. If the class attribute is set to a not null value, its reference is
	 * also removed from the unvisitedPOIs class attribute.
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
	 * This private method returns a Direction object representing the most
	 * favourable ‘random’ direction the drone is made to take. It computes sets of
	 * direction(s) that are possible for the drone to take at its current position,
	 * updates the attributes describing its state at the end of the move and
	 * returns a Direction object representing the direction the drone took.
	 * 
	 * From the 16 possible direction it makes sets of them for each of the
	 * following conditions which have priorities as follows, ordered from most
	 * important to least important:
	 * 
	 * 1. The drone doesn’t charge from a danger feature. 2. The drone moves to a
	 * valid position.
	 * 
	 * It calls the updateState() method with the set of directions computed to
	 * return a Direction object representing the direction the drone takes.
	 * 
	 * @return A Direction object representing a 'random' move by the drone that
	 *         aims to not charge from any feature.
	 */
	private Direction getRandomMove() {
		ArrayList<Direction> safeMoves = new ArrayList<>();
		ArrayList<Direction> validMoves = new ArrayList<>();

		for (Direction d : Direction.values()) {
			Position nextPos = currentPosition.nextPosition(d);
			boolean danger = false;
			boolean lighthouse = false;

			double closestLighthouse = Integer.MAX_VALUE;
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
						}
					} else if (distanceToFeature <= 0.00025 && feature.symbol.equals("danger")
							&& (feature.coins < 0 || feature.power < 0)) {
						danger = true;
						if (distanceToFeature <= closestDanger) {
							closestDanger = distanceToFeature;
						}
					}
				}
				if ((!danger || (danger && lighthouse && (closestLighthouse < closestDanger))))
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
	 * This private method is responsible for updating all necessary attributes of
	 * the drone after it has computed the set of possible direction(s) to go to. It
	 * takes an ArrayList of type Direction, representing the possible directions
	 * the drone can take, as dictated by the makeMove() method, as an input. It
	 * outputs a Direction object representing the final direction the drone has
	 * chosen to take.
	 * 
	 * It chooses a Direction at random, if necessary, from the input ArrayList
	 * using the pseudo-random number generator attribute. The current position of
	 * the drone is then updated, the power attribute is deducted for the move, and
	 * the getInRange() and updateStatus() methods are called to get the features
	 * now visible by the drone and to charge from the closest feature in range if
	 * applicable. It will also check if the drone is stuck on its current target if
	 * applicable. If the drone currently has a target it will check if the target
	 * has been charged from, calling the getNextTarget() method to get the next
	 * target if so. Otherwise it checks if other lighthouses, that are not the
	 * target, have been charged from, removing them from the unvisitedPOIs class
	 * attribute.
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
			logger.finer(String.format("Drone couldn't charge from target %s in 20 moves, switching to new target %s",
					temp.id, target.id));
		}
		prevPos = currentPosition;

		currentPosition = currentPosition.nextPosition(nextDir);
		power -= 1.25;
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
	 * This public method implements the abstract method declared in the superclass
	 * for the statateful drone behaviour. It computes sets of direction(s) that are
	 * possible for the drone to take at its current position, updates the
	 * attributes describing its state at the end of the move and returns a
	 * Direction object representing the direction the drone took.
	 * 
	 * From the 16 possible direction it makes sets of them for each of the
	 * following conditions which have priorities as follows, ordered from most
	 * important to least important:
	 * 
	 * 1. The drone makes progress to charge from the current target by taking that
	 * direction. 2. The drone is not in charging range of any danger feature or in
	 * charging range of a feature with no coin and power values by taking that
	 * direction. 3. The drone moves to a valid position.
	 * 
	 * The method then calls the updateState() method to update the attributes
	 * describing its current state with the most favourable non-empty set of
	 * direction(s) it computed, returning the Direction object representing the
	 * direction the drone takes in this move.
	 * 
	 */
	@Override
	public Direction makeMove() {
		move++;

		if (!hasTarget()) {
			return getRandomMove();
		}

		double minSafeDistToTarget = Integer.MAX_VALUE;
		ArrayList<Direction> movesToTarget = new ArrayList<>();
		ArrayList<Direction> randomValidMoves = new ArrayList<>();
		ArrayList<Direction> safeMoves = new ArrayList<>();

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
						&& (!(nextDistToTarget <= 0.00025) || closestLighthouseId.equals(target.id)) && (hasJustCharged
								|| !(nextPos.latitude == prevPos.latitude && nextPos.longitude == prevPos.longitude))) {
					logger.finest(String.format("Detected 'safe' direction %s during move %d", d, move));
					safeMoves.add(d);
					if (nextDistToTarget < minSafeDistToTarget) {
						minSafeDistToTarget = nextDistToTarget;
						movesToTarget.clear();
						movesToTarget.add(d);
					} else if (nextDistToTarget == minSafeDistToTarget) {
						movesToTarget.add(d);
					}
				} else if ((!danger || (danger && lighthouse && (closestLighthouse < closestDanger)))) {
					safeMoves.add(d);
				}
				randomValidMoves.add(d);
			}
		}
		if (!movesToTarget.isEmpty()) {
			return updateState(movesToTarget);
		} else if (movesToTarget.isEmpty() && !safeMoves.isEmpty()) {
			logger.finer(
					String.format("No safe directions minimizing distance to target were found during move %d", move));
			return updateState(safeMoves);
		} else {
			logger.finer(String.format("No safe directions detected during move %d", move));
			return updateState(randomValidMoves);
		}
	}
}
