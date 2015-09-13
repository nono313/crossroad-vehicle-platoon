package fr.utbm.ia54.agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import fr.utbm.ia54.consts.Const;
import fr.utbm.ia54.utils.Functions;
import fr.utbm.ia54.utils.OrientedPoint;
import madkit.kernel.Agent;
import madkit.kernel.Message;
import madkit.message.ObjectMessage;
import madkit.message.StringMessage;

/**
 * Train class.
 * @author Alexis Florian
 */
public class Train extends Agent {
	
	private float speed;
	private float crossingSpeed;
	private int safeD;
	private int crossingSafeD;
	
	private Car[] cars;
	private String group;
	private int numTrain;
	
	private int count; // Timer
	
	private List<OrientedPoint> soloCrossing;
	private List<OrientedPoint> notAloneCrossing;
	
	@Override
	protected void activate() {
		// initialization
		speed = 120;
		crossingSpeed = 60;
		safeD = 2*Const.CAR_SIZE;
		crossingSafeD = 4*Const.CAR_SIZE;
		
		cars = new Car[Const.NB_CAR_BY_TRAIN];
		numTrain = 0;
		
		count = -1;
		soloCrossing = new ArrayList<OrientedPoint>();
		notAloneCrossing = new ArrayList<OrientedPoint>();
		
		group = Const.SIMU_GROUP+numTrain;
		while(getAgentsWithRole( Const.MY_COMMUNITY, group, Const.TRAIN_ROLE) != null) {
			numTrain++;
			group = Const.SIMU_GROUP+numTrain;
		}

        requestRole(Const.MY_COMMUNITY, group, Const.TRAIN_ROLE);
        requestRole(Const.MY_COMMUNITY, Const.TRAIN_ROLE, Const.TRAIN_ROLE);
	}

	@Override
	protected void live() {

		/* Create cars */
		int i = 0;
		while (i < Const.NB_CAR_BY_TRAIN) {
			getNewMessages();
			if(count <= 0) {
				cars[i] = new Car();
				cars[i].setGroup(group);
				cars[i].setPosition(i);
				launchAgent(cars[i]);
				count = (35+i*5);
				i++;
			} 
			else {
				count --;
			}
			pause(Const.PAS);
		}
		
		broadcastMessage(Const.MY_COMMUNITY, group, Const.CAR_ROLE, new StringMessage("speed:"+speed));
		broadcastMessage(Const.MY_COMMUNITY, group, Const.CAR_ROLE, new StringMessage("safe:"+safeD));
		
		while(true) {
			getNewMessages();
			/*All behaviours in train are related to communication, 
			
			TODO deal with multiple train
			TODO deal with train following eachother
			*/
			pause(Const.PAS);
		}
	}

	@Override
	protected void end() {
	}

	public float getSpeed() {
		return speed;
	}

	public void setSpeed(float speed) {
		this.speed = speed;
	}

	public int getSafeD() {
		return safeD;
	}

	public void setSafeD(int safeD) {
		this.safeD = safeD;
	}
	
	/**
	 * Check for new messages and updates internal parameters
	 */
	private void getNewMessages() {
		Message m = null;
		
		// we manage all messages
		while (!isMessageBoxEmpty()){
			m = nextMessage();
			
			if(m instanceof StringMessage) {
				// Nothing to do
			} 
			else if (m instanceof ObjectMessage) {
				@SuppressWarnings("unchecked")
				Object o = ((ObjectMessage) m).getContent();
				if (o.getClass().equals(new HashMap<String,OrientedPoint>().getClass())) {
					HashMap<String,OrientedPoint> dataRetrieved = (HashMap<String, OrientedPoint>) o;
					for(String i : dataRetrieved.keySet()) {
						if(i.equals("crossing")) {
							System.out.println("our train entered a crossing");
							
							soloCrossing.add(dataRetrieved.get(i));
							
							// call other trains, if they respond they are in range (comming to the crossing)
							HashMap<String, OrientedPoint> tmp = new HashMap<String,OrientedPoint>();
							tmp.put("warningCrossing", dataRetrieved.get(i));
							ObjectMessage<HashMap<String,OrientedPoint>> msg = new ObjectMessage<HashMap<String,OrientedPoint>>(tmp);
							System.out.println(broadcastMessage(Const.MY_COMMUNITY, Const.TRAIN_ROLE, Const.TRAIN_ROLE, msg));
						} else if (i.equals("warningCrossing")) {
							//another train come into a crossing
							System.out.println("another train entered a crossing");
							
							//if we are into the crossing (or coming to it)
							//we adapt our own speed and crossing distance, and warn the other train
							if(soloCrossing.contains(dataRetrieved.get(i))) {
								System.out.println("...And we are into this crossing");
								notAloneCrossing.add(dataRetrieved.get(i));
								changeCarBehavior(new ObjectMessage<String>("speed:"+crossingSpeed));
								changeCarBehavior(new ObjectMessage<String>("safeD:"+crossingSafeD));
								
								HashMap<String, OrientedPoint> tmp = new HashMap<String,OrientedPoint>();
								tmp.put("confirmCrossing", dataRetrieved.get(i));
								ObjectMessage<HashMap<String,OrientedPoint>> msg = new ObjectMessage<HashMap<String,OrientedPoint>>(tmp);
								int idOtherTrain =(numTrain==0) ? 1:0;
								System.out.println(sendMessage(m.getSender(), msg));
							}
							
						} else if (i.equals("confirmCrossing")) {
							System.out.println("So both trains are into the crossing");
							//another train have detected collision potential, we adapt speed(safe D) and crossing distance
							if(soloCrossing.contains(dataRetrieved.get(i))) {
								notAloneCrossing.add(dataRetrieved.get(i));
							changeCarBehavior(new ObjectMessage<String>("speed:"+crossingSpeed));
							changeCarBehavior(new ObjectMessage<String>("safeD:"+crossingSafeD));
							}
						} else if (i.equals("exitCrossing")) {
							System.out.println("our train is getting out of the crossing");
							//we have left the crossing, return to normal state
							soloCrossing.remove(dataRetrieved.get(i));
							notAloneCrossing.remove(dataRetrieved.get(i));
							changeCarBehavior(new ObjectMessage<String>("speed:"+speed));
							changeCarBehavior(new ObjectMessage<String>("safeD:"+safeD));
						}
					}
				} else if (o.getClass().equals(new LinkedList<OrientedPoint>().getClass())) {
					ObjectMessage<LinkedList<OrientedPoint>> message = (ObjectMessage<LinkedList<OrientedPoint>>) m;
					prepareCrossing(message.getContent());
				}
			}
		}
	}
	
	public void prepareCrossing( LinkedList<OrientedPoint> otherCars) {
		OrientedPoint crossing = otherCars.poll();
		OrientedPoint firstCar = this.cars[0].getPos();
		LinkedList<Double> timeLeft = new LinkedList<Double>();
		Integer firstCarD = Functions.manhattan(firstCar, crossing);
		Double firstCarDelay = (firstCarD/firstCar.getSpeed());
		Double minTime = null;
		Double maxTime = null;
		Double optimalTime = null;
		Double adjustSpeed = null;
		
		for (OrientedPoint car : otherCars) {
			timeLeft.push(Functions.manhattan(car, crossing)/car.speed);
		}
		
		for (Double timer : timeLeft) {
			// if closest car in front of our train
			if(timer < firstCarDelay && (minTime == null ||timer > minTime)) {
				minTime = timer;
			// if closest car behind our train's first
			} 
			else if (timer >= firstCarDelay && (maxTime == null || maxTime > timer)) {
				maxTime = timer;
			}
		}

		if (maxTime == null) {
			adjustSpeed = firstCar.getSpeed();
		} 
		else { 
			if(minTime == null) {
				optimalTime = maxTime - 2*safeD /firstCar.getSpeed();
			} 
			else {
				optimalTime = minTime + (maxTime - minTime)/3;
			}
	
			adjustSpeed = firstCarD/optimalTime;
			adjustSpeed += (adjustSpeed -firstCar.getSpeed())/100;
			
			count = (int) (optimalTime*1000/Const.PAS);
			broadcastMessage(Const.MY_COMMUNITY, group, Const.CAR_ROLE, new StringMessage("speed:"+adjustSpeed));
		}
	}
	
	
	private void changeCarBehavior(ObjectMessage data) {
		broadcastMessage(Const.MY_COMMUNITY, group, Const.CAR_ROLE, data);
	}
}
