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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class App {
	public static ArrayList<POI> POIs = new ArrayList<>();

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
		}
		return;
	}

	private static String inputStreamToString(InputStream inputStream) {
		Stream<String> result = new BufferedReader(new InputStreamReader(inputStream)).lines();
		return result.parallel().collect(Collectors.joining("\n"));
	}

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

	private static String formatTextOutput(Position firstPos, Position secondPos, Direction direction, float coins,
			float power) {
		return String.format("%f,%f,%s,%f,%f,%f,%f", firstPos.latitude, firstPos.longitude, direction,
				secondPos.latitude, secondPos.longitude, coins, power);
	}

	private static Drone initDrone(Position initialPosition, Random randNumGen, String droneType) {
		if (droneType.equals("stateless"))
			return new Stateless(initialPosition, randNumGen);
		else if (droneType.equals("stateful"))
			return new Stateful(initialPosition, randNumGen);
		else
			throw new IllegalArgumentException("Invalid Arguments!");
	}

	private static String arrayToString(ArrayList<String> moveList) {
		String moves = moveList.get(0);
		for (int i = 1; i < moveList.size(); i++)
			moves = moves.concat("\n" + moveList.get(i));
		return moves;
	}

	/**
	 * This method will compute the move sequence of the drone on the target map.
	 * Its inputs are the drone object and the list of points the drone visits
	 * during its move sequence, which is passed by reference to this method. The
	 * makeMove() method of the drone object will be called until the drone has made
	 * 250 moves. It will stop the move sequence early if the drone runs out of
	 * power before having made 250 moves. For each move the method will record the
	 * starting position of the drone, the direction it took, its final position,
	 * and its power and coins at the end of the move. It will output all of the
	 * recorded information as a string, as well as updating the input point list
	 * after every move. This method will be called in the main function.
	 * 
	 * @param drone  This is the drone object which will be used for computing the
	 *               move sequence.
	 * @param points This is the list of points that the drone will visit during its
	 *               move sequence.
	 * @return A string holding the current position of the drone, the direction it
	 *         took, its final position, and its power and coins at the end of the
	 *         move, for every move in the move sequence.
	 */
	private static String computeMoveSequence(Drone drone, ArrayList<Point> points) {
		ArrayList<String> moveList = new ArrayList<>();
		int moveNo = 0;

		while (drone.hasPower() && moveNo < 250) {
			Position firstPos = drone.currentPosition;
			Direction move = drone.makeMove();
			Position secondPos = drone.currentPosition;
			String text = formatTextOutput(firstPos, secondPos, move, drone.coins, drone.power);
			points.add(Point.fromLngLat(drone.currentPosition.longitude, drone.currentPosition.latitude));
			moveList.add(text);
			moveNo++;
		}
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
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (mapSource != null)
			return mapSource;
		else
			throw new IllegalArgumentException("Invalid map from URL!");
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
			e.printStackTrace();
		}
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
	 * @param args This is the 7 input arguments to the powergrab application.
	 */
	public static void main(String[] args) {
		String day = args[0];
		String month = args[1];
		String year = args[2];
		String mapString = String.format("http://homepages.inf.ed.ac.uk/stg/powergrab/%s/%s/%s/powergrabmap.geojson",
				year, month, day);

		String mapSource = getMapSource(mapString);
		parseFeatures(mapSource);

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

		String jsonFileName = String.format("%s-%s-%s-%s.geojson", droneType, day, month, year);
		String jsonFile = buildJsonFile(mapSource, points);

		writeToFile(jsonFileName, jsonFile);
	}
}
