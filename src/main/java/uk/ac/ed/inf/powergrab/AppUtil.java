package uk.ac.ed.inf.powergrab;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class defines support methods for the App class. It consists exclusively
 * of static methods, which will be called from the App class, that operate
 * primarily on Strings, formatting them into a desired result. The class is
 * made final to prevent it from being extended.
 * 
 * @author David Jorge (s1712653)
 *
 */
public final class AppUtil {
	/**
	 * A private attribute, logger, of type Logger is kept by this class in order to
	 * make logging statements throughout its methods where applicable.
	 */
	private static Logger logger;

	/**
	 * Make Constructor private to prevent the creation of any instances of this
	 * class.
	 */
	private AppUtil() {
	};

	/**
	 * This method is responsible for initialising a subclass logger of the logger
	 * class initialised in the App class. This logger object will be used for
	 * debugging and information message logging in the AppUtils class.
	 */
	public static void setupLogger() {
		logger = Logger.getLogger("App.AppUtils");
		return;
	}

	/**
	 * This is a support method for the getMapSource method in the App class. It
	 * takes an InputStream object resulting from a connection attempt to a URL,
	 * which contains the map source of the target map. It outputs the map source as
	 * a String.
	 * 
	 * @param inputStream This is the InputStream object resulting from a connection
	 *                    attempt to the URL containing the map source of the target
	 *                    map.
	 * @return A String representing the map source of the target map.
	 */
	public static String inputStreamToString(InputStream inputStream) {
		Stream<String> result = new BufferedReader(new InputStreamReader(inputStream)).lines();
		return result.parallel().collect(Collectors.joining("\n"));
	}

	/**
	 * This is a support method for the computeMoveSequence method in the App class.
	 * It takes an ArrayList of type String as its input, and it outputs a String
	 * where every entity of the input ArrayList is separated by a new line.
	 * 
	 * @param moveList This is the ArrayList of type String containing the output
	 *                 String created after every move in the move sequence.
	 * @return A String where each String in the ArrayList is separated by a new
	 *         line.
	 */
	public static String arrayToString(ArrayList<String> moveList) {
		String moves = moveList.get(0);
		for (int i = 1; i < moveList.size(); i++)
			moves = moves.concat("\n" + moveList.get(i));
		return moves;
	}

	/**
	 * This is a support method for the computeMoveSequence method in the App class.
	 * Its inputs are a Position object representing the initial position of the
	 * drone before a move, a Position object representing the final position of the
	 * drone after a move, the direction the drone went to during the move, and the
	 * total value of the drone's coins and power after the move. It outputs a
	 * String encapsulating all the information from the inputs, which will be
	 * written in each new line of the output text file of the powergrab
	 * application, thus it follows the format specified by the design
	 * specifications.
	 * 
	 * @param firstPos  This is the Position object containing the latitude and
	 *                  longitude of the drone before making a move.
	 * @param secondPos This is the Position object containing the latitude and
	 *                  longitude of the drone after making a move.
	 * @param direction This is one of the 16 cardinal directions the drone took
	 *                  during a move.
	 * @param coins     This is the total value of coins the drone holds after a
	 *                  move.
	 * @param power     This is the total value of power the drone holds after a
	 *                  move.
	 * @return A String reporting the initial and final position of a drone after
	 *         making a move, the direction it took, and the total value of coins
	 *         and power it holds after making a move.
	 */
	public static String formatTextOutput(Position firstPos, Position secondPos, Direction direction, double coins,
			double power) {
		logger.finer(String.format(
				"Parsing text file output for drone at position %.3f %.3f taking direction %s to position %.3f %.3f",
				firstPos.latitude, firstPos.longitude, direction, secondPos.latitude, secondPos.longitude));
		return String.format("%f,%f,%s,%f,%f,%f,%f", firstPos.latitude, firstPos.longitude, direction,
				secondPos.latitude, secondPos.longitude, coins, power);
	}
}
