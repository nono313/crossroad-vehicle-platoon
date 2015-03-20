package fr.utbm.ia54.agents;

import java.util.LinkedList;

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
	private int safeD;
	private Car[] cars;
	private String group;
	private int count; // Timer
	
	/**
	 * This is the first activated behavior in the life cycle of a MaDKit agent.
	 */
	@Override
	protected void activate() {
		// initialization
		speed = 130;
		safeD = Const.CAR_SIZE;
		cars = new Car[Const.NB_CAR_BY_TRAIN];
		int numTrain = 0;
		count = -1;
		group = Const.SIMU_GROUP+numTrain;
		
		while(getAgentsWithRole( Const.MY_COMMUNITY, group, Const.TRAIN_ROLE) != null) {
			numTrain++;
			group = Const.SIMU_GROUP+numTrain;
		}
		
        requestRole(Const.MY_COMMUNITY, group, Const.TRAIN_ROLE);
	}

	/**
	 * This is the second behavior which is activated, i.e. when activate ends.
	 * It actually implements the life of the agent.
	 * It is usually a while true loop.
	 * Here the agent lives 10 seconds and quits.
	 * 
	 */
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
			if(count > 0) {
				// Decrease the timer
				count--;
			} 
			else if(count == 0) {
				// If timeout, send default simulation parameters
				count = -1;
				broadcastMessage(Const.MY_COMMUNITY, group, Const.CAR_ROLE, new StringMessage("speed:"+speed));
			}
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
		// Nothing to do
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
				ObjectMessage<LinkedList<OrientedPoint>> message = (ObjectMessage<LinkedList<OrientedPoint>>) m;
				prepareCrossing(message.getContent());
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
}
