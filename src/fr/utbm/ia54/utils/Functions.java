package fr.utbm.ia54.utils;

import java.util.HashMap;

import fr.utbm.ia54.consts.Const;

/**
 * Utils functions class.
 * @author Alexis
 *
 */
public class Functions {

	/**
	 * true if x is between a and b, false otherwise
	 * @param a
	 * @param b
	 * @param x
	 * @return
	 */
	public static boolean between(double a, double b, double x) {
		if(a<=b) {
			if(x>=a && x<=b) {
				return true;
			}
		} else {
			if(x>=b && x<=a) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static double manhattanCar(OrientedPoint a, OrientedPoint b) {
		double distance = manhattan(a,b);

		if(distance > Const.CAR_SIZE) {
			distance -= Const.CAR_SIZE;
		} else {
			distance = 1;
		}
		return distance;
	}
	
	public static double manhattan(OrientedPoint a, OrientedPoint b) {
		return (Math.abs(a.x-b.x)+ Math.abs(a.y-b.y));
	}

	public static boolean sEloigne(OrientedPoint other, OrientedPoint we) {
		OrientedPoint tmp = new OrientedPoint(other);
		
		// we try to know if the other car is going away or comming, by comparing manhattan of actual pos and virtual one where the other car has moved forward
		double actualM = manhattanCar(other, we);
		if(tmp.orientation == 2*Math.PI || tmp.orientation == 0) {
			tmp.y--;
		} else if (tmp.orientation == Math.PI/2){
			tmp.x++;
		} else if (tmp.orientation == Math.PI){
			tmp.y++;
		} else{
			tmp.x--;
		}
		
		if(manhattanCar(tmp, we) > actualM) {
			return true;
		}
		return false;
	}
	
	public static boolean sApproche(OrientedPoint car, OrientedPoint tmpPos) {
		return !sEloigne(car, tmpPos);
	}

	public static String closest(HashMap<String, OrientedPoint> list, OrientedPoint p) {
		String closer = new String();
		int manhC = -1;
		if(list !=null) {
			if (!list.isEmpty()) {
				for(String it : list.keySet()) {
					if(manhC < 0) {
						closer = it;
					} else if(manhattanCar(list.get(it), p) < manhattanCar(list.get(closer), p)) {
						closer = it;
					}
				}
				return closer;
			}
			return null;
		}
		return null;
	}
	

	public static String closestInTrain(HashMap<String, OrientedPoint> list, OrientedPoint p) {
		String closer = new String();
		int manhC = -1;
		if(list !=null) {
			if (!list.isEmpty()) {
				for(String it : list.keySet()) {
					if(manhC < 0) {
						closer = it;
					} else if(manhattanCar(list.get(it), p) < manhattanCar(list.get(closer), p)) {
						closer = it;
					}
				}
				return closer;
			}
			return null;
		}
		return null;
	}	
	
	
	public static boolean isBehind(OrientedPoint other, OrientedPoint we) {
		//other is behind we if we moving coordinate > other corresponding coordinate
		if((we.orientation == 2*Math.PI || we.orientation == 0) && we.y < other.y) {
			return true;
		} else if ((we.orientation == Math.PI) && we.y > other.y) {
			return true;
		} else if ((we.orientation == Math.PI/2) && we.x > other.x) {
			return true;
		} else if ((we.orientation == 3*Math.PI/2) && we.x < other.x) {
			return true;
		} else {
			return false;
		}
	}	
	
	
	public static boolean isOutside(OrientedPoint car, OrientedPoint cross) {
		if(isBefore(car, cross) || isAfter(car, cross))
			return true;
		return false;
	}
	
	public static boolean isBefore(OrientedPoint car, OrientedPoint cross) {
		
		if((car.orientation == 2*Math.PI || car.orientation == 0) && car.y > cross.y+Const.CAR_SIZE) {
			return true;
		} else if ((car.orientation == Math.PI) && car.y < cross.y-Const.CAR_SIZE) {
			return true;
		} else if ((car.orientation == Math.PI/2) && car.x < cross.x-Const.CAR_SIZE) {
			return true;
		} else if ((car.orientation == 3*Math.PI/2) && car.x > cross.x+Const.CAR_SIZE) {
			return true;
		} else {
			return false;
		}
	}
	public static boolean isAfter(OrientedPoint car, OrientedPoint cross) {
		
		if((car.orientation == 2*Math.PI || car.orientation == 0) && car.y+Const.CAR_SIZE < cross.y) {
			return true;
		} else if ((car.orientation == Math.PI) && car.y-Const.CAR_SIZE > cross.y) {
			return true;
		} else if ((car.orientation == Math.PI/2) && car.x-Const.CAR_SIZE > cross.x) {
			return true;
		} else if ((car.orientation == 3*Math.PI/2) && car.x+Const.CAR_SIZE < cross.x) {
			return true;
		} else {
			return false;
		}
	}
}
