package uk.ac.ed.inf.powergrab;

/**
 * This class is used for representing a position in terms of latitude and
 * longitude. It also offers utility methods.
 * 
 * @author David Jorge (s1712653)
 *
 */
public class Position {
	public double latitude;
	public double longitude;
	public final double r = 0.0003;

	/**
	 * Constructor for the Position class. It stores the latitude and longitude of a
	 * 2D point
	 * 
	 * @param latitude  Latitude of the 2D point.
	 * @param longitude Longitude of the 2D point.
	 */
	public Position(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	/**
	 * This method returns the next position after a move of 0.0003 degrees in a
	 * direction. It takes one of 16 cardinal directions as its input. It outputs
	 * the next position using trigonometry.
	 * 
	 * @param direction This is one of the 16 cardinal direction the move is made
	 *                  to.
	 * @return A Position object representing the next position after taking a move
	 *         of 0.0003 degrees from the current position to the input direction.
	 */
	public Position nextPosition(Direction direction) {
		Position nextPos = new Position(this.latitude, this.longitude);
		nextPos.latitude += r * Math.sin(Math.toRadians(direction.angle));
		nextPos.longitude += r * Math.cos(Math.toRadians(direction.angle));
		return nextPos;
	}

	/**
	 * This method returns a boolean value representing whether the current position
	 * is within a defined area.
	 * 
	 * @return true if the Position object is within the play area otherwise it
	 *         returns false.
	 */
	public boolean inPlayArea() {
		boolean inPlayArea = (this.latitude > 55.942617) && (this.latitude < 55.946233) && (this.longitude > -3.192473)
				&& (this.longitude < -3.184319);
		return inPlayArea;
	}
}
