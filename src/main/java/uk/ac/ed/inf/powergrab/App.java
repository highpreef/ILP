package uk.ac.ed.inf.powergrab;

import com.google.gson.*;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Feature;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements the main routine for the application. It consists
 * exclusively of static methods that operate primarily on or return objects
 * representing arguments and outputs of the drone functionality. It’s
 * responsible for handling the I/O of the application and for controlling the
 * drone functionality. Where applicable the methods of this class can throw an
 * IllegalArgumentException, MalformedURLException and IOException, indicating
 * invalid arguments or outputs.
 * 
 * @author David Jorge (s1712653)
 *
 */
public class App {
	/**
	 * This class has 4 attributes: a public ArrayList, POIs, of type POI which
	 * store references to all individual features of the target map, a private
	 * Logger object, logger, to log statements in this class, and 2 private double
	 * variables, totalCoins and coinsCollected, representing the total possible
	 * amount of coins to be acquired and the total amount acquired by the drone
	 * during its move sequence respectively.
	 */
	public static ArrayList<POI> POIs = new ArrayList<>();
	private static Logger logger;
	private static double totalCoins = 0;
	private static double coinsCollected = 0;

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
	 * @param mapSource This is the String holding the information from the target
	 *                  map in a geojson format.
	 */
	private static void parseFeatures(String mapSource) {
		List<Feature> featureList = FeatureCollection.fromJson(mapSource).features();

		for (Feature feature : featureList) {
			Point point = (Point) feature.geometry();
			String id = feature.getProperty("id").getAsString();
			double latitude = point.latitude();
			double longitude = point.longitude();
			double coins = feature.getProperty("coins").getAsDouble();
			double power = feature.getProperty("power").getAsDouble();
			String symbol = feature.getProperty("marker-symbol").getAsString();
			String color = feature.getProperty("marker-color").getAsString();
			POIs.add(new POI(id, latitude, longitude, coins, power, symbol, color));
			if (coins > 0)
				totalCoins += coins;
		}
		return;
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
	 * This method initialises the drone object. Its inputs are a position object
	 * representing the drone's initial position, a pseudo-random number generator
	 * object, and a String representing the drone type. It outputs a drone object
	 * representing one of two possible types of drones: The Stateless drone or the
	 * Stateful drone. This method handles any invalid argument errors arising from
	 * a non-existing drone type. This method will be called in the main method.
	 * 
	 * @param initialPosition This is the Position object containing the starting
	 *                        latitude and longitude of the drone.
	 * @param randNumGen      This is the pseudo-random number generator object used
	 *                        in calculating random movements taken by the drone.
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
			String text = AppUtil.formatTextOutput(firstPos, secondPos, move, drone.coins, drone.power);
			points.add(Point.fromLngLat(drone.currentPosition.longitude, drone.currentPosition.latitude));
			moveList.add(text);
		}
		logger.fine("Drone path computed successfully");
		coinsCollected = drone.coins;
		return AppUtil.arrayToString(moveList);
	}

	/**
	 * This method parses the target map information from a URL. It takes a String,
	 * representing a URL, as its input which contains the geojson file of the
	 * target map. It handles errors from the URL connection attempt and it will
	 * check for malformed URLs. It will also raise an illegal argument exception if
	 * the output geojson file is null. This method will be called in the main
	 * function.
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
			mapSource = AppUtil.inputStreamToString(inputStream);
		} catch (MalformedURLException e) {
			logger.severe("Input URL is malformed!");
			e.printStackTrace();
		} catch (IOException e) {
			logger.severe("Failed to get map!");
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
	 * This private method is responsible for creating a new Logger from the Logger
	 * class using its getLogger() method. A hierarchy of loggers will be created
	 * throughout the application with the App class logger as the top level. The
	 * level reported by the logger can be modified here to specify the granularity
	 * of information reported to aid debugging. This method will be called at the
	 * start of the main method.
	 */
	private static void setupLogger() {
		logger = Logger.getLogger("App");
		logger.setLevel(Level.INFO);
		Logger rootLogger = Logger.getLogger("");
		Handler handler = rootLogger.getHandlers()[0];
		handler.setLevel(Level.INFO);

		AppUtil.setupLogger();
		FileOutput.setupLogger();
		return;
	}

	/**
	 * This is the main method. It will be called from the command line with 7
	 * arguments which will act as inputs to the application. The input arguments
	 * are ordered as follows: day, month and year of the target map, the latitude
	 * and longitude for the starting position of the drone, the random seed and the
	 * drone type.
	 * 
	 * The method will start by parsing all input arguments, catching any invalid
	 * argument exceptions, after which it will get the target map information. The
	 * specified drone type is then initialised as a drone object and its movement
	 * sequence on the target map is then computed. The output is written to two
	 * files: a text file describing the move sequence of the drone and a geojson
	 * file storing the geographical locations of the target map and the path the
	 * drone took during its move sequence.
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
		double latitude;
		double longitude;
		int seed;

		try {
			latitude = Double.parseDouble(args[3]);
			longitude = Double.parseDouble(args[4]);
			seed = Integer.parseInt(args[5]);
		} catch (NumberFormatException e) {
			logger.severe("Invalid input arguments!");
			throw new IllegalArgumentException("Invalid arguments!");
		}

		String mapString = String.format("http://homepages.inf.ed.ac.uk/stg/powergrab/%s/%s/%s/powergrabmap.geojson",
				year, month, day);

		String mapSource = getMapSource(mapString);
		parseFeatures(mapSource);
		logger.fine("Target map parsed successfully");

		String droneType = args[6];

		Random generator = new Random(seed);
		Position initialPosition = new Position(latitude, longitude);

		ArrayList<Point> points = new ArrayList<>();
		points.add(Point.fromLngLat(longitude, latitude));

		Drone drone = initDrone(initialPosition, generator, droneType);
		String moves = computeMoveSequence(drone, points);
		String textFileName = String.format("%s-%s-%s-%s.txt", droneType, day, month, year);

		FileOutput.writeToFile(textFileName, moves);
		logger.fine("Write to text file successful");

		String jsonFileName = String.format("%s-%s-%s-%s.geojson", droneType, day, month, year);
		String jsonFile = buildJsonFile(mapSource, points);

		FileOutput.writeToFile(jsonFileName, jsonFile);
		logger.fine("Write to geojson file successful");

		logger.info(String.format("For target map (%s/%s/%s):\n%s drone collected a total of %.2f out of %.2f coins",
				day, month, year, droneType, coinsCollected, totalCoins));
	}
}
