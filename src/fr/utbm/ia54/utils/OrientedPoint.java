package fr.utbm.ia54.utils;

import javax.vecmath.Point2d;

/**
 * 
 * @author Florian & Alexis
 * Manages a point witch is oriented.
 * the orientation is given in radiant (0 is facing north)
 */
public class OrientedPoint extends Point2d {
	
	private static final long serialVersionUID = 1L;
	public double orientation;
	public double speed;
	
	public OrientedPoint() {
		this(0);
	}
	
	@SuppressWarnings("unused")
	public OrientedPoint(double angle) {
		super();
		this.orientation = 0;
	}
	
	public OrientedPoint(int x, int y, double angle) {
		super(x,y);
		this.orientation = angle;
	}
	
	public OrientedPoint (OrientedPoint p) {
		super(p);
		this.orientation = p.getOrientation();
	}


	public double getOrientation() {
		return this.orientation;
	}

	public void setOrientation(double orientation) {
		this.orientation = orientation;
	}
	
	public double getAngle() {
		return this.orientation;
	}

	public void setAngle(double orientation) {
		this.orientation = orientation;
	}
	
	public void addAngle(double angle) {
		this.orientation += angle;
	}

	public double getSpeed() {
		return this.speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}
	
	
	@Override
	public String toString() {
		
		return "("+this.x+","+this.y+","+this.orientation+","+this.speed+")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		
	}
}
