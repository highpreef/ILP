package uk.ac.ed.inf.powergrab;

/**
 * This class is used for enumerating the 16 cardinal directions along with
 * their angles in degrees.
 * 
 * @author David Jorge (s1712653)
 *
 */
public enum Direction {
	N(90), NNE(67.5), NE(45), ENE(22.5), E(0), ESE(337.5), SE(315), SSE(292.5), S(270), SSW(247.5), SW(225), WSW(202.5),
	W(180), WNW(157.5), NW(135), NNW(112.5);

	public double angle;

	/**
	 * Constructor of the Direction class. It allows for the angle of each direction
	 * to be accessible.
	 * 
	 * @param angle This is the angle for each one of the 16 cardinal directions.
	 */
	private Direction(double angle) {
		this.angle = angle;
	}

}
