package uk.ac.ed.inf.powergrab;

/**
 * This class represents an instance of a position in a 2D lat-long space. It
 * consists of non-static methods that operate on the current position.
 * 
 * @author David Jorge (s1712653)
 *
 */
public class Position {
	/**
	 * This class has 3 attributes for each an instance: a public double
	 * representing the latitude of the position, a public double representing the
	 * longitude of the position, and a private final double representing the
	 * distance a drone travels during a move.
	 */
	public double latitude;
	public double longitude;
	private final double r = 0.0003;

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
	 * This public method returns the next position relative to the current position
	 * after travelling a distance of 0.0003 degrees in one of the 16 cardinal
	 * directions. It takes a Direction object representing one of the 16 cardinal
	 * directions and returns the position after the move using trigonometry.
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
	 * This public method returns a Boolean value representing whether or not the
	 * current position is within the well-defined playing are of the powergrab
	 * application.
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
