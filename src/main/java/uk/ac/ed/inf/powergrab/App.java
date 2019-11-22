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
			throw new IllegalArgumentException("Invalid map from url!");
	}

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
