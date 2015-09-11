package fr.utbm.ia54.agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import javax.swing.ImageIcon;

import madkit.kernel.Agent;
import madkit.kernel.Message;
import madkit.message.ObjectMessage;
import madkit.message.StringMessage;
import fr.utbm.ia54.consts.Const;
import fr.utbm.ia54.main.MainProgram;
import fr.utbm.ia54.path.CarPath;
import fr.utbm.ia54.utils.Functions;
import fr.utbm.ia54.utils.OrientedPoint;
import fr.utbm.ia54.utils.RotateLabel;

/**
 * Car class.
 * @author Alexis Florian
 */
public class Car extends Agent {

	private OrientedPoint pos;
	
	private Float vToReach;
	private int safeD;
	private int crossingD;
	private int seeD;
	
	private String group;
	private int position;
	private int numTrain;
	
	private RotateLabel icone;
	private CarPath carPath;
	private HashMap<String, OrientedPoint> knownCars;
	private Queue<String> crossCars;
	private HashMap<String, Boolean> crossCarStatus;
	
	/**
	 * This is the first activated behavior in the life cycle of a MaDKit agent.
	 */
	@Override
	protected void activate() {
		carPath = MainProgram.getCarPath();
		numTrain = getNumTrain(group);
		pos = carPath.getStart(numTrain);

		requestRole(Const.MY_COMMUNITY,group,Const.CAR_ROLE);
		requestRole(Const.MY_COMMUNITY,group, this.getNetworkID());
		
		addCar();
		
		pos.setSpeed(10);
		vToReach=(float) 110;
		vToReach+= (0.05f*position*vToReach);
		safeD = 30;
		seeD = 120;
		
		knownCars = new HashMap<String, OrientedPoint>();
		crossCars = new LinkedList<String>();
		crossCarStatus = new HashMap<String, Boolean>();
	}


	/**
	 * This is the second behavior which is activated, i.e. when activate ends.
	 * It actually implements the life of the agent.
	 * It is usually a while true loop.
	 */
	@Override
	protected void live() {
		boolean live = true;
		
		float newV = 0;
		float distance = 0;
		float slowD = 0;
		float toSlowV = 0;
		int place =0;
		OrientedPoint tmpPos;
		
		String closerInTrain = null;
		OrientedPoint closerPosInTrain;
		float toSlowVInTrain;
		String closerOutTrain = null;
		OrientedPoint closerPosOutTrain;
		float toSlowVOutTrain;
		
		Boolean isInTrain = true;
		
		Boolean messageToCar;
		
		OrientedPoint cross = null;
		Boolean priority = false;
		
		HashMap<String, OrientedPoint> neighbours = new HashMap<String, OrientedPoint>();
		HashMap<String, OrientedPoint> emergencies = new HashMap<String, OrientedPoint>();
		HashMap<String, OrientedPoint> tmpKnownCars = new HashMap<String, OrientedPoint>();
		
		// we drive for a while
		while(live) {
			closerInTrain = new String();
			closerOutTrain = new String();
			toSlowVInTrain = 0;
			toSlowVOutTrain = 0;
			newV = 0;
			distance = 0;
			slowD = 0;
			toSlowV = 0;
			place = 10;
			messageToCar = null;
			
			//if(pos.getSpeed() == 0)
			//	System.out.println("that's me stopped : " + this.getNetworkID());
			
			// we treat all new messages
			getNewMessages();
			
/*OPTIMAL SITUATION ***************************************************/
			// adapt speed according to speed objectives and ACC-eleration and DECC-eleration
			if(pos.getSpeed() == vToReach) {
				newV = (float) pos.getSpeed();
			}
			else {
				if(pos.getSpeed() <vToReach) {
					newV = (float) (pos.getSpeed() + (Const.ACC * (Const.PAS/1000.f)));
					// we have accelerated enough to reach the desired speed
					if(newV > vToReach) {
						newV = vToReach;
					}
				} 
				else {
					newV = (float) (pos.getSpeed() - (Const.DECC * (Const.PAS/1000.f)));
					// we have deccelerated enough to reach the desired speed
					if(newV < vToReach) {
						newV = vToReach;
					}
				}
			}
			
			distance = newV*(Const.PAS/1000.f);
			tmpPos = carPath.getNextPoint(pos, distance, numTrain);
			tmpKnownCars = new HashMap<String, OrientedPoint>();
			
/* BACK TO REALITY ****************************************/
			neighbours = inRange(tmpPos, seeD, null);
			tmpKnownCars.putAll(neighbours);
			
			if(neighbours != null && !neighbours.isEmpty()) {
								
/* EMERGENCIES **********************************************************/
				//In case of Vehicule in that area, we stop the car
				emergencies = inRange(tmpPos, safeD, neighbours);
				if(emergencies != null && !emergencies.isEmpty()) {
					if(crossCars.peek() != null && crossCars.peek().equals(emergencies.get(0))) {
						// check for priority
						priority = crossCarStatus.get(emergencies.get(0));
					} 
					else if(!crossCars.isEmpty() && crossCars.contains(emergencies.get(0))) {
						// need to clean queue until reach closer
						while(!crossCars.peek().equals(emergencies.get(0))) {
							crossCarStatus.remove(crossCars.poll());
						}
						priority = crossCarStatus.get(emergencies.get(0));
					} else {
						priority = false;
					}

					if(!priority) {
						newV = (float) (pos.getSpeed() - (Const.DECC * (Const.PAS/1000.f)));
						distance = newV*(Const.PAS/1000.f);
						tmpPos = carPath.getNextPoint(pos, distance, numTrain);
					}
					
				} 
				else {
					//no car "emergencies", which would be too close for safety purposes
						
				//we have to consider different safeD according to the train of the car.
					
/* CROSSING *******************************************************************************/
				//TODO multi trains	
				
					
					//search in neighbors for closer of our and other train.
					Set<String> keys = neighbours.keySet();
					Iterator<String> it = keys.iterator();
					String tmpNeighbour;
					OrientedPoint closerPos;
					
					while ((closerInTrain.isEmpty() || closerOutTrain.isEmpty()) && it.hasNext()) {
						tmpNeighbour = it.next();
						isInTrain = Environment.isInMyTrain(this.getName(), tmpNeighbour);
						if(closerInTrain.isEmpty() && isInTrain) {
							closerInTrain = Functions.closest(neighbours, tmpPos);
							closerPosInTrain = neighbours.get(closerInTrain);	
						}
						else if (closerOutTrain.isEmpty() && !isInTrain) {
							closerPos = neighbours.get(tmpNeighbour);
							// we make sure that the two cars are heading for a common crossing
							if(Math.abs(closerPos.getOrientation()-pos.getOrientation()) == Math.toRadians(90) 
								|| Math.abs(closerPos.getOrientation()-pos.getOrientation()) == Math.toRadians(270)) {
								
								ArrayList<OrientedPoint> crossings = carPath.getCrossingNear(tmpPos, seeD);
								Iterator<OrientedPoint> it2 = crossings.iterator();
								while(it2.hasNext() && closerOutTrain == null) {
									cross = it2.next();
									if((closerPos.getX() == cross.getX()) && (tmpPos.getY() == cross.getY())) {
										
										if((closerPos.getY()<cross.getY() && closerPos.getOrientation() == Math.toRadians(180))||
											(closerPos.getY()>cross.getY() && closerPos.getOrientation() == Math.toRadians(0))) {
											closerOutTrain = Functions.closest(neighbours, tmpPos);
											closerPosOutTrain = neighbours.get(closerOutTrain);
										}
									} 
									else if ((closerPos.getY() == cross.getY()) && (tmpPos.getX() == cross.getX())) {
										if((closerPos.getX()<cross.getX() && closerPos.getOrientation() == Math.toRadians(90))||
											(closerPos.getX()>cross.getX() && closerPos.getOrientation() == Math.toRadians(270))) {
											closerOutTrain = Functions.closest(neighbours, tmpPos);
											closerPosOutTrain = neighbours.get(closerOutTrain);
										}
									}
								}
							}
						}
					}
					
					int newD = 0;
					double dToCross;
					
					if (!closerInTrain.isEmpty()) {
						/*
						 TODO : si même qu'avant, estimation rapide de l'accélération avec coefitient de conservation de l'ancienne valeur
						
						différence vitesse
						temps restant avant safeD
						
						nombre d'etapes restantes et nombre d'etapes necessaires au freinage
						
						=> acceleration pour suivi*/
						
						
						if(knownCars.containsKey(closerInTrain)) {
							newD = Functions.manhattanCar(neighbours.get(closerInTrain), tmpPos);
							float diffV = (float) (newV - neighbours.get(closerInTrain).getSpeed());
							
							if(diffV > 0) {
								
								float margeT = (newD-safeD+10)/newV;
								
								int nbEtapes = (int) (margeT/Const.PAS);
								if(nbEtapes <=0) {
									nbEtapes=1;
								}
								int i = 0;
								do {
									i++;
								}while (Const.DECC * (Const.PAS/1000.f) * i < diffV );
								
								if (nbEtapes <= i) {
									toSlowVInTrain = Const.DECC * (Const.PAS/1000.f);
								} 
								else {
									toSlowVInTrain = 0;
								}
							} 
							else {
								toSlowVInTrain = 0;
							}
						}
						
					}
						
					
					if(!closerOutTrain.isEmpty()) {
						/*
						 *TODO :  si même qu'avant estimation rapide de l'acélération avec prise en compte de l'ancien calcul (selon priorité)
					
						definir priorité 
						
						si priorité
							on roule a balle
						si non
							estimer temps/ distance avant libération du carfour par prioritaire
							déduire vmax, accélaration de croisement
						 * 
						 */
							
						dToCross = Functions.manhattan(tmpPos, cross)+Const.CAR_SIZE;
						
						
						if(crossCars.peek() != null && crossCars.peek().equals(closerOutTrain)) {
							// check for priority
							priority = crossCarStatus.get(closerOutTrain);
						} 
						else if(!crossCars.isEmpty() && crossCars.contains(closerOutTrain)) {
							// need to clean queue until reach closer
							while(!crossCars.peek().equals(closerOutTrain)) {
								crossCarStatus.remove(crossCars.poll());
							}
							priority = crossCarStatus.get(closerOutTrain);
						} 
						else {
							//no defined priority
							//Priority goes to closest and fastest car
							//we then inform that car
							newD = Functions.manhattanCar(neighbours.get(closerOutTrain), cross)+Const.CAR_SIZE;
							System.out.println(this.getNetworkID() + " dToCross/newV=" + dToCross/newV);
							System.out.println(this.getNetworkID() + " newD/neighbours.get(closerOutTrain).getSpeed()=" + newD/neighbours.get(closerOutTrain).getSpeed());
							if(dToCross/newV < newD/neighbours.get(closerOutTrain).getSpeed()) {
								priority = true;
								System.out.println(this.getNetworkID() + " have priority over " + closerOutTrain);
							}
							else
								priority = false;
							
							// send it to the car directly
							// need to remove role creation with networkId in activate
							String tmp = "crossing:"+ ((Boolean)(!priority)).toString()+":"+this.getNetworkID();
							MainProgram.getEnv().sendMessageToId(closerOutTrain, tmp);
							crossCars.add(closerOutTrain);
							crossCarStatus.put(closerOutTrain, priority);
								
						}
						
						if(!priority) {
							//do we have to reduce speed ?
							if ((newD+safeD)/neighbours.get(closerOutTrain).getSpeed() > dToCross/newV) {
								//if so by how much ?
								toSlowVOutTrain =  ((float)((newD+safeD)/neighbours.get(closerOutTrain).getSpeed() - dToCross/newV)/(float)newD);
							}
						}
						else
							//TODO : worry about next car to cross over ?
							toSlowVOutTrain = 0;
						
					}
					
					
					//We now now how much we need to slow down for each situation. We take care of the most important one
					toSlowV = (toSlowVOutTrain < toSlowVInTrain) ? toSlowVInTrain : toSlowVOutTrain ;
					
					knownCars = tmpKnownCars;				
				}//no emergencies
			}//no neighbours
/* APPLICATION OF WHAT IS PLANNED ********************/
			executingRun(newV, toSlowV, distance, tmpPos);
			pause(Const.PAS);
		}//while live
	}


	/**
	 * This behavior is called when the agent has finished its live behavior.
	 * Because there is no other agent, MaDKit quits when the agent is terminated.
	 * 
	 */
	@Override
	protected void end() {
		leaveRole(Const.MY_COMMUNITY, group,Const.CAR_ROLE);
		MainProgram.getMainFrame().getSuperposition().remove(icone);
		MainProgram.getMainFrame().getSuperposition().repaint();
	}
	
	
	/* Functions used by the car */
	
	public void addCar(){
		/* Car representation */
		int carColor = (int) (Math.random() * Const.CAR_COLOR.length);
		ImageIcon car = new ImageIcon(Const.RESOURCES_DIR+"/"+Const.CAR_COLOR[carColor]);
		icone = new RotateLabel(car);
   		icone.setBounds(0,0,Const.CAR_SIZE, Const.CAR_SIZE);
   		icone.setLocation(pos.x, pos.y);
   		icone.setAngle(pos.getAngle());
   		
   		/* Add the car to the frame */
   		MainProgram.getMainFrame().getSuperposition().add(icone,1,0);
	}
	
	/**
	 * Update coordinates and orientation of the car visually
	 * @param tmpPos
	 */
	private void moveTo(OrientedPoint tmpPos) {
		icone.setLocation(tmpPos.x, tmpPos.y);
		icone.setAngle(tmpPos.orientation);
	}
	

	/**
	 * Check for new messages and updates internal parameters
	 */
	private void getNewMessages() {
		Message m = null;
		
		//we manage all messages
		while (!isMessageBoxEmpty()){
			m = nextMessage();
			
			if(m instanceof StringMessage) {
				StringMessage message = (StringMessage) m;
				String[] data = message.getContent().split(":");
				if(data[0].equals("speed")) {
					vToReach = Float.valueOf(data[1]);
					// correction for train fusion
					vToReach += (0.01f*position*vToReach);

				}
				else if(data[0].equals("safe")) {
					safeD = Integer.valueOf(data[1]);
					seeD = 3*safeD;
				} 
				else if (data[0].equals("crossing")) {
					String id = data[2].substring(0, message.getReceiver().getAgentNetworkID().length()-2);
					//TODO : check if the car is already in the pool, if so and different values ... fuck
					if(crossCars.contains(id)) {
						if(!crossCarStatus.get(id).equals(Boolean.valueOf(data[1]))) {
							//we need to decide who will pass, cars having decided for opposit priority
						}
					} else {
						crossCars.add(id);
						crossCarStatus.put(id, Boolean.valueOf(data[1]));
					}
				} else if (data[0].equals("crossD")) {
					crossingD = Integer.valueOf(data[1]);
				} 
			}
		}
	}

	public void setGroup(String group2) {
		this.group = group2;
	}

	public void setPosition(int i) {
		this.position = i;
		
	}
	
	/**
	 * Get the number of the train
	 * @param group
	 * @return
	 */
	private int getNumTrain(String group) {
		char c = group.charAt(group.length()-1);
		return Character.getNumericValue(c);
	}
	

	private HashMap<String, OrientedPoint> inRange(OrientedPoint tmpPos, int range, HashMap<String, OrientedPoint> population) {
		HashMap<String, OrientedPoint> exclus = new HashMap<String, OrientedPoint>();
		HashMap<String, OrientedPoint> neighbours = new HashMap<String, OrientedPoint>();
		HashMap<String, OrientedPoint> neigh2 = new HashMap<String, OrientedPoint>();
		
		exclus.put( this.getNetworkID(), pos);
		
		if(population == null) {
			neighbours = MainProgram.getEnv().inRange(tmpPos, range, exclus);
		} else {
			neighbours = MainProgram.getEnv().inRange(tmpPos, range, population, exclus);
		}
		
		// if there are cars nearby
		if(neighbours != null && !neighbours.isEmpty()) {
			for(String ad : neighbours.keySet()) {
				if(!Functions.estDerriere(neighbours.get(ad),tmpPos)) {
					neigh2.put(ad, neighbours.get(ad));
				}
			}
		}
		return neigh2;
	}
	
	private void executingRun(float newV, float toSlowV, float distance, OrientedPoint tmpPos) {
		// we slow down as much as possible
		if (Const.DECC * (Const.PAS/1000.f) > toSlowV) {
			// we can slow down as much as we need
			newV -= toSlowV;
		} 
		else {
			// we slow the max we can and hope for the best
			newV -= Const.DECC * (Const.PAS/1000.f);
		}
		
		// we avoid going backward
		if(newV > 0) {
			distance = newV*(Const.PAS/1000.f);
			tmpPos = carPath.getNextPoint(pos, distance, numTrain);
		} 
		else {
			newV = 0;
			distance = 0;
			tmpPos = pos;
		}
		moveTo(tmpPos);
		HashMap<String, OrientedPoint> sendPos = new HashMap<String, OrientedPoint>();
		sendPos.put(this.getNetworkID(), tmpPos);
		sendMessage(Const.MY_COMMUNITY, group, Const.ENV_ROLE, new ObjectMessage<HashMap<String, OrientedPoint>>(sendPos));
		pos = tmpPos; 
		pos.setSpeed(newV);
	}


	public OrientedPoint getPos() {
		return pos;
	}


	public void setPos(OrientedPoint pos) {
		this.pos = pos;
	}
	
	public int timeToCross(OrientedPoint goal, OrientedPoint start) {
		int toRun = Functions.manhattan(start, goal);
		return (int) (toRun/pos.speed);
	}
}
