package uk.ac.ed.inf.powergrab;

public enum Direction {
	N(90),
	NNE(67.5),
	NE(45),
	ENE(22.5),
	E(0),
	ESE(-22.5),
	SE(-45),
	SSE(-67.5),
	S(-90),
	SSW(-112.5),
	SW(-135),
	WSW(-157.5),
	W(180),
	WNW(157.5),
	NW(135),
	NNW(112.5);
	
	public double angle;
	
	private Direction(double angle) {
		this.angle = angle;
	}
	
}
