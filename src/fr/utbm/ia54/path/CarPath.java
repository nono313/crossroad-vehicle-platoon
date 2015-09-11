package fr.utbm.ia54.path;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;

import fr.utbm.ia54.utils.OrientedPoint;
import fr.utbm.ia54.utils.Functions;

/**
 * 
 * @author Florian & Alexis
 * Represents the path described in an XML file.
 */
public class CarPath {
	
	private List<List<OrientedPoint>> path;
	private List<OrientedPoint> crossing;
	private ImageIcon background;
	
	public CarPath(){
		this.path = new ArrayList<List<OrientedPoint>>();
		this.crossing = new ArrayList<OrientedPoint>();
	}

	public List<List<OrientedPoint>> getPath() {
		return path;
	}

	public void setPath(List<List<OrientedPoint>> path) {
		this.path = path;
	}

	public ImageIcon getBackground() {
		return background;
	}

	public void setBackground(ImageIcon background) {
		this.background = background;
	}
	
	/** 
	 * Search for position on a road, 
	 * compute distance to travel and position, 
	 * return new position
	 */
	public OrientedPoint getNextPoint(OrientedPoint actualP, float distance, int numTrain) {
		OrientedPoint nextP = new OrientedPoint(actualP);
		OrientedPoint previousP = null;
		
		Iterator<OrientedPoint> it = path.get(numTrain).iterator();
		
		// for now we suppose the car is on the road, and we try to find it (between 2 points/corners)
		while(it.hasNext() && distance > 0) {
			OrientedPoint p = it.next();
			// we check if the actual position is between supposedP and p
			if (previousP != null) {
				if((nextP.x == p.x && Functions.between(previousP.y,p.y,nextP.y))
						|| (nextP.y == p.y && Functions.between(previousP.x,p.x,nextP.x))) {
					//if we move in y
					if(previousP.x == p.x) {
						//if no needs to rotate
						if(nextP.y + distance < p.y && nextP.orientation ==Math.PI) {
							nextP = new OrientedPoint(nextP);
							nextP.y = (int) (nextP.y + distance);
							distance = 0;
						} 
						else if (nextP.y - distance > p.y && (nextP.orientation == Math.PI*2 || nextP.orientation == 0)) {
							nextP = new OrientedPoint(nextP);
							nextP.y = (int) (nextP.y - distance);
							distance = 0;
						// in case of arriving just on the point	
						} 
						else if (nextP.y - distance == p.y || nextP.y + distance == p.y) {
							nextP = new OrientedPoint(p);
							distance = 0;
						} 
						else { // we need to rotate
							nextP = new OrientedPoint(p);
							previousP = p;
							distance -= Functions.manhattan(nextP, p);
						}
					} 
					else { // if we move on x
						// if no needs to rotate
						if(nextP.x + distance < p.x && nextP.orientation ==Math.PI/2) {
							nextP = new OrientedPoint(nextP);
							nextP.x =  (int) (nextP.x + distance);
							distance = 0;
						}
						else if (nextP.x - distance > p.x && nextP.orientation == Math.PI*3/2) {
							nextP = new OrientedPoint(nextP);
							nextP.x =  (int) (nextP.x - distance);
							distance = 0;
							
						// in case of arriving just on the point	
						} 
						else if (nextP.x - distance == p.x || nextP.x + distance == p.x) {
							nextP = new OrientedPoint(p);
							distance = 0;
						
						} 
						else { // we need to rotate
							nextP = new OrientedPoint(p);
							previousP = p;
							distance -= Functions.manhattan(nextP, p);
						}
					}
				} 
				else {
					// we continue searching
					previousP = null;
				}
				
			}
			// we avoid a lot of research by putting away all roads not oriented like the car
			if(p.orientation == nextP.orientation) {
				if(p.x == nextP.x || p.y == nextP.y) {
					previousP = p;
				}
			}
		}
		return nextP;
	}
	
	/**
	 * Get the first point of the path.
	 * @param numTrain
	 * @return
	 */
	public OrientedPoint getStart(int numTrain) {
		return this.path.get(numTrain).get(0);
	}
	
	/**
	 * Set a list containing all the crossing points between trains.
	 */
	public void generateCrossing(){
		List<List<OrientedPoint>> pathCopy = new ArrayList<List<OrientedPoint>>(path); // Copy the path list
		List<OrientedPoint> current = new ArrayList<OrientedPoint>();
		List<OrientedPoint> tmp = new ArrayList<OrientedPoint>();
		OrientedPoint startCurrent = new OrientedPoint();
		OrientedPoint endCurrent = new OrientedPoint();
		OrientedPoint startTmp = new OrientedPoint();
		OrientedPoint endTmp = new OrientedPoint();
		
		// For all train path
		for(int i=0;(i+1)<path.size();i++){
			current = pathCopy.get(0);

			for(int l=1;l<path.size();l++){
				tmp = pathCopy.get(l);
				
				// Two points by two fixed : we iterate over the whole other path
				for(int j=0;(j+1)<current.size();j++){
					startCurrent = current.get(j);
					endCurrent = current.get(j+1);
					
					// Iterate over the whole path
					for(int k=0;(k+1)<tmp.size();k++){
						startTmp = tmp.get(k);
						endTmp = tmp.get(k+1);
						
						// Check if there is a crossing
						OrientedPoint cross = isCrossed(startCurrent, endCurrent, startTmp,endTmp);
						
						if(cross != null) {
							this.crossing.add(cross);
						}
					}
				}
			}
			pathCopy.remove(0);
		}
	}

	/**
	 * Check if there is a cross between 2 lines.
	 * @param startCurrent
	 * @param endCurrent
	 * @param startTmp
	 * @param endTmp
	 * @return the crossing point or null
	 */
	private OrientedPoint isCrossed(OrientedPoint startCurrent, OrientedPoint endCurrent, OrientedPoint startTmp, OrientedPoint endTmp) {
		OrientedPoint point = new OrientedPoint();
		
		// Check if the two lines are perpendicular
		if(Math.abs(Math.toDegrees(startCurrent.orientation) - Math.toDegrees(startTmp.orientation)) == 90 || Math.abs(Math.toDegrees(startCurrent.orientation) - Math.toDegrees(startTmp.orientation)) == 270){
			
			OrientedPoint tmp = null;
			// Swap points if they are not in the correct order to simplify tests
			if((startCurrent.x > endCurrent.x) || (startCurrent.y < endCurrent.y)){
				tmp = startCurrent;
				startCurrent = endCurrent;
				endCurrent = tmp;
			}
			
			if((startTmp.x > endTmp.x) || (startTmp.y < endTmp.y)){
				tmp = startTmp;
				startTmp = endTmp;
				endTmp = tmp;
			}
			
			// Check if there is a crossing
			if(((startTmp.x > startCurrent.x && startTmp.x < endCurrent.x && endTmp.x > startCurrent.x && endTmp.x < endCurrent.x)
					&& (startCurrent.y < startTmp.y && startCurrent.y > endTmp.y && endCurrent.y < startTmp.y && endCurrent.y > endTmp.y))
					|| ((startCurrent.x > startTmp.x && startCurrent.x < endTmp.x && endCurrent.x > startTmp.x && endCurrent.x < endTmp.x)
							&& (startTmp.y < startCurrent.y && startTmp.y > endCurrent.y && endTmp.y < startCurrent.y && endTmp.y > endCurrent.y))){
				
				// Get the crossing point
				if(startTmp.x == endTmp.x && startCurrent.y == endCurrent.y){
					point.x = startTmp.x;
					point.y = startCurrent.y;
					return point;
				}
				
				if(startCurrent.x == endCurrent.x && startTmp.y == endTmp.y){
					point.x = startCurrent.x;
					point.y = startTmp.y;
					return point;
				}
			}
		}
		return null;
	}

	public List<OrientedPoint> getCrossing() {
		return crossing;
	}

	public ArrayList<OrientedPoint> getCrossingNear(OrientedPoint pos, int range) {
		ArrayList<OrientedPoint> concerned = new ArrayList<OrientedPoint>();
		
		for (OrientedPoint it : crossing) {
			if(Functions.manhattan(it, pos) <= range) {
				concerned.add(it);
			}
		}
		return concerned;
	}
	
	/**
	 * Set a list containing all the crossing points between trains.
	 */
	public void getCrossingPoint(){
		List<List<OrientedPoint>> pathCopy = new ArrayList<List<OrientedPoint>>(path); // Copy the path list
		List<OrientedPoint> current = new ArrayList<OrientedPoint>();
		List<OrientedPoint> tmp = new ArrayList<OrientedPoint>();
		OrientedPoint startCurrent = new OrientedPoint();
		OrientedPoint endCurrent = new OrientedPoint();
		OrientedPoint startTmp = new OrientedPoint();
		OrientedPoint endTmp = new OrientedPoint();
		
		// For all train path
		for(int i=0;(i+1)<path.size();i++){
			current = pathCopy.get(0);

			for(int l=1;l<path.size();l++){
				tmp = pathCopy.get(l);
				
				// Two points by two fixed : we iterate over the whole other path
				for(int j=0;(j+1)<current.size();j++){
					startCurrent = current.get(j);
					endCurrent = current.get(j+1);
					
					// Iterate over the whole path
					for(int k=0;(k+1)<tmp.size();k++){
						startTmp = tmp.get(k);
						endTmp = tmp.get(k+1);
						
						// Check if there is a crossing
						OrientedPoint cross = isCrossed(startCurrent, endCurrent, startTmp,endTmp);
						
						if(cross != null)
							this.crossing.add(cross);
					}
				}
			}
			pathCopy.remove(0);
		}
		System.out.println(crossing);
	}
	public void setCrossing(List<OrientedPoint> crossing) {
		this.crossing = crossing;
	}

	/**
	 * Return true if point is in the path of the car, defined by it's train.
	 * If distance is defined (!=-1), point have to be closer than distance from carPos (moving along the circuit)
	 * @param carPos
	 * @param train
	 * @param point
	 * @param distance
	 * @return
	 */
	public boolean isInPath(OrientedPoint carPos, int train, OrientedPoint point, int distance) {
		Iterator<OrientedPoint> it = path.get(train).iterator();
		Iterator<OrientedPoint> itbis = path.get(train).iterator();
		boolean found = false;
		boolean foundCar = false;
		OrientedPoint previousP = null;
		OrientedPoint p = null;
		
		// for now we suppose the car is on the road, and we try to find it (between 2 points/corners)
		while(it.hasNext() && !foundCar) {
			p = it.next();
			// we check if the actual position is between supposedP and p
			if (previousP != null) {
				if((carPos.x == p.x && Functions.between(previousP.y,p.y,carPos.y))
						|| (carPos.y == p.y && Functions.between(previousP.x,p.x,carPos.x))) {
					//we found where the car was in the circuit
					foundCar = true;
					previousP = carPos;
				}
			}
			if(!foundCar)
				previousP = p;
		}
		
		if(foundCar) {
			//we check in front of the car, within a distance.
			{
				// we check if the actual position is between supposedP and p
				if (previousP != null) {
					if((point.x == p.x && Functions.between(previousP.y,p.y,point.y))
							|| (point.y == p.y && Functions.between(previousP.x,p.x,point.x))) {
						//we found where the car was in the circuit
						found = true;
						if(distance>0) {
							distance -= Functions.manhattan(previousP, point);
							if(distance < 0){
								distance=0;
								found = false;
							}
								
						}
					}
				}
				//update distance
				
				if(distance>0) {
					distance -= Functions.manhattan(previousP, p);
					if(distance < 0)
						distance=0;
				}
				
				previousP = p;
				p = it.next();
				if(!it.hasNext() && distance>0) {
					it=itbis;
					System.out.println("one more turn of circuit to search the void");
				}
					
			}while(it.hasNext() && !found && distance >0);
		}
		return found;
	}
}