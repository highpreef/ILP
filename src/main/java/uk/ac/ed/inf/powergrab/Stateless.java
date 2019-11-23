package uk.ac.ed.inf.powergrab;

import java.util.ArrayList;
import java.util.Random;

/**
 * This class extends the abstract Drone class. It represents the Stateless
 * drone type. It overrides the getInRange method, implements the makeMove
 * method and defines any necessary support methods.
 * 
 * @author David Jorge (s1712653)
 *
 */
public class Stateless extends Drone {
	private ArrayList<POI> inMoveRange = new ArrayList<>();

	/**
	 * Constructor of the Stateless drone class. It is executed when a new instance
	 * of the Stateless class is initialised, calling the superclass constructor and
	 * the overridden getInRange method. Its inputs are a Position object,
	 * representing the initial latitude and longitude of the drone, and a random
	 * number generator object.
	 * 
	 * @param initialPosition This is the Position object representing the initial
	 *                        latitude and longitude of the drone.
	 * @param randNumGen      This is the random number generator object.
	 */
	public Stateless(Position initialPosition, Random randNumGen) {
		super(initialPosition, randNumGen);
		getInRange();
	}

	/**
	 * This method overrides the method defined in the superclass. Apart from
	 * getting all features that are within 0.00025 degrees of the drone, it also
	 * gets all features that could be in range after the drone has made a move.
	 * 
	 * It cycles through all features in the POIs ArrayList and adds all features
	 * that are 0.00025 degrees away from the current position of the drone to the
	 * inRange ArrayList while adding all features that are further than 0.00025
	 * degrees but less than 0.00055 degrees (move distance plus charging distance)
	 * away from the current position of the drone. The inRange and the inMoveRange
	 * ArrayLists are cleared every time this method is called.
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
	 * This method is responsible for updating all aspects of the drone after it has
	 * made a move. It takes an ArrayList moveList, representing the possible
	 * directions the drone can take dictated by the makeMove method, as an input.
	 * It outputs a direction, which represents the direction the drone has been
	 * made to take. This method is called by the makeMove method.
	 * 
	 * If the size of the input moveList ArrayList is larger than 1 the pseudo
	 * random number generator is used to randomly choose which direction the drone
	 * takes. The position of the drone is then updated with the nextPosition method
	 * of the Position class, the power value is deducted for the move, and the
	 * getInRange and updateStatus methods are called to update the stations now
	 * visible by the drone and charge from the closest station in range if one
	 * exists.
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
	 * This method implements the abstract method declared in the superclass. It
	 * computes the direction the drone takes during its next move. First 3
	 * ArrayLists are initialised: randomValidMoves, representing a set of
	 * directions that are valid for the drone to take, safeMoves, representing a
	 * set of direction where the drone won't charge to a danger station after its
	 * move, and lighthousesInMoveRange, representing a set of directions where the
	 * drone will charge to a lighthouse after its move.
	 * 
	 * The 16 cardinals directions are then iterated in a for loop. For each
	 * direction the next position the drone would be in is computed and then all
	 * features in the inMoveRange ArrayList are iterated if the next position is
	 * within the playing area. For each feature it is checked whether that feature
	 * is a lighthouse or a danger and is within 0.00025 degrees of the next
	 * position, asserting a respective boolean in that case. For each direction the
	 * distance to the closest lighthouse and closest danger is also computed,
	 * followed by adding that direction to the lighthousesInMoveRange if the drone
	 * charges from a lighthouse after taking that direction, to the safeMoves
	 * ArrayList if the drone doesn't charge from a danger after taking that
	 * direction, and the randomValidMoves if the drone is still within the playing
	 * area after taking that direction.
	 * 
	 * The updateState method is then called with one of the 3 ArrayLists. If the
	 * lighthousesInMoveRange ArrayList is not empty the updateStatus method is
	 * called with it as its input, otherwise if it's empty and the safeMoves
	 * ArrayList is not empty then the updateStatus method is called with it as its
	 * input, otherwise if both ArrayLists are empty then the updateStatus method is
	 * called with the randomValidMoves ArrayList as its input.
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
