package uk.ac.ed.inf.powergrab;


import com.google.gson.*;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Feature;

import java.io.BufferedReader;
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
	}
	
	private static String inputStreamToString(InputStream inputStream) {
		Stream<String> result = new BufferedReader(new InputStreamReader(inputStream)).lines();
		return result.parallel().collect(Collectors.joining("\n"));
	}
	
    public static void main( String[] args ) {
    	int seed = 5234;
    	String mapString = "http://homepages.inf.ed.ac.uk/stg/powergrab/2019/09/15/powergrabmap.geojson";
    	
    	try {
			URL mapUrl = new URL(mapString);
			HttpURLConnection conn = (HttpURLConnection) mapUrl.openConnection();
			conn.setReadTimeout(10000);
			conn.setConnectTimeout(15000);
			conn.setRequestMethod("GET");
			conn.setDoInput(true);
			conn.connect();
			InputStream inputStream = conn.getInputStream();
			String mapSource = inputStreamToString(inputStream);
			parseFeatures(mapSource);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	Random generator = new Random(seed);
    	Position initialPosition = new Position(55.944425, -3.188396);
    	//Stateless_Drone drone = new Stateless_Drone(initialPosition, generator);
    	
    	;
    	System.out.println(POIs.get(0).latitude);
    }
}
