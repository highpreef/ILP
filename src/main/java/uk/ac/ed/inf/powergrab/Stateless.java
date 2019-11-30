package uk.ac.ed.inf.powergrab;

import java.util.ArrayList;
import java.util.Random;

/**
 * This class extends the abstract Drone class. It represents the stateless
 * drone and consists of non-static methods that implement its behaviour. It
 * also defines the abstract method makeMove() in the superclass.
 * 
 * @author David Jorge (s1712653)
 *
 */
public class Stateless extends Drone {
	/**
	 * It keeps one private attribute ArrayList, inMoveRange, of type POI
	 * representing all features that could be in charging range after a single
	 * move.
	 */
	private ArrayList<POI> inMoveRange = new ArrayList<>();

	/**
	 * Constructor of the Stateless drone class. It is executed when a new instance
	 * of the Stateless class is initialised, calling the superclass constructor and
	 * the overridden getInRange method. Its inputs are a Position object,
	 * representing the initial latitude and longitude of the drone, and a
	 * pseudo-random number generator object.
	 * 
	 * @param initialPosition This is the Position object representing the initial
	 *                        latitude and longitude of the drone.
	 * @param randNumGen      This is the pseudo-random number generator object.
	 */
	public Stateless(Position initialPosition, Random randNumGen) {
		super(initialPosition, randNumGen);
		getInRange();
	}

	/**
	 * This method overrides the one defined in the superclass. Apart from storing
	 * all features that are within 0.00025 degrees of the drone’s current position,
	 * it also stores all features that are further than 0.00025 degrees away but
	 * less than 0.00055 degrees (move distance plus charging distance) in the
	 * inMoveRange ArrayList kept as an attribute by this class. This represents the
	 * features that could be in charging range after the drone has made a single
	 * move.
	 */
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

	/**
	 * This private method is responsible for updating all necessary attributes of
	 * the drone after it has computed which direction(s) to go in. It takes an
	 * ArrayList of type Direction, representing the set of possible directions the
	 * drone can take, as dictated by the makeMove() method, as an input. It outputs
	 * a Direction object representing the final direction the drone has chosen to
	 * take.
	 * 
	 * It chooses a Direction at random, if necessary, from the input ArrayList
	 * using the pseudo-random number generator attribute. The current position of
	 * the drone is then updated with the nextPosition() method of the Position
	 * class, the power attribute is deducted for the move, and the getInRange() and
	 * updateStatus() methods are called to get the features now visible by the
	 * drone and to charge from the closest feature in range if applicable.
	 * 
	 * 
	 * @param moveList This is the ArrayList of directions representing the possible
	 *                 directions the drone can take dictated by the makeMove
	 *                 method.
	 * @return The direction the drone has been made to take.
	 */
	private Direction updateState(ArrayList<Direction> moveList) {
		Direction nextDir = moveList.get(randNumGen.nextInt(moveList.size()));
		logger.finer(String.format("Drone went in direction %s during move %d", nextDir, move));
		currentPosition = currentPosition.nextPosition(nextDir);
		power -= 2.5;
		getInRange();
		updateStatus();
		return nextDir;
	}

	/**
	 * This public method implements the abstract method declared in the superclass
	 * for the stateless drone behaviour. It computes sets of direction(s) that are
	 * possible for the drone to take at its current position, updates the
	 * attributes describing its state at the end of the move and returns a
	 * Direction object representing the direction the drone took.
	 * 
	 * From the 16 possible direction it makes sets of them for each of the
	 * following conditions which have priorities as follows, ordered from most
	 * important to least important:
	 * 
	 * 1. The drone charges to a lighthouse by taking that direction. 2. The drone
	 * is not in charging range of any feature or in charging range of a feature
	 * with no coin and power values by taking that direction. 3. The drone moves to
	 * a valid position.
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
					logger.finest(
							String.format("Detected 'safe' lighthouse(s) in direction %s during move %d", d, move));
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
