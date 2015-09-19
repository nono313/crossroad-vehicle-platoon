package fr.utbm.ia54.agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
	private OrientedPoint tmpPos;
	
	private Float vToReach;
	private float newV = 0;
	float refV = 0;
	private int safeD;
	private int crossingD;
	private int seeD;
	
	private String group;
	private int position;
	private int numTrain;
	private int carColor;
	
	private RotateLabel icone;
	private CarPath carPath;
	private HashMap<String, OrientedPoint> knownCars;
	private Queue<String> crossCars;
	private HashMap<String, Boolean> crossCarStatus;
	
	private boolean printingTurn = false;
	private String printings = new String();
	
	
	/**
	 * This is the first activated behavior in the life cycle of a MaDKit agent.
	 */
	@Override
	protected void activate() {
		carPath = MainProgram.getCarPath();
		numTrain = getNumTrain(group);
		pos = carPath.getStart(numTrain);

		requestRole(Const.MY_COMMUNITY,Const.CAR_ROLE,Const.CAR_ROLE);
		
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
		
		
		float distance = 0;
		float slowD = 0;
		float toSlowV = 0;
		int place =0;
		
		String closerInTrain = null;
		OrientedPoint closerPosInTrain;
		float toSlowVInTrain;
		String closerOutTrain = null;
		OrientedPoint closerPosOutTrain;
		float toSlowVOutTrain;
		
		Boolean isInTrain = true;
		
		Boolean messageToCar;
		
		ArrayList<OrientedPoint> crossings;
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
			printings += "I'm " + this.getName() + ", car " + position + " of train " + numTrain + ". My color is " + Const.CAR_COLOR[carColor] + ".\n";
			printings += this.getNetworkID() + "\n" + this.getSimpleNetworkID() + "\n";
			printings += "Speed to reach is " + vToReach + ", and safeD is " + safeD + "\n";
			
			
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
			printings += "Actual speed : "+ pos.speed + "\n";
			printings += "Optimal situation\nnewV = " + newV + "\n";
			
/* BACK TO REALITY ****************************************/
			neighbours = inRange(tmpPos, seeD, null);
			tmpKnownCars.putAll(neighbours);
			
			if(neighbours != null && !neighbours.isEmpty()) {
								
/* EMERGENCIES **********************************************************/
				emergencies = inRange(tmpPos, safeD, neighbours);
				crossings = carPath.getCrossingNear(tmpPos, seeD);
				
				
				/* clean emergencies from conflicts situations
				IF car is in another train AND out of the cross
					IF no priority defined
						define priority
					IF priority to us OR we are in the crossing already
						remove car from the list emergencies
						(& from neighbours ?!)
				IF car in emergencies, we stop the car as soon as possible
				END-IFS*/
				
				printings += "Cars in emergencies : " + emergencies + "\n";
				
				if(emergencies != null) {
					Iterator<String> itEmr = emergencies.keySet().iterator();
					List<String> tmpList = new ArrayList<String>();
					String carEmr;
					OrientedPoint carPosEmr;
					while(!emergencies.isEmpty() && itEmr.hasNext()) {
						carEmr = itEmr.next();
						carPosEmr = emergencies.get(carEmr);
						for(OrientedPoint tmpCross : crossings) {
							printings += "Other car is tested, its "+!Environment.isInMyTrain(this.getNetworkID(), carEmr) +" AND "+Functions.isOutside(carPosEmr, tmpCross);
							if (!Environment.isInMyTrain(this.getNetworkID(), carEmr) && Functions.isOutside(carPosEmr, tmpCross)) {
								printings += "not in my train AND outside the crossing;\n";
								if(definePriority(carEmr,carPosEmr,tmpCross) || Functions.manhattan(tmpPos,tmpCross)<Const.CAR_SIZE) {
									tmpList.add(carEmr);
									printings += "We remove " + carEmr + " from emergencies, for reasons \n " + getPriority(carEmr);
									printings += "\n " + (Functions.manhattan(tmpPos,tmpCross)<Const.CAR_SIZE) + "\n";
									
								}
								printings += "We do not remove " + carEmr + " from emergencies, for reasons \n " + getPriority(carEmr);
								printings += " OR " + (Functions.manhattan(tmpPos,tmpCross)<Const.CAR_SIZE) + "\n";
							}
						}
					}
					for(String i : tmpList) {
						emergencies.remove(i);
						neighbours.remove(i);
					}
				}
				
				
				if(emergencies != null && !emergencies.isEmpty()) {
					newV = (float) (pos.getSpeed() - (Const.DECC * (Const.PAS/1000.f)));
					distance = newV*(Const.PAS/1000.f);
					tmpPos = carPath.getNextPoint(pos, distance, numTrain);
					printings += "Emergencies : on essaie de s'arreter tant bien que mal.\n"; 
				} 
				else {
					//no car "emergencies", which would be too close for safety purposes
					
					
				//TODO multi(>2) trains, not operationnal at ALL. 

					printings += "No emergencies, we go for neighbors.\n"; 
					
					//search in neighbors for closer of our and other train.
					Set<String> keys = neighbours.keySet();
					Iterator<String> it = keys.iterator();
					String tmpNeighbour;
					OrientedPoint closerPos;
					
					while ((closerInTrain.isEmpty() || closerOutTrain.isEmpty()) && it.hasNext()) {
						tmpNeighbour = it.next();
						isInTrain = Environment.isInMyTrain(this.getNetworkID(), tmpNeighbour);
						if(closerInTrain.isEmpty() && isInTrain) {
							closerInTrain = Functions.closest(neighbours, tmpPos);
							closerPosInTrain = neighbours.get(closerInTrain);	

							printings += "We have a friend in front.\n"; 
						}
						else if (closerOutTrain.isEmpty() && !isInTrain) {
							closerPos = neighbours.get(tmpNeighbour);
							// we make sure that the two cars are heading for a common crossing
							if(Math.abs(closerPos.getOrientation()-pos.getOrientation()) == Math.toRadians(90) 
								|| Math.abs(closerPos.getOrientation()-pos.getOrientation()) == Math.toRadians(270)) {
								
								
								Iterator<OrientedPoint> it2 = crossings.iterator();
								while(it2.hasNext() && closerOutTrain == null) {
									cross = it2.next();
									if((closerPos.getX() == cross.getX()) && (tmpPos.getY() == cross.getY())) {
										
										if((closerPos.getY()<cross.getY() && closerPos.getOrientation() == Math.toRadians(180))||
											(closerPos.getY()>cross.getY() && closerPos.getOrientation() == Math.toRadians(0))) {
											closerOutTrain = Functions.closest(neighbours, tmpPos);
											closerPosOutTrain = neighbours.get(closerOutTrain);

											printings += "We have a car from another train in front.\n"; 
										}
									} 
									else if ((closerPos.getY() == cross.getY()) && (tmpPos.getX() == cross.getX())) {
										if((closerPos.getX()<cross.getX() && closerPos.getOrientation() == Math.toRadians(90))||
											(closerPos.getX()>cross.getX() && closerPos.getOrientation() == Math.toRadians(270))) {
											closerOutTrain = Functions.closest(neighbours, tmpPos);
											closerPosOutTrain = neighbours.get(closerOutTrain);
											
											printings += "We have a car from another train in front.\n";
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
						
						
						priority = definePriority(closerOutTrain,neighbours.get(closerOutTrain),cross);
						
						
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
					

					printings += "cars from the neibghors have us make slowing down : " + toSlowV + ".\n";
					
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
		carColor = (int) (Math.random() * Const.CAR_COLOR.length);
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
	 * @param printingTurn 
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
					
					printings += " objective speed is now " + Float.valueOf(data[1]) + "\n";
				}
				else if(data[0].equals("safe")) {
					safeD = Integer.valueOf(data[1]);
					seeD = 3*safeD;

					printings += " Safe distance is now " + Integer.valueOf(data[1]) + "\n";
				} 
				else if (data[0].equals("crossing")) {
					String id = new String();
					try {
						id = data[2].substring(0, message.getSender().getAgentNetworkID().length()-2);
					}
					catch (Exception e) {
						System.out.println("Error in messaging priority, we get the message" + message.getContent() + " from " + message.getSender() + " we are car : " + this.getNetworkID());
					}
					
					printings += id + " tels us that the priority between us is " + Boolean.valueOf(data[1]) + "\n";
					
					//TODO : check if the car is already in the pool, if so and different values ... fuck
					if(crossCars.contains(id)) {
							//we need to decide who will pass, cars having decided for opposit priority
							//DUMB solution : train 0 passes first
						if(!crossCarStatus.get(id).equals(Boolean.valueOf(data[1])) && this.numTrain == 0) {
							crossCarStatus.put(id, true);
						}
						else if (!crossCarStatus.get(id).equals(Boolean.valueOf(data[1])) && this.numTrain == 1) 
							crossCarStatus.put(id, false);
						else
							crossCarStatus.put(id, Boolean.valueOf(data[1]));
					} else {
						crossCars.add(id);
						crossCarStatus.put(id, Boolean.valueOf(data[1]));
					}
				} else if (data[0].equals("crossD")) {
					crossingD = Integer.valueOf(data[1]);
				}
				else if (data[0].equals("printPriority")) {
					String priorities = new String("Priorities of " + this.getName() + " (" + crossCarStatus.size() + ")");
					System.out.println(priorities + crossCarStatus);
				}
				else if(data[0].equals("Print")) {
					printingTurn = true;
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
		
		// if there are cars nearby we filter cars behind
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

		printings += " fin des questions, la vitesse definitive, c'est : " + newV + "\n";
		
		/*if ((newV < 0.9*refV || newV > 1.1*refV) && !crossCars.isEmpty()){
			//System.out.println("Priorities have become obsoletes for " + this.getName());
			crossCars.clear();
			refV = newV;
		}*/
		
		moveTo(tmpPos);
		HashMap<String, OrientedPoint> sendPos = new HashMap<String, OrientedPoint>();
		sendPos.put(this.getNetworkID(), tmpPos);
		sendMessage(Const.MY_COMMUNITY, group, Const.ENV_ROLE, new ObjectMessage<HashMap<String, OrientedPoint>>(sendPos));
		pos = tmpPos; 
		pos.setSpeed(newV);
		
		if(printingTurn)
			System.out.println(printings);
		printings = new String();
		printingTurn = false;
		
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
	
	protected boolean definePriority(String car2, OrientedPoint car2Pos, OrientedPoint cross) {
		printings += " We try to reach priority between us and " + car2;
		int priorityInt;
		try {
			priorityInt = getPriority(car2);
		}
		catch (NullPointerException e) {
			printings += "priorities not found, we build it.\n";
			priorityInt = -1;
		}
		boolean priority = false;
		if(priorityInt >= 0) {
			priority = (priorityInt == 1) ? true : false;

			 printings += ",the priority was defiend between us and is " + priority + "\n";
		}
		else {
			//no defined priority
			printings += ",the priority was NOT defiend between us.\n";
			//Priority goes to closest and fastest car (the one that could get out faster)
			//we then inform that car
			long dToCross = Functions.manhattan(tmpPos, cross)+Const.CAR_SIZE;
			long newD = Functions.manhattan(car2Pos, cross)+Const.CAR_SIZE;
			printings +=  " dToCross/newV=" + dToCross/newV;
			printings +=  " newD/neighbours.get(closerOutTrain).getSpeed()=" + newD/car2Pos.getSpeed();
			if(dToCross/(newV+1) < newD/(car2Pos.getSpeed()+1)) {
				priority = true;
				//System.out.println(this.getNetworkID() + " have priority over " + car2);
			}
			else
				priority = false;
			
			// send it to the car directly
			// need to remove role creation with networkId in activate
			printings += " We try to reach priority between us and " + car2 + ",it didn't existed, so we decided for " + priority + "\n";
			String tmp = "crossing:"+ ((Boolean)(!priority)).toString()+":"+this.getNetworkID();
			MainProgram.getEnv().sendMessageToId(car2, tmp);
			//sendMessage(closerOutTrain, tmp);
			crossCars.add(car2);
			crossCarStatus.put(car2, priority);
		}
		
		return priority;
	}
	
	
	protected int getPriority(String name) {
		boolean priority = false;
		if(!crossCars.peek().isEmpty() && crossCars.peek().equals(name)) {
			// check for priority
			priority = crossCarStatus.get(name);
		} 
		else if(!crossCars.isEmpty() && crossCars.contains(name)) {
			// need to clean queue until reach closer
			while(!crossCars.peek().equals(name)) {
				crossCarStatus.remove(crossCars.poll());
			}
			priority = crossCarStatus.get(name);
		} 
		else {
			return -1;
		}
		
		if(priority)
			return 1;
		else
			return 0;
	}
}
