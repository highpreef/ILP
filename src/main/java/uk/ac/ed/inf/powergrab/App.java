package uk.ac.ed.inf.powergrab;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

public class App {
	
	private static ArrayList<Feature> POI = new ArrayList<>();
	
	private static void parseFeature(JSONObject feature) {	
		JSONObject properties = (JSONObject) feature.get("properties");
		JSONObject geometry = (JSONObject) feature.get("geometry");
		
		String id = (String) properties.get("id");
		String coins = (String) properties.get("coins");
		String power = (String) properties.get("power");
		String symbol = (String) properties.get("marker-symbol");
		String color = (String) properties.get("marker-color");


		JSONArray coordinates = (JSONArray) geometry.get("coordinates");
		ArrayList<String> coordinateList = new ArrayList<>();
		coordinates.forEach(c -> coordinateList.add(c.toString()));
		
		POI.add(new Feature(id, Double.parseDouble(coordinateList.get(1)), Double.parseDouble(coordinateList.get(0)), Double.parseDouble(coins), Double.parseDouble(power), symbol, color));;
	}
	
    public static void main( String[] args ) {
    	 JSONParser parser = new JSONParser();
    	 
    	 try (Reader reader = new FileReader("C:\\Users\\DAVID\\git\\ILP\\powergrab\\powergrabmap.json")) {
    		 
    		 JSONObject jsonObject = (JSONObject) parser.parse(reader);
    		 
    		 JSONArray featureList = (JSONArray) jsonObject.get("features");
    		 
    		 featureList.forEach(feature -> parseFeature( (JSONObject) feature));
    		 
    	 } catch (IOException e) {
    		 e.printStackTrace();
    	 } catch (ParseException e) {
    		 e.printStackTrace();
    	 }
    	 
    	System.out.println(POI.get(0).getLatitude());
    }
}
