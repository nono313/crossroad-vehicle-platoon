package fr.utbm.ia54.path;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;

import fr.utbm.ia54.utils.Functions;
import fr.utbm.ia54.utils.OrientedPoint;

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
		this.path = new ArrayList<>();
		this.crossing = new ArrayList<>();
	}

	public List<List<OrientedPoint>> getPath() {
		return this.path;
	}

	public void setPath(List<List<OrientedPoint>> path) {
		this.path = path;
	}

	public ImageIcon getBackground() {
		return this.background;
	}

	public void setBackground(ImageIcon background) {
		this.background = background;
	}
	
	@Override
	public String toString() {
		String str = ""; //$NON-NLS-1$
		for(List<OrientedPoint> a : this.path) {
			for(OrientedPoint o : a)  {
				str += o.toString()+"\n"; //$NON-NLS-1$
			}
		}
		return str;
	}
	
	/** 
	 * Search for position on a road, 
	 * compute distance to travel and position, 
	 * return new position
	 */
	public OrientedPoint getNextPoint(OrientedPoint actualP, double distance, int numTrain) {
		OrientedPoint nextPosition = new OrientedPoint(actualP);
		OrientedPoint previousCorner = null;
		
		Iterator<OrientedPoint> it = this.path.get(numTrain).iterator();
		
		// for now we suppose the car is on the road, and we try to find it (between 2 points/corners)
		while(it.hasNext() && distance > 0) {
			OrientedPoint nextCorner = it.next();
			// we check if the actual position is between supposedP and p
			if (previousCorner != null) {
				if((nextPosition.x == nextCorner.x && Functions.between(previousCorner.y,nextCorner.y,nextPosition.y))
						|| (nextPosition.y == nextCorner.y && Functions.between(previousCorner.x,nextCorner.x,nextPosition.x))) {
					//if we move in y
					if(previousCorner.x == nextCorner.x) {
						//if no needs to rotate
						if(nextPosition.y + distance < nextCorner.y && nextPosition.orientation ==Math.PI) {
							nextPosition = new OrientedPoint(nextPosition);
							nextPosition.y = nextPosition.y + distance;
							distance = 0;
						} 
						else if (nextPosition.y - distance > nextCorner.y && (nextPosition.orientation == Math.PI*2 || nextPosition.orientation == 0)) {
							nextPosition = new OrientedPoint(nextPosition);
							nextPosition.y = nextPosition.y - distance;
							distance = 0;
						// in case of arriving just on the point	
						} 
						else if (nextPosition.y - distance == nextCorner.y || nextPosition.y + distance == nextCorner.y) {
							nextPosition = new OrientedPoint(nextCorner);
							distance = 0;
						} 
						else { // we need to rotate
							nextPosition = new OrientedPoint(nextCorner);
							previousCorner = nextCorner;
							distance -= Functions.manhattan(nextPosition, nextCorner);
						}
					} 
					else { // if we move on x
						// if no needs to rotate
						if(nextPosition.x + distance < nextCorner.x && nextPosition.orientation ==Math.PI/2) {
							nextPosition = new OrientedPoint(nextPosition);
							nextPosition.x = nextPosition.x + distance;
							distance = 0;
						}
						else if (nextPosition.x - distance > nextCorner.x && nextPosition.orientation == Math.PI*3/2) {
							nextPosition = new OrientedPoint(nextPosition);
							nextPosition.x = nextPosition.x - distance;
							distance = 0;
							
						// in case of arriving just on the point	
						} 
						else if (nextPosition.x - distance == nextCorner.x || nextPosition.x + distance == nextCorner.x) {
							nextPosition = new OrientedPoint(nextCorner);
							distance = 0;
						
						} 
						else { // we need to rotate
							nextPosition = new OrientedPoint(nextCorner);
							previousCorner = nextCorner;
							distance -= Functions.manhattan(nextPosition, nextCorner);
						}
					}
				} 
				else {
					// we continue searching
					previousCorner = null;
				}
				
			}
			// we avoid a lot of research by putting away all roads not oriented like the car
			if(nextCorner.orientation == nextPosition.orientation) {
				if(nextCorner.x == nextPosition.x || nextCorner.y == nextPosition.y) {
					previousCorner = nextCorner;
				}
			}
		}		
		return nextPosition;
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
		List<List<OrientedPoint>> pathCopy = new ArrayList<>(this.path); // Copy the path list
		List<OrientedPoint> current = new ArrayList<>();
		List<OrientedPoint> tmp = new ArrayList<>();
		OrientedPoint startCurrent = new OrientedPoint();
		OrientedPoint endCurrent = new OrientedPoint();
		OrientedPoint startTmp = new OrientedPoint();
		OrientedPoint endTmp = new OrientedPoint();
		
		// For all train path
		for(int i=0;(i+1)<this.path.size();i++){
			current = pathCopy.get(0);

			for(int l=1;l<this.path.size();l++){
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
	private static OrientedPoint isCrossed(OrientedPoint startCurrent, OrientedPoint endCurrent, OrientedPoint startTmp, OrientedPoint endTmp) {
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
		return this.crossing;
	}

	public ArrayList<OrientedPoint> getCrossingNear(OrientedPoint pos, int range) {
		ArrayList<OrientedPoint> concerned = new ArrayList<>();
		
		for (OrientedPoint it : this.crossing) {
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
		List<List<OrientedPoint>> pathCopy = new ArrayList<>(this.path); // Copy the path list
		List<OrientedPoint> current = new ArrayList<>();
		List<OrientedPoint> tmp = new ArrayList<>();
		OrientedPoint startCurrent = new OrientedPoint();
		OrientedPoint endCurrent = new OrientedPoint();
		OrientedPoint startTmp = new OrientedPoint();
		OrientedPoint endTmp = new OrientedPoint();
		
		// For all train path
		for(int i=0;(i+1)<this.path.size();i++){
			current = pathCopy.get(0);

			for(int l=1;l<this.path.size();l++){
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
		System.out.println(this.crossing);
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
		Iterator<OrientedPoint> it = this.path.get(train).iterator();
		Iterator<OrientedPoint> itbis = this.path.get(train).iterator();
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
					assert(p!=null);
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
					System.out.println("one more turn of circuit to search the void"); //$NON-NLS-1$
				}
					
			}
			while(it.hasNext() && !found && distance >0){
				/**
				 * @TODO : Voir à quoi ça sert
				 * Etonnant ça non ?
				 */
			}
		}
		return found;
	}
}