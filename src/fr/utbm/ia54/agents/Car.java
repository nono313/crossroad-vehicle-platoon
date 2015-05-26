package fr.utbm.ia54.agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

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
		String closer = null;
		OrientedPoint closerPos;
		Boolean messageToCar;
		OrientedPoint cross = null;
		boolean priority = false;
		boolean crossing = false;
		HashMap<String, OrientedPoint> neighbours = new HashMap<String, OrientedPoint>();
		HashMap<String, OrientedPoint> emergencies = new HashMap<String, OrientedPoint>();
		HashMap<String, OrientedPoint> tmpKnownCars = new HashMap<String, OrientedPoint>();
		
		// we drive for a while
		while(live) {
			closer = new String();
			newV = 0;
			distance = 0;
			slowD = 0;
			toSlowV = 0;
			place = 10;
			messageToCar = null;
			
			// we treat all new messages
			getNewMessages();
			
			/*OPTIMAL SITUATION */
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
			
			/* BACK TO REALITY */
			neighbours = inRange(tmpPos, seeD, null);
			
			if(neighbours != null && !neighbours.isEmpty()) {
				tmpKnownCars.putAll(neighbours);
				closer = Functions.closest(neighbours, tmpPos);
				closerPos = neighbours.get(closer);

			//we have to consider different safeD according to the train of the car.	
				
				
				
				/* CROSSING */
				if(crossCars.peek() != null && crossCars.peek().equals(closer)) {
					// check for priority
					priority = crossCarStatus.get(closer);
					crossing = true;
				} 
				else if(!crossCars.isEmpty() && crossCars.contains(closer)) {
					// need to clean queue until reach closer
					while(!crossCars.peek().equals(closer)) {
						crossCarStatus.remove(crossCars.poll());
					}
					priority = crossCarStatus.get(closer);
					crossing = true;
				} 
				else {
					// if both cars are coming near a crossing and are on different roads
					if(Math.abs(closerPos.getOrientation()-pos.getOrientation()) == Math.toRadians(90) 
							|| Math.abs(closerPos.getOrientation()-pos.getOrientation()) == Math.toRadians(270)) {
						
						ArrayList<OrientedPoint> crossings = carPath.getCrossingNear(tmpPos, seeD);
						Iterator<OrientedPoint> it = crossings.iterator();
						while(messageToCar == null && it.hasNext()) {
							cross = it.next();
							// we make sure that the two cars are heading for a common crossing
							if((closerPos.getX() == cross.getX()) && (tmpPos.getY() == cross.getY())) {
								// we are closer
								if(Math.abs(closerPos.getY() - cross.getY()) > Math.abs(tmpPos.getX() - cross.getX())) {
									messageToCar = new Boolean(false);
									priority = true;
								} 
								else {
									messageToCar = new Boolean(true);
									priority = false;
								}
							} 
							else if ((closerPos.getY() == cross.getY()) && (tmpPos.getX() == cross.getX())) {
								if(Math.abs(closerPos.getX() - cross.getX()) > Math.abs(tmpPos.getY() - cross.getY())) {
									messageToCar = new Boolean(false);
									priority = true;
								} 
								else {
									messageToCar = new Boolean(true);
									priority = false;
								}
							}
						}
						
						if(messageToCar != null) {
							// send it to the car directly
							// need to remove role creation with networkId in activate
							String tmp = "crossing:"+messageToCar.toString()+":"+this.getNetworkID();
							MainProgram.getEnv().sendMessageToId(closer, tmp);
							crossCars.add(closer);
							crossCarStatus.put(closer, priority);
							crossing = true;
						} 
						else {
							priority = false;
							crossing = false;
						}
					} 
					else {
						priority = false;
						crossing = false;
					}
				}
				//we know this car's ancient position, and we have to let her pass => adapting speed
				if(!priority && crossing) {
					if(knownCars.containsKey(closer)) {
						ArrayList<OrientedPoint> crossings = carPath.getCrossingNear(tmpPos, seeD);
						Iterator<OrientedPoint> it = crossings.iterator();
						while(it.hasNext()) {
							cross = it.next();
							// we make sure that the two cars are heading for a common crossing
							if(((closerPos.getX() == cross.getX()) && (tmpPos.getY() == cross.getY())) || ((closerPos.getY() == cross.getY()) && (tmpPos.getX() == cross.getX()))) {
								// this is the crossing
								int otherLeftToRun =  Functions.manhattan(neighbours.get(closer), cross);
								int otherAlreadyRun = Functions.manhattan(knownCars.get(closer), cross) - otherLeftToRun;
								int timeLeft = 0;
								if(otherAlreadyRun >0) {
									timeLeft = otherLeftToRun/otherAlreadyRun;
									if(otherLeftToRun%otherAlreadyRun != 0) {
										timeLeft++;
									}
								}
								
								int ourLeftToRun = Functions.manhattanCar(tmpPos, cross) - safeD;
								if(ourLeftToRun < 0) {
									ourLeftToRun = 0;
								}
								
								if(timeLeft == 0) {
									
								} 
								else {
									toSlowV = (float) (2*Math.abs(pos.getSpeed() - (slowD / timeLeft)));
								}
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
							}
						}
					}
				}
				
				
				if (!crossing) {
					emergencies = inRange(tmpPos, safeD, neighbours);
					/* EMERGENCIES */
					if(emergencies != null && !emergencies.isEmpty()) {

						closer = Functions.closest(emergencies, tmpPos);
						place =  Functions.manhattanCar(tmpPos, emergencies.get(closer));
						slowD = (safeD - place);
						// what we have to deccelerate
						toSlowV = slowD / (Const.PAS/1000.f);
					} 
					else {
						/* WARNINGS */
						// if first one stable, warn about next */
						closer = null;
						int oldD = 0;
						int newD = 0;
						do {
							// search for a registered car
							do {
								neighbours.remove(closer);
								closer = Functions.closest(neighbours, tmpPos);
							} while(!knownCars.containsKey(closer) && !neighbours.isEmpty());
							// we take the closest who is getting closer
							if(!neighbours.isEmpty()) {
								oldD = Functions.manhattanCar(knownCars.get(closer), pos);
								newD = Functions.manhattanCar(neighbours.get(closer), tmpPos);
							}
						} while(oldD == newD && !neighbours.isEmpty());
						
						if(!neighbours.isEmpty()){
							// the purpose is to be at the same speed when we reach safeD
							
							if(oldD > newD) {
								float diffV = (oldD - newD - 1)/(Const.PAS/1000.f);
								float margeT = (newD-2*safeD)/newV;
								
								int nbEtapes = (int) (margeT/Const.PAS);
								if(nbEtapes <=0) {
									nbEtapes=1;
								}
								toSlowV = Math.abs((diffV/nbEtapes)-newV);
							} 
							else {
								toSlowV = 0;
							}
						}
					}
					
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
				}
				knownCars = tmpKnownCars;				
			}
			
			/* APPLICATION OF WHAT IS PLANNED*/
			moveTo(tmpPos);
			HashMap<String, OrientedPoint> sendPos = new HashMap<String, OrientedPoint>();
			sendPos.put(this.getNetworkID(), tmpPos);
			sendMessage(Const.MY_COMMUNITY, group, Const.ENV_ROLE, new ObjectMessage<HashMap<String, OrientedPoint>>(sendPos));
			pos = tmpPos; 
			pos.setSpeed(newV);
			pause(Const.PAS);
		}
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
		ImageIcon car = new ImageIcon("ressources/voiture_verte.png");
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
					vToReach += (0.05f*position*vToReach);

				}
				else if(data[0].equals("safe")) {
					safeD = Integer.valueOf(data[1]);
					seeD = 3*safeD;
				} // est-ce encore utile ??
				else if (data[0].equals("crossing")) {
					crossCars.add(data[2].substring(0, message.getReceiver().getAgentNetworkID().length()-2));
					crossCarStatus.put(data[2].substring(0, message.getReceiver().getAgentNetworkID().length()-2), Boolean.valueOf(data[1]));
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


	public OrientedPoint getPos() {
		return pos;
	}


	public void setPos(OrientedPoint pos) {
		this.pos = pos;
	}
}
