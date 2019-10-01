package uk.ac.ed.inf.powergrab;

import com.google.gson.*;
import com.mapbox.geojson.*;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Random;

public class App {
	
	public static ArrayList<Feature> POI = new ArrayList<>();
	
	private static void parseFeature(JsonObject feature) {	
		JsonObject properties = (JsonObject) feature.get("properties");
		JsonObject geometry = (JsonObject) feature.get("geometry");
		
		String id = properties.get("id").getAsString();
		double coins = properties.get("coins").getAsDouble();
		double power = properties.get("power").getAsDouble();
		String symbol = properties.get("marker-symbol").getAsString();
		String color = properties.get("marker-color").getAsString();

		JsonArray coordinates = (JsonArray) geometry.get("coordinates");
		
		POI.add(new Feature(id, coordinates.get(1).getAsDouble(), coordinates.get(0).getAsDouble(), coins, power, symbol, color));
	}
	
    public static void main( String[] args ) {
    	int seed = 5234;
    	
    	JsonParser parser = new JsonParser();
    	 
    	try (Reader reader = new FileReader("C:\\Users\\DAVID\\git\\ILP\\powergrab\\powergrabmap.json")) {    		 
    		JsonObject jsonObject = (JsonObject) parser.parse(reader);		 
    		JsonArray featureList = (JsonArray) jsonObject.get("features");	 
    		featureList.forEach(feature -> parseFeature((JsonObject) feature));
    	} catch (IOException e) {    		 
    		e.printStackTrace();
    	} 
    	
    	Random generator = new Random(seed);
    	Position initialPosition = new Position(55.944425, -3.188396);
    	Stateless_Drone drone = new Stateless_Drone(initialPosition, generator);
    	
    	System.out.println(POI.get(0).latitude);
    }
}
