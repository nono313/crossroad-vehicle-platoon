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
		crossingSpeed = 120;
		safeD = 3*Const.CAR_SIZE;
		crossingSafeD = 5*Const.CAR_SIZE;
		
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
		
		i=0;
		changeCarBehavior(new StringMessage("speed:"+speed));
		changeCarBehavior(new StringMessage("safeD:"+safeD));
		changeCarBehavior(new StringMessage("crossD:"+safeD));
		
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
							System.out.println("Train" + numTrain + " entered a crossing");
							
							soloCrossing.add(dataRetrieved.get(i));
							
							// call other trains, if they respond they are in range (comming to the crossing)
							HashMap<String, OrientedPoint> tmp = new HashMap<String,OrientedPoint>();
							tmp.put("warningCrossing", dataRetrieved.get(i));
							ObjectMessage<HashMap<String,OrientedPoint>> msg = new ObjectMessage<HashMap<String,OrientedPoint>>(tmp);
							broadcastMessage(Const.MY_COMMUNITY, Const.TRAIN_ROLE, Const.TRAIN_ROLE, msg);
						} else if (i.equals("warningCrossing")) {
							//another train come into a crossing
							//System.out.println("Train" + numTrain +": another train entered a crossing");
							
							//if we are into the crossing (or coming to it)
							//we adapt our own speed and crossing distance, and warn the other train
							if(soloCrossing.contains(dataRetrieved.get(i))) {
								System.out.println("Train" + numTrain +": ...And we are into this crossing");
								notAloneCrossing.add(dataRetrieved.get(i));
								changeCarBehavior(new StringMessage("speed:"+crossingSpeed));
								changeCarBehavior(new StringMessage("crossD:"+crossingSafeD));
								
								/*HashMap<String, OrientedPoint> tmp = new HashMap<String,OrientedPoint>();
								tmp.put("confirmCrossing", dataRetrieved.get(i));
								ObjectMessage<HashMap<String,OrientedPoint>> msg = new ObjectMessage<HashMap<String,OrientedPoint>>(tmp);
								sendMessage(m.getSender(), msg);*/
								
								LinkedList<OrientedPoint> carsList = new LinkedList<OrientedPoint>();
								carsList.add(dataRetrieved.get(i));
								for(Car j : cars) {
									carsList.add(j.getPos());
								}
								ObjectMessage<LinkedList<OrientedPoint>> msg = new ObjectMessage<LinkedList<OrientedPoint>>(carsList);
								sendMessage(m.getSender(), msg);
							}
							
						} else if (i.equals("confirmCrossing")) {
							System.out.println("Train" + numTrain +": So both trains are into the crossing");
							//another train have detected collision potential, we adapt speed(safe D) and crossing distance
							if(soloCrossing.contains(dataRetrieved.get(i))) {
								notAloneCrossing.add(dataRetrieved.get(i));
							changeCarBehavior(new StringMessage("speed:"+crossingSpeed));
							changeCarBehavior(new StringMessage("crossD:"+crossingSafeD));
							}
						} else if (i.equals("enteringCrossing")) {
							//System.out.println("Train" + numTrain + " is getting into the crossing");
							//force safe speed
							if(notAloneCrossing.contains(dataRetrieved.get(i))) {
								changeCarBehavior(new StringMessage("speed:"+crossingSpeed));
							}
						}
							
						else if (i.equals("exitCrossing")) {
							System.out.println("Train" + numTrain + " is getting out of the crossing");
							//we have left the crossing, return to normal state
							soloCrossing.remove(dataRetrieved.get(i));
							if(notAloneCrossing.contains(dataRetrieved.get(i))) {
								notAloneCrossing.remove(dataRetrieved.get(i));
								changeCarBehavior(new StringMessage("speed:"+speed));
								changeCarBehavior(new StringMessage("crossD:"+safeD));
								
								HashMap<String, OrientedPoint> tmp = new HashMap<String,OrientedPoint>();
								tmp.put("warningExitCrossing", dataRetrieved.get(i));
								ObjectMessage<HashMap<String,OrientedPoint>> msg = new ObjectMessage<HashMap<String,OrientedPoint>>(tmp);
								broadcastMessage(Const.MY_COMMUNITY, Const.TRAIN_ROLE, Const.TRAIN_ROLE, msg);
							}
						}
						else if (i.equals("warningExitCrossing")) {
							System.out.println("Train" + numTrain + " is finally alone in the crossing");
							//we have left the crossing, return to normal state
							notAloneCrossing.remove(dataRetrieved.get(i));
							changeCarBehavior(new StringMessage("speed:"+speed));
							changeCarBehavior(new StringMessage("crossD:"+safeD));
							//changeCarBehavior(new StringMessage("safeD:"+safeD));
						}
					}
				} else if (o.getClass().equals(new LinkedList<OrientedPoint>().getClass())) {
					ObjectMessage<LinkedList<OrientedPoint>> message = (ObjectMessage<LinkedList<OrientedPoint>>) m;
					LinkedList<OrientedPoint> otherCars = message.getContent();
					prepareCrossing(otherCars);

					if(soloCrossing.contains(otherCars.peek())) {
						notAloneCrossing.add(otherCars.peek());
					}
					System.out.println("Train" + numTrain +": So both trains are into the crossing");
					changeCarBehavior(new StringMessage("crossD:"+crossingSafeD));
				}
			}
		}
	}
	
	public void prepareCrossing( LinkedList<OrientedPoint> otherCars) {
		OrientedPoint crossing = otherCars.poll();
		OrientedPoint firstCar = this.cars[0].getPos();
		LinkedList<Double> timeLeft = new LinkedList<Double>();
		Double minTime = null;
		Double maxTime = null;
		Double optimalTime = null;
		Double adjustSpeed = null;
		Double firstCarD = (double) Functions.manhattan(firstCar, crossing);
		Double firstCarDelay = (firstCarD/crossingSpeed);
		
		for (OrientedPoint car : otherCars) {
			timeLeft.push((double) (Functions.manhattan(car, crossing)/crossingSpeed));
		}
		
		for (Double timer : timeLeft) {
			// if closest car to be in front of our train
			if(timer < firstCarDelay && (minTime == null ||timer > minTime)) {
				minTime = timer;
			// if closest car to go behind our train's first
			} 
			else if (timer >= firstCarDelay && (maxTime == null || maxTime > timer)) {
				maxTime = timer;
			}
		}

		if (maxTime == null) {
			adjustSpeed = (double) crossingSpeed;
		} 
		else { 
			if(minTime == null) {
				optimalTime = maxTime - 2*(float)safeD /crossingSpeed;
				adjustSpeed = (float)(firstCarD + Const.CAR_SIZE)/optimalTime;
			} 
			else {
				optimalTime = minTime + (float)safeD /crossingSpeed;
				adjustSpeed = (float)(firstCarD - Const.CAR_SIZE)/optimalTime;
			}
			changeCarBehavior(new StringMessage("speed:"+adjustSpeed));
		}
	}
	
	
	private void changeCarBehavior(StringMessage data) {
		ReturnCode echo = broadcastMessage(Const.MY_COMMUNITY, group, Const.CAR_ROLE, data);
		if(!echo.toString().equals("OK"))
			System.out.println(echo);
	}
}
