package uk.ac.ed.inf.powergrab;

import com.google.gson.*;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Feature;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class contains the main routine of the powergrab application. It is
 * responsible for the I/O necessities of the application and for initialising
 * all essential classes to the powergrab application.
 * 
 * @author David Jorge (s1712653)
 *
 */
public class App {
	public static ArrayList<POI> POIs = new ArrayList<>();
	private static Logger logger;
	private static float totalCoins = 0;
	private static float coinsCollected = 0;

	/**
	 * This method parses the features, representing geographical locations, of the
	 * target map from its map source. It takes a String representing the map source
	 * of the target map as an input. It adds instances of the POI object, which
	 * defines a single feature, to the global ArrayList POIs.
	 * 
	 * The FeatureCollection class is used to split the map source of the target map
	 * into a List of Features. Each feature is then used to initialise a separate
	 * instance of the POI class containing the ID, latitude and longitude, coins,
	 * power, symbol and colour described by that feature.
	 * 
	 * @param mapSource
	 */
	private static void parseFeatures(String mapSource) {
		FeatureCollection features = FeatureCollection.fromJson(mapSource);
		List<Feature> featureList = features.features();

		for (Feature feature : featureList) {
			Point point = (Point) feature.geometry();
			String id = feature.getProperty("id").getAsString();
			double latitude = point.latitude();
			double longitude = point.longitude();
			float coins = feature.getProperty("coins").getAsFloat();
			float power = feature.getProperty("power").getAsFloat();
			String symbol = feature.getProperty("marker-symbol").getAsString();
			String color = feature.getProperty("marker-color").getAsString();
			POIs.add(new POI(id, latitude, longitude, coins, power, symbol, color));
			if (coins > 0)
				totalCoins += coins;
		}
		return;
	}

	/**
	 * This is a support method for the getMapSource method. It takes an InputStream
	 * object resulting from a connection attempt to a URL, which contains the map
	 * source of the target map. It outputs the map source as a String.
	 * 
	 * @param inputStream This is the InputStream object resulting from a connection
	 *                    attempt to the URL containing the map source of the target
	 *                    map.
	 * @return A String representing the map source of the target map.
	 */
	private static String inputStreamToString(InputStream inputStream) {
		Stream<String> result = new BufferedReader(new InputStreamReader(inputStream)).lines();
		return result.parallel().collect(Collectors.joining("\n"));
	}

	/**
	 * This method builds a file, containing the geographical locations of the
	 * target map plus the path that the drone took during its move sequence, with a
	 * geojson type format. Its inputs are a String representing the map source for
	 * the target map and an ArrayList of points, which is passed by reference and
	 * represents the points the drone has visited in its move sequence. It outputs
	 * a String, which represents the geographical locations of the target map plus
	 * the path the drone took during its move sequence, in a geojson type format
	 * which will be written directly to the output geojson file of the powergrab
	 * application. This method will be called in the main method.
	 * 
	 * The geojson file content is built from scratch with JsonObjects and
	 * JsonArrays. First a JsonObject representing the final geojson content is
	 * created and the FeatureCollection property is added to it. Next the features
	 * from the target map, representing geographical locations, are parsed into a
	 * JsonArray using JsonParser. The LineString class is then used to create a
	 * line string from the ArrayList of points and put at the end of the previously
	 * created JsonArray. The JsonArray is then finally added to the main JsonObject
	 * which is then converted to a String with the geojson format.
	 * 
	 * @param mapSource This is String representing the map source for the target
	 *                  map.
	 * @param points    This is the ArrayList of Points representing the points the
	 *                  drone has visited during its move sequence.
	 * @return A String, representing the features of the target map plus the path
	 *         the drone took in its move sequence, in the geojson format. This
	 *         String will be written directly to the geojson file which is output
	 *         by the powergrab application.
	 */
	private static String buildJsonFile(String mapSource, ArrayList<Point> points) {
		JsonObject main = new JsonObject();
		main.addProperty("type", "FeatureCollection");

		JsonParser parser = new JsonParser();
		JsonObject jsonFile = (JsonObject) parser.parse(mapSource);
		JsonArray featureArray = jsonFile.get("features").getAsJsonArray();

		LineString lineStringObject = LineString.fromLngLats(points);
		String lineStringJson = lineStringObject.toJson();

		JsonObject geometry = (JsonObject) parser.parse(lineStringJson);

		JsonObject lineString = new JsonObject();
		lineString.addProperty("type", "Feature");
		lineString.add("properties", new JsonObject());
		lineString.add("geometry", geometry);
		featureArray.add(lineString);

		main.add("features", featureArray);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String prettyJson = gson.toJson(main);
		return prettyJson;
	}

	/**
	 * This is a support method for the computeMoveSequence method. Its inputs are a
	 * Position object representing the initial position of the drone before a move,
	 * a Position object representing the final position of the drone after a move,
	 * the direction the drone went to during the move, and the total value of the
	 * drone's coins and power after the move. It outputs a String encapsulating all
	 * the information from the inputs, which will be written in each new line of
	 * the output text file of the powergrab application, thus it follows the format
	 * specified by the design specifications.
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
	private static String formatTextOutput(Position firstPos, Position secondPos, Direction direction, float coins,
			float power) {
		return String.format("%f,%f,%s,%f,%f,%f,%f", firstPos.latitude, firstPos.longitude, direction,
				secondPos.latitude, secondPos.longitude, coins, power);
	}

	/**
	 * This method initialises the drone object. Its inputs are a position object
	 * representing the drone's initial position, a random number generator object,
	 * and a String representing the drone type. It outputs a drone object
	 * representing one of two possible types of drones: The Stateless drone or the
	 * Stateful drone. This method handles any invalid argument errors arising from
	 * a non-existing drone type. This method will be called in the main method.
	 * 
	 * @param initialPosition This is the Position object containing the starting
	 *                        latitude and longitude of the drone.
	 * @param randNumGen      This is the random number generator object used in
	 *                        calculating random movements taken by the drone.
	 * @param droneType       This is a String representing the drone type. Only
	 *                        accepted drone types are "Stateless" or "Stateful".
	 * @return A drone object representing the selected drone type.
	 */
	private static Drone initDrone(Position initialPosition, Random randNumGen, String droneType) {
		if (droneType.equals("stateless")) {
			logger.fine("Stateless Drone initialised successfully");
			return new Stateless(initialPosition, randNumGen);
		} else if (droneType.equals("stateful")) {
			logger.fine("Stateful Drone initialised successfully");
			return new Stateful(initialPosition, randNumGen);
		} else {
			logger.severe("Invalid drone type!");
			throw new IllegalArgumentException("Invalid Arguments!");
		}
	}

	/**
	 * This is a support method for the computeMoveSequence method. It takes an
	 * input ArrayList of type String representing the output created for each move
	 * in the move sequence. It outputs a String that will be written to the output
	 * text file following its design specifications where each output String for
	 * every move has to be in a new line.
	 * 
	 * @param moveList This is the ArrayList of type String containing the output
	 *                 String created after every move in the move sequence.
	 * @return A String where each String in the ArrayList is separated by a new
	 *         line.
	 */
	private static String arrayToString(ArrayList<String> moveList) {
		String moves = moveList.get(0);
		for (int i = 1; i < moveList.size(); i++)
			moves = moves.concat("\n" + moveList.get(i));
		return moves;
	}

	/**
	 * This method will compute the move sequence of the drone on the target map.
	 * Its inputs are the drone object and an ArrayList of points the drone visits
	 * during its move sequence, which is passed by reference to this method. The
	 * makeMove() method of the drone object will be called until the drone has made
	 * 250 moves. It will stop the move sequence early if the drone runs out of
	 * power before having made 250 moves. For each move the method will record the
	 * starting position of the drone, the direction it took, its final position,
	 * and its power and coins at the end of the move. It will output all of the
	 * recorded information as a string, as well as updating the input point
	 * ArrayList after every move. This method will be called in the main function.
	 * 
	 * @param drone  This is the drone object which will be used for computing the
	 *               move sequence.
	 * @param points This is the ArrayList of points that the drone will visit
	 *               during its move sequence.
	 * @return A string holding the current position of the drone, the direction it
	 *         took, its final position, and its power and coins at the end of the
	 *         move, for every move in the move sequence.
	 */
	private static String computeMoveSequence(Drone drone, ArrayList<Point> points) {
		ArrayList<String> moveList = new ArrayList<>();

		while (drone.hasPower() && drone.move < 250) {
			Position firstPos = drone.currentPosition;
			Direction move = drone.makeMove();
			Position secondPos = drone.currentPosition;
			String text = formatTextOutput(firstPos, secondPos, move, drone.coins, drone.power);
			points.add(Point.fromLngLat(drone.currentPosition.longitude, drone.currentPosition.latitude));
			moveList.add(text);
		}
		logger.fine("Drone path computed successfully");
		coinsCollected = drone.coins;
		return arrayToString(moveList);
	}

	/**
	 * This method parses the target map information from a URL as a String. It
	 * takes a URL as its input, which will contain the geojson file of the target
	 * map. It handles errors from the URL reading and it will check for malformed
	 * URLs. It will also raise an illegal argument exception if the output geojson
	 * file is null. This method will be called in the main function.
	 * 
	 * @param url This is the URL containing the geojson file of the target map.
	 * @return The geojson file for the target map as a string.
	 */
	private static String getMapSource(String url) {
		String mapSource = null;
		try {
			URL mapUrl = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) mapUrl.openConnection();
			conn.setReadTimeout(10000);
			conn.setConnectTimeout(15000);
			conn.setRequestMethod("GET");
			conn.setDoInput(true);
			conn.connect();
			InputStream inputStream = conn.getInputStream();
			mapSource = inputStreamToString(inputStream);
		} catch (MalformedURLException e) {
			logger.severe("Input URL is malformed!");
			e.printStackTrace();
		} catch (IOException e) {
			logger.severe("Failed to connect!");
			e.printStackTrace();
		}
		if (mapSource != null)
			return mapSource;
		else {
			logger.warning("Invalid Map Source!");
			throw new IllegalArgumentException("Invalid map from URL!");
		}
	}

	/**
	 * This method writes an input String to a file and outputs that file. Its
	 * inputs are a file name, which will be the output file name, and the text to
	 * write to that file. This method handles any I/O exceptions arising from
	 * writing to the output file, and it will be called in the main method.
	 * 
	 * @param fileName This is be the name of the output file created plus the file
	 *                 extension.
	 * @param text     This is the text that will be written to the output file.
	 */
	private static void writeToFile(String fileName, String text) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
			writer.write(text);
			writer.close();
		} catch (IOException e) {
			logger.severe("Writing to file failed!");
			e.printStackTrace();
		}
		return;
	}

	/**
	 * This method is responsible for initialising an instance of the Logger class,
	 * which will be used throughout the application for debugging and information
	 * reports. The level reported by the logger can be modified here to specify the
	 * granularity of information reported to aid debugging. This method will be
	 * called at the start of the main method.
	 */
	public static void setupLogger() {
		logger = Logger.getLogger("App");
		logger.setLevel(Level.INFO);
		Logger rootLogger = Logger.getLogger("");
		Handler handler = rootLogger.getHandlers()[0];
		handler.setLevel(Level.INFO);
		return;
	}

	/**
	 * This is the main method. It will be called from the command line with 7
	 * arguments which will act as inputs to the application. The input arguments
	 * are ordered as follows: day, month and year of the target map, the latitude
	 * and longitude for the starting position of the drone, the random seed and the
	 * drone type.
	 * 
	 * The method will start by parsing all input arguments, after which it will get
	 * the target map information. The specified drone type is then initialised as a
	 * drone object and its movement sequence on the target map is then computed.
	 * The output is written to two files: a text file describing the move sequence
	 * of the drone and a geojson file storing the geographical locations of the
	 * target map and the path the drone took during its move sequence.
	 * 
	 * A log of the main routine will be also be written to the console, including a
	 * report on the total coins collected.
	 * 
	 * @param args This is the 7 input arguments to the powergrab application.
	 */
	public static void main(String[] args) {
		setupLogger();

		String day = args[0];
		String month = args[1];
		String year = args[2];
		String mapString = String.format("http://homepages.inf.ed.ac.uk/stg/powergrab/%s/%s/%s/powergrabmap.geojson",
				year, month, day);

		String mapSource = getMapSource(mapString);
		parseFeatures(mapSource);
		logger.fine("Target map parsed successfully");

		double latitude = Double.parseDouble(args[3]);
		double longitude = Double.parseDouble(args[4]);
		int seed = Integer.parseInt(args[5]);
		String droneType = args[6];

		Random generator = new Random(seed);
		Position initialPosition = new Position(latitude, longitude);

		ArrayList<Point> points = new ArrayList<>();
		points.add(Point.fromLngLat(longitude, latitude));

		Drone drone = initDrone(initialPosition, generator, droneType);
		String moves = computeMoveSequence(drone, points);
		String textFileName = String.format("%s-%s-%s-%s.txt", droneType, day, month, year);

		writeToFile(textFileName, moves);
		logger.fine("Write to text file successful");

		String jsonFileName = String.format("%s-%s-%s-%s.geojson", droneType, day, month, year);
		String jsonFile = buildJsonFile(mapSource, points);

		writeToFile(jsonFileName, jsonFile);
		logger.fine("Write to geojson file successful");

		logger.info(String.format("For target map (%s/%s/%s):\nCollected a total of %f out of %f coins", day, month,
				year, coinsCollected, totalCoins));
	}
}
