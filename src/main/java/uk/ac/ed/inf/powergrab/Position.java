package uk.ac.ed.inf.powergrab;

public class Position {
	public double latitude;
	public double longitude;
	public final double r = 0.0003;
	
	public Position(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public Position nextPosition(Direction direction) {
		Position nextPos = new Position(this.latitude, this.longitude);
		nextPos.latitude += r * Math.sin(Math.toRadians(direction.angle));
		nextPos.longitude += r * Math.cos(Math.toRadians(direction.angle));		
		return nextPos;
	}
	
	public boolean inPlayArea() {
		boolean inPlayArea = (this.latitude > 55.942617) && (this.latitude < 55.946233) && 
				(this.longitude > -3.192473) && (this.longitude < -3.184319); 
		return inPlayArea;
	}
}
