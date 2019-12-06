package uk.ac.ed.inf.powergrab;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * This class is responsible for handling output file writing requests. It
 * consists exclusively of static methods which either setup a logger for the
 * class or write contents to an output file. The class is made final to prevent
 * it from being extended. Where appropriate, the methods of this class can
 * throw an IOException.
 * 
 * @author David Jorge (s1712653)
 *
 */
public final class FileOutput {
	/**
	 * A private attribute, logger, of type Logger is kept by this class in order to
	 * make logging statements throughout its methods where applicable.
	 */
	private static Logger logger;

	/**
	 * Make Constructor private to prevent the creation of any instances of this
	 * class.
	 */
	private FileOutput() {
	};

	/**
	 * This method is responsible for initialising a subclass logger of the logger
	 * initialised in the App class. This logger object will be used for debugging
	 * and information message logging in the AppFileOutput class.
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
