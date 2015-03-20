package fr.utbm.ia54.utils;

import java.awt.Point;

/**
 * 
 * @author Florian & Alexis
 * Manages a point witch is oriented.
 * the orientation is given in radiant (0 is facing north)
 */
public class OrientedPoint extends Point {
	
	private static final long serialVersionUID = 1L;
	public double orientation;
	public double speed;
	
	public OrientedPoint() {
		this(0);
	}
	
	public OrientedPoint(double angle) {
		super();
		orientation = 0;
	}
	
	public OrientedPoint(int x, int y, double angle) {
		super(x,y);
		orientation = angle;
	}
	
	public OrientedPoint (OrientedPoint p) {
		super((Point)p);
		orientation = p.getOrientation();
	}


	public double getOrientation() {
		return orientation;
	}

	public void setOrientation(double orientation) {
		this.orientation = orientation;
	}
	
	public double getAngle() {
		return orientation;
	}

	public void setAngle(double orientation) {
		this.orientation = orientation;
	}
	
	public void addAngle(double angle) {
		this.orientation += angle;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}
}
