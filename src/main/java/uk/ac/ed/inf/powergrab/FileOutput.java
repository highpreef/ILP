package uk.ac.ed.inf.powergrab;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * This class specialises in handling all output file writing requests from the
 * App class.
 * 
 * @author DAVID
 *
 */
public final class FileOutput {
	private static Logger logger;
	
	/**
	 * Make Constructor private to prevent the creation of any instances of this
	 * class.
	 */
	private FileOutput() {};

	/**
	 * This method is responsible for initialising a subclass logger of the logger
	 * class initialised in the App class. This logger object will be used for
	 * debugging and information message logging in the AppFileOutput class.
	 */
	public static void setupLogger() {
		logger = Logger.getLogger("App.AppFileOutput");
		return;
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
	public static void writeToFile(String fileName, String text) {
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
}
