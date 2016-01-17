package fr.utbm.ia54.agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import fr.utbm.ia54.consts.Const;
import fr.utbm.ia54.utils.Functions;
import fr.utbm.ia54.utils.OrientedPoint;
import fr.utbm.ia54.utils.SpeedPolynom;
import madkit.kernel.Agent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import madkit.message.ObjectMessage;
import madkit.message.StringMessage;

/**
 * Train class.
 * 
 * Train agent, virtual, not represented on the UI.
 * @author Nathan Olff
 */
public class Train extends Agent {
	/* Identity attributes */
	private Car[] cars;
	private String group;
	private int numTrain;
	
	/* Speed */
	private float speed;
	private float crossingSpeed;
	
	/* Inter-distance */
	private int safeD;
	private int crossingSafeD;
	
	private int count; // Timer
	
	/* Lists of crossings in which the train is */
	private List<OrientedPoint> soloCrossing;
	private List<OrientedPoint> notAloneCrossing;
	
	/* Logging */
	private Level defaultLogLevel = Level.WARNING;
	private Level moreLog = Level.FINEST;
	
	/**
	 * This is the first activated behavior in the life cycle of a MaDKit agent.
	 */
	@Override
	protected void activate() {
		setLogLevel(this.defaultLogLevel);

		/* Initialize attributes with default values */
		this.speed = 80;
		this.crossingSpeed = 80;
		this.safeD = 6*Const.CAR_SIZE;
		this.crossingSafeD = 6*Const.CAR_SIZE;
		this.cars = new Car[Const.NB_CAR_BY_TRAIN];
		this.numTrain = 0;
		
		this.count = -1;
		
		/* Initialize lists */
		this.soloCrossing = new ArrayList<>();
		this.notAloneCrossing = new ArrayList<>();
		
		this.group = Const.SIMU_GROUP+this.numTrain;
		while(getAgentsWithRole( Const.MY_COMMUNITY, this.group, Const.TRAIN_ROLE) != null) {
			this.numTrain++;
			this.group = Const.SIMU_GROUP+this.numTrain;
		}

        requestRole(Const.MY_COMMUNITY, this.group, Const.TRAIN_ROLE);
        requestRole(Const.MY_COMMUNITY, Const.TRAIN_ROLE, Const.TRAIN_ROLE);
	}

	/**
	 * This is the second behavior which is activated, i.e. when activate ends.
	 * It actually implements the life of the agent. It is usually a while true
	 * loop.
	 */
	@Override
	protected void live() {
		/* Create cars and launch them sequentially */
		int i = 0;
		while (i < Const.NB_CAR_BY_TRAIN) {
			getNewMessages();
			if(this.count <= 0) {
				this.cars[i] = new Car();
				if(i == 0) {
					this.cars[i].setLeader(true);
				}
				if(i == Const.NB_CAR_BY_TRAIN-1) {
					this.cars[i].setTail(true);
				}
				this.cars[i].setGroup(this.group);
				this.cars[i].setPosition(i);
				launchAgent(this.cars[i]);
				this.count = (35+i*5);
				
				i++;
			} 
			else {
				this.count --;
			}
			pause((int) (Const.PAS*Const.debugAccelerator));
		}
		
		i=0;
		changeCarBehavior(new StringMessage("speed:"+this.speed));
		changeCarBehavior(new StringMessage("safeD:"+this.safeD));
		//changeCarBehavior(new StringMessage("crossD:"+safeD));

		while(isAlive()) {
			getNewMessages();
			/* All behaviours in train are related to communication	*/
			pause((int) (Const.PAS*Const.debugAccelerator));
		}
	}

	public float getSpeed() {
		return this.speed;
	}

	public void setSpeed(float speed) {
		this.speed = speed;
	}

	public int getSafeD() {
		return this.safeD;
	}

	public void setSafeD(int safeD) {
		this.safeD = safeD;
	}
	
	/**
	 * Check for new messages and updates internal parameters
	 */
	private void getNewMessages() {
		Message m = null;
		
		/* We read all messages */
		while (!isMessageBoxEmpty()){
			setLogLevel(this.moreLog);
			m = nextMessage();
			setLogLevel(this.defaultLogLevel);
			
			if(m instanceof StringMessage) {
				/* 
				 * Nothing to do
				 * A train should not receive SringMessage.
				 */
			} 
			else if (m instanceof ObjectMessage) {
				Object o = ((ObjectMessage<?>) m).getContent();
				
				if (o.getClass().equals(new HashMap<String,OrientedPoint>().getClass())) {
					/* If the object sent is a Map, we know it has Strings as keys */
					@SuppressWarnings("unchecked")
					HashMap<String,OrientedPoint> dataRetrieved = (HashMap<String, OrientedPoint>) o;

					for(String i : dataRetrieved.keySet()) {

						if(i.equals("crossing")) {
							
							this.logger.info("Train" + this.numTrain + " entered a crossing");
							this.soloCrossing.add(dataRetrieved.get(i));
							
							/* 
							 * Send warning to all other trains.
							 * If one of them is in the same crossing, it will send its vehicles coordinates.
							 */
							HashMap<String, OrientedPoint> tmp = new HashMap<>();
							tmp.put("warningCrossing", dataRetrieved.get(i));
							ObjectMessage<HashMap<String,OrientedPoint>> msg = new ObjectMessage<>(tmp);
							broadcastMessage(Const.MY_COMMUNITY, Const.TRAIN_ROLE, Const.TRAIN_ROLE, msg);
							
							/* Also send message to the cars */
							HashMap<String, OrientedPoint> tmp2 = new HashMap<>();
							tmp2.put("crossing", dataRetrieved.get(i));
							ObjectMessage<HashMap<String,OrientedPoint>> msgToCars = new ObjectMessage<>(tmp2);
							broadcastToCars(msgToCars);
							
							changeCarBehavior(new StringMessage("safeD:"+this.crossingSafeD));
							changeCarBehavior(new StringMessage("speed:"+this.crossingSpeed));
							
						} else if (i.equals("warningCrossing")) {
							/* Another train come into a crossing */
														
							/* 
							 * If we are into that crossing (or coming to it)
							 * we respond by sending our vehicles coordinates and speeds
							 */
							if(this.soloCrossing.contains(dataRetrieved.get(i))) {
								this.logger.info("Train" + this.numTrain +": ...And we are into this crossing");

								this.notAloneCrossing.add(dataRetrieved.get(i));
								changeCarBehavior(new StringMessage("speed:"+this.crossingSpeed));
								
								/* Create an ObjectMessage with a LinkedList filled with coordinates */
								LinkedList<OrientedPoint> carsList = new LinkedList<>();
								carsList.add(dataRetrieved.get(i));
								for(Car j : this.cars) {
									carsList.add(j.getPos());
								}
								ObjectMessage<LinkedList<OrientedPoint>> msg = new ObjectMessage<>(carsList);
								sendMessage(m.getSender(), msg);
							}
						}
						else if (i.equals("exitCrossing")) {
							this.logger.info("Train" + this.numTrain + " is getting out of the crossing");
							/* 
							 * The queue told the train that is has left the crossing
							 * and should return to normal state
							 */
							this.soloCrossing.remove(dataRetrieved.get(i));
							if(this.notAloneCrossing.contains(dataRetrieved.get(i))) {
								/* 
								 * If another train was in that crossing, we send our 
								 * initial speed order to our trains 
								 */
								this.notAloneCrossing.remove(dataRetrieved.get(i));
								changeCarBehavior(new StringMessage("speed:"+this.speed));
								
								HashMap<String, OrientedPoint> tmp = new HashMap<>();
								tmp.put("warningExitCrossing", dataRetrieved.get(i));
								ObjectMessage<HashMap<String,OrientedPoint>> msg = new ObjectMessage<>(tmp);
								broadcastMessage(Const.MY_COMMUNITY, Const.TRAIN_ROLE, Const.TRAIN_ROLE, msg);
							}
							
							/* We also tell the cars that their train has left the crossing */
							HashMap<String, OrientedPoint> tmp2 = new HashMap<>();
							tmp2.put("exitCrossing", dataRetrieved.get(i));
							ObjectMessage<HashMap<String,OrientedPoint>> msgToCars = new ObjectMessage<>(tmp2);
							broadcastToCars(msgToCars);
							if(this.soloCrossing.isEmpty() && this.notAloneCrossing.isEmpty()) {
								changeCarBehavior(new StringMessage("speed:"+this.speed));
								changeCarBehavior(new StringMessage("safeD:"+this.safeD));
							}
							
						}
						else if (i.equals("warningExitCrossing")) {
							this.logger.info("Train" + this.numTrain + " is finally alone in the crossing");
							/* We have left the crossing, return to normal state */
							this.notAloneCrossing.remove(dataRetrieved.get(i));
						}
					}
				} else if (o.getClass().equals(new LinkedList<OrientedPoint>().getClass())) {
					/* 
					 * If a train sent us their vehicles coordinates, then we are 
					 * arriving to the same crossing as it.
					 */
					@SuppressWarnings("unchecked")
					ObjectMessage<LinkedList<OrientedPoint>> message = (ObjectMessage<LinkedList<OrientedPoint>>) m;
					LinkedList<OrientedPoint> otherCars = message.getContent();
					prepareCrossing(otherCars);	// Decide of a crossing strategy

					if(this.soloCrossing.contains(otherCars.peek())) {
						this.notAloneCrossing.add(otherCars.peek());
					}
					this.logger.info("Train" + this.numTrain +": So both trains are into the crossing");
				}
			}
		}
	}
	
	/**
	 * Override function to get some log data
	 */
	@Override
	public ReturnCode sendMessage(final String community, final String group1, final String role, final Message message) {
		//setLogLevel(this.moreLog);
		ReturnCode ret = super.sendMessage(community,  group1, role, message);
		setLogLevel(this.defaultLogLevel);
		return ret;
	}
	
	@Override
	public ReturnCode sendMessage(final AgentAddress receiver, final Message messageToSend){
		//setLogLevel(this.moreLog);
		ReturnCode ret = super.sendMessage(receiver, messageToSend);
		setLogLevel(this.defaultLogLevel);
		return ret;
	}
	
	/**
	 * Define a crossing strategy
	 * @param otherCars : list of coordinates with the other trains' vehicles positions
	 */
	private void prepareCrossing( LinkedList<OrientedPoint> otherCars) {
		setLogLevel(this.moreLog);
		
		/* The first element of the list if the crossing's coordinates */
		OrientedPoint crossing = otherCars.poll();

		OrientedPoint myFirstCar = this.cars[0].getPos();
		
		LinkedList<Double> timeLeft = new LinkedList<>();
		
		Double minTime = null;
		Double maxTime = null;
		Double optimalTime = null;
		Double adjustSpeed = null;
		double averageInterdistance = 0;
		
		OrientedPoint firstOtherCar = otherCars.getFirst();
		double otherTrainSpeed = firstOtherCar.getSpeed();
		
		this.logger.fine("First other car = " + firstOtherCar);

		double myFirstCarSpeed = myFirstCar.getSpeed();
		double myFirstCarDistance = Functions.manhattan(myFirstCar, crossing);
		double myFirstCarDelayToCrossing;
		myFirstCarDelayToCrossing = (myFirstCarDistance/myFirstCarSpeed);
		timeLeft.clear();
		this.logger.info("firstCarD = "+myFirstCarDistance + " ; firstCarDelay = " + myFirstCarDelayToCrossing);
		OrientedPoint lastCar = null;
		this.logger.info("Speed of the first elements = " + otherCars.getFirst().getSpeed());
		for (OrientedPoint car : otherCars) {
			timeLeft.push(Functions.manhattan(car, crossing)/otherTrainSpeed);
			if(lastCar != null) {
				double d = Functions.manhattan(car, lastCar);
				this.logger.finer("d = "+d);
				averageInterdistance += d;
			}
				
			lastCar = (OrientedPoint) car.clone();
		}
		/* 
		 * We compute the average interdistance to simulate the presence of a car in front 
		 * of the leader or after the queue.
		 */
		averageInterdistance /= (Const.NB_CAR_BY_TRAIN-1);
		
		/* 
		 * Figure out between which of the other train cars, the first car 
		 * of the current train will have reached the crossing 
		 */
		for (Double timer : timeLeft) {
			/* Iif closest car to be in front of our train */
			if(timer < myFirstCarDelayToCrossing && (minTime == null ||timer > minTime)) {
				minTime = timer;
			}
			/* If closest car to go behind our train's first */
			else if (timer >= myFirstCarDelayToCrossing && (maxTime == null || maxTime > timer)) {
				maxTime = timer;
			}
			this.logger.info("time = " + timer + " ; minTime = " + minTime + " ; maxTime = " + maxTime);
		}

		this.logger.info("minTime = " + minTime + " ; maxTime = " + maxTime);
		
		/* If the other train will have left the crossing when I arrive */
		if(maxTime == null) {
			maxTime = minTime + averageInterdistance/otherTrainSpeed;
			this.logger.info("maxTime was null : now + "+maxTime);
		}
		if(minTime == null) {
			minTime = maxTime - averageInterdistance/otherTrainSpeed;
			this.logger.info("minTime was null : now + "+minTime);
		} 
			
		/* Slightly edit min and max to take into account the time for each to enter and leave the crossing */
		minTime += Const.CAR_SIZE/otherTrainSpeed;
		maxTime -= Const.CAR_SIZE/otherTrainSpeed;
		
		optimalTime = (minTime + maxTime)/2.;
		
		if(maxTime-minTime < safeD/otherTrainSpeed) {
			logger.warning("maxTime ("+maxTime+") and minTime ("+minTime+") are too close to allow safe passage !!! (safeD/crossingSpeed = "+safeD/crossingSpeed +")");
		}
			
		/* 
		 * Adjust speed to get the average speed necessary 
		 * to reach the crossing at the right time. 
		 */
		adjustSpeed = myFirstCarDistance/optimalTime;			
		
		/* Setup polynom computation */
		SpeedPolynom speedPol; 
		if(Math.abs(adjustSpeed-otherTrainSpeed) < 5) {
			/* 
			 * If adjustSpeed and otherTrainSpeed are close, we just use the 
			 * average speed (adjustSpeed) and then change directly at the crossing.
			 */
			speedPol = new SpeedPolynom();
			speedPol.setCoeff(0, adjustSpeed);
			this.logger.info("Both trains have similar speeds : no need to use polynom");
		}
		else {
			speedPol = new SpeedPolynom();
			speedPol.interpolation(myFirstCarSpeed, otherTrainSpeed, myFirstCarDistance, optimalTime);
		}
		
		/* Forward the speed polynom and the final speed to its vehicles */
		HashMap<String, SpeedPolynom> tmp2 = new HashMap<>();
		tmp2.put("speedPolynom", speedPol);
		ObjectMessage<HashMap<String,SpeedPolynom>> msgToCars = new ObjectMessage<>(tmp2);
		broadcastToCars(msgToCars);
		
		changeCarBehavior(new StringMessage("speed:"+otherTrainSpeed));
		this.logger.info("address : " + this.cars[0].getAgentAddressIn(Const.MY_COMMUNITY, this.group, Const.CAR_ROLE));
		
		myFirstCarSpeed = adjustSpeed;
		setLogLevel(this.defaultLogLevel);
	}
	
	/**
	 * Send an order to all its cars 
	 * @param data : StringMessage to broadcast to its cars
	 */
	private void changeCarBehavior(StringMessage data) {
		ReturnCode echo = broadcastMessage(Const.MY_COMMUNITY, this.group, Const.CAR_ROLE, data);
		if(!echo.toString().equals("OK"))
			System.out.println(echo);
	}
	
	/**
	 * Broadcast any message to its cars
	 * @param message to send
	 */
	private ReturnCode broadcastToCars(Message m) {
		return broadcastMessage(Const.MY_COMMUNITY, this.group, Const.CAR_ROLE, m);
	}
}
