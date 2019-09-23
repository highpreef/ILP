package uk.ac.ed.inf.powergrab;

public class Position {
	public double latitude;
	public double longitude;
	public final double r = 0.0003;
	public final double longitude_max = 55.946233;
	public final double longitude_min = 55.942617;
	public final double latitude_max = -3.184319;
	public final double latitude_min = -3.192473;
	
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
		boolean inPlayArea = (this.latitude > this.latitude_min) && (this.latitude < this.latitude_max) && 
				(this.longitude > this.longitude_min) && (this.longitude < this.longitude_max); 
		return inPlayArea;
	}
}
