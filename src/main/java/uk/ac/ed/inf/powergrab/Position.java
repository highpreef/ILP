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
		
		switch(direction) {
			case N:
				nextPos.latitude += r;
				break;
			case NNE:
				nextPos.latitude += r * Math.sin(Math.toRadians(67.5));
				nextPos.longitude += r * Math.cos(Math.toRadians(67.5));
				break;
			case NE:
				nextPos.latitude += r * Math.sin(Math.toRadians(45));
				nextPos.longitude += r * Math.cos(Math.toRadians(45));
				break;
			case ENE:
				nextPos.latitude += r * Math.sin(Math.toRadians(22.5));
				nextPos.longitude += r * Math.cos(Math.toRadians(22.5));
				break;
			case E:
				nextPos.longitude += r;
				break;
			case ESE:
				nextPos.latitude += r * Math.sin(Math.toRadians(-22.5));
				nextPos.longitude += r * Math.cos(Math.toRadians(-22.5));
				break;
			case SE:
				nextPos.latitude += r * Math.sin(Math.toRadians(-45));
				nextPos.longitude += r * Math.cos(Math.toRadians(-45));
				break;
			case SSE:
				nextPos.latitude += r * Math.sin(Math.toRadians(-67.5));
				nextPos.longitude += r * Math.cos(Math.toRadians(-67.5));
				break;
			case S:
				nextPos.latitude -= r;
				break;
			case SSW:
				nextPos.latitude += r * Math.sin(Math.toRadians(-112.5));
				nextPos.longitude += r * Math.cos(Math.toRadians(-112.5));
				break;
			case SW:
				nextPos.latitude += r * Math.sin(Math.toRadians(-135));
				nextPos.longitude += r * Math.cos(Math.toRadians(-135));
				break;
			case WSW:
				nextPos.latitude += r * Math.sin(Math.toRadians(-157.5));
				nextPos.longitude += r * Math.cos(Math.toRadians(-157.5));
				break;
			case W:
				nextPos.longitude -= r;
				break;
			case WNW:
				nextPos.latitude += r * Math.sin(Math.toRadians(157.5));
				nextPos.longitude += r * Math.cos(Math.toRadians(157.5));
				break;
			case NW:
				nextPos.latitude += r * Math.sin(Math.toRadians(135));
				nextPos.longitude += r * Math.cos(Math.toRadians(135));
				break;
			case NNW:
				nextPos.latitude += r * Math.sin(Math.toRadians(112.5));
				nextPos.longitude += r * Math.cos(Math.toRadians(112.5));
				break;		
		}
		return nextPos;
	}
	
	public boolean inPlayArea() {
		return true;
	}
	
	public static void main(String[] args) {
		Position test = new Position(35,46);
		Position next = test.nextPosition(Direction.N);
		System.out.printf("(%f,%f)", next.latitude, next.longitude);
	}
}
