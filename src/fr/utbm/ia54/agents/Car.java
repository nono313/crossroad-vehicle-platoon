package fr.utbm.ia54.agents;

import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import fr.utbm.ia54.consts.Const;
import fr.utbm.ia54.main.MainProgram;
import fr.utbm.ia54.path.CarPath;
import fr.utbm.ia54.utils.Functions;
import fr.utbm.ia54.utils.OrientedPoint;
import fr.utbm.ia54.utils.RotateLabel;
import fr.utbm.ia54.utils.SpeedPolynom;
import madkit.kernel.Agent;
import madkit.kernel.Message;
import madkit.message.ObjectMessage;
import madkit.message.StringMessage;

/**
 * Car class.
 * 
 * Car agent, represented by a square on the UI
 * @author Nathan Olff
 *
 */
public class Car extends Agent {

	/* Identity attributes */
	private OrientedPoint pos;
	private OrientedPoint tmpPos;
	private String group;
	private int position;
	private int numTrain;
	private boolean leader = false;
	private boolean tail = false;
	private int carColor;
	//private RotateLabel icone;
	private JLabel icone;

	/* Speed attributes */
	private double speedToReach;
	private double newSpeed = 0;
	private double maxSpeed = 300;
	private double normalSpeed = 80;
	private SpeedPolynom speedPolynom;
	private long roundSinceSetPolynom = 0;
	
	/* Distance attributes */
	private double safeD;
	private double crossingD;
	private double seeD;
	private double beaconRange;
	private double distanceToGoThrough;


	/* Path on which the car is moving */
	private CarPath carPath;	
	
	private HashMap<String, Boolean> crossCarStatus;

	/* Logging attributes */
	private boolean printingTurn = false;
	private String printings = new String();
	private Level defaultLogLevel = Level.INFO;
	private Level moreLog = Level.INFO;

	/* Movement attributes for followers */
	private double a, D;
	private double error;
	private double lastError = 0;
	private double integral = 0;
	private double derivative = 0;
	private double Kp;
	private double Ki;
	private double Kd;

	/* Set of crossing in which the car's train currently is */
	private Set<OrientedPoint> inCrossing;

	/**
	 * This is the first activated behavior in the life cycle of a MaDKit agent.
	 */
	@Override
	protected void activate() {
		setLogLevel(this.defaultLogLevel);
		
		/* Retrieve data from the Environnement and the train */
		this.carPath = MainProgram.getCarPath();
		this.numTrain = getNumTrain(this.group);
		this.pos = this.carPath.getStart(this.numTrain);
		this.beaconRange = MainProgram.getEnv().getBeaconRange();

		/* Request roles in the different organisation for communication purposes */
		requestRole(Const.MY_COMMUNITY, Const.CAR_ROLE, Const.CAR_ROLE);
		requestRole(Const.MY_COMMUNITY, this.group, Const.CAR_ROLE);
		requestRole(Const.MY_COMMUNITY, this.group, this.getNetworkID());

		setupCarIcone();

		/* Initialize attributes with default values*/
		
		this.pos.setSpeed(this.normalSpeed);
		this.speedToReach = this.normalSpeed;

		this.safeD = 6 * Const.CAR_SIZE;
		this.seeD = 3 * this.safeD;
		this.crossingD = this.safeD;

		this.crossCarStatus = new HashMap<>();
		this.inCrossing = new HashSet<>();

		/* Set parameters for following functions */
		
		this.a = 0.5;
		this.D = 2 * Const.CAR_SIZE;
		
		this.Kp = 0;
		this.Ki = 0;
		this.Kp = 0.8;

	}

	/**
	 * This is the second behavior which is activated, i.e. when activate ends.
	 * It actually implements the life of the agent. It is usually a while true
	 * loop.
	 */
	@Override
	protected void live() {
		this.logger.info(this.getClass().getSimpleName()+" is alive.");
		
		while (isAlive()) {
			this.newSpeed = 0;
			this.distanceToGoThrough = 0;

			/*
			if(this.pos.getSpeed() == 0) {
				this.logger.warning("I am not moving.");
			}
			*/

			setLogLevel(this.moreLog);
			getNewMessages();	// Process all new messages
			setLogLevel(this.defaultLogLevel);
			
			
			this.distanceToGoThrough = this.newSpeed * (Const.PAS / 1000.);
			this.tmpPos = this.carPath.getNextPoint(this.pos, this.distanceToGoThrough, this.numTrain);
			
			this.printings += toString();
			this.printings += "Actual speed : " + this.pos.speed + "\n";
			this.printings += "Optimal situation\nnewV = " + this.newSpeed + "\n";

			if (isLeader()) {
				/* Leader's behavior */
				
				if (this.speedPolynom != null) {
					/* 
					 * If a speedPolynom is defined, computer the value of the function for the current time
					 */
					this.newSpeed = this.speedPolynom.getValue(this.roundSinceSetPolynom * Const.PAS / 1000.);
					this.roundSinceSetPolynom++;
					this.logger.fine(this.newSpeed + " / " + this.roundSinceSetPolynom);
				} else {
					
					this.newSpeed = this.speedToReach;

					/* Log change of speed */
					if (this.pos.getSpeed() != this.newSpeed) {
						setLogLevel(this.moreLog);
						this.logger.fine("new speed = " + this.newSpeed);
						setLogLevel(this.defaultLogLevel);
					}
				}
			} else {
				/* Followers behavior */
				
				double d = distance();	// Get distance from the car before me
				
				this.logger.fine("distance = " + d);

				/* PID controler */
				if (d < Float.MAX_VALUE) {
					/* Compute error and use the PID controler to define a new speed */
					
					this.error = this.safeD - d;
					
					this.integral += this.error * (Const.PAS / 1000.);		// Integrale part
					this.derivative = (this.error - this.lastError) / (Const.PAS / 1000.);	// Derivative part
					
					double out = this.Kp * this.error + this.Ki * this.integral + this.Kd * this.derivative;	// PID function	
					
					this.newSpeed = this.normalSpeed * (1 - out / 100.);
					this.lastError = this.error;
					
					/* Logging data */
					this.logger.fine(this.pos.toString());
					this.logger.fine("safeD = " + this.safeD);
					this.logger.fine("error = " + this.error);
					this.logger.fine("out = " + out);

				} else {
					/* If the car in front of me is out of reach, we go the the maximum speed */
					this.newSpeed = this.maxSpeed;
					this.lastError = this.seeD;
				}

				/* 
				 * To two points politic
				 * 
				 * This is an alternative method to the PID (more simple) for trying
				 * to respect a distance between each car and the previous one.
				 */
				/*
				 * if(this.inCrossing == null) { if(d < Float.MAX_VALUE) {
				 * 
				 * double percentage;
				 * 
				 * double currentPercentage = pos.getSpeed()*100./this.maxSpeed;
				 * percentage = Math.max(2.5*(d-this.safeD),
				 * Math.min(this.a*(d-this.D), currentPercentage)); //percentage
				 * = Math.min(Math.max(2.5*(d-this.safeD),
				 * Math.min(Math.max(this.a*(d-this.D), 0), currentPercentage)),
				 * 50);
				 * 
				 * this.newV = percentage/100.*this.maxSpeed;
				 * 
				 * //logger.fine("Respect safeD");
				 * 
				 * } else { this.newV = this.maxSpeed; } } else {
				 * 
				 * if(d < Float.MAX_VALUE) { double percentage; double
				 * currentPercentage = this.pos.getSpeed()*100./this.maxSpeed;
				 * percentage = Math.min(Math.max(2.5*(d-this.crossingD),
				 * Math.min(Math.max(this.a*(d-this.D), 0), currentPercentage)),
				 * 50); this.newV = percentage/100f*this.maxSpeed;
				 * //logger.fine("Respect crossingD"); } else { this.newV =
				 * this.normalSpeed; } } 
				 */

			}

			/* Log any change of speed */
			if (this.newSpeed != this.pos.getSpeed()) {
				this.logger.fine("newV = " + this.newSpeed);

			}
			
			setLogLevel(this.defaultLogLevel);
			
			/* Execute movement and sleep for the step time*/
			executingRun(this.newSpeed);
			pause((int) (Const.PAS * Const.debugAccelerator));
		}
	}

	/**
	 * This behavior is called when the agent has finished its live behavior.
	 * Because there is no other agent, MaDKit quits when the agent is
	 * terminated.
	 */
	@Override
	protected void end() {
		leaveRole(Const.MY_COMMUNITY, this.group, Const.CAR_ROLE);
		leaveRole(Const.MY_COMMUNITY, Const.CAR_ROLE, Const.CAR_ROLE);
		leaveRole(Const.MY_COMMUNITY, this.group, this.getNetworkID());
		
		MainProgram.getMainFrame().getSuperposition().remove(this.icone);
		MainProgram.getMainFrame().getSuperposition().repaint();
	}

	/**
	 * Generates the icone for displaying the car on the UI
	 */
	private void setupCarIcone() {
		/* Car representation */
		this.carColor = (int) (Math.random() * Const.CAR_COLOR.length);
		ImageIcon car = new ImageIcon(Const.RESOURCES_DIR + "/" + Const.CAR_COLOR[this.carColor]); 
		this.icone = new RotateLabel(car);
		this.icone = new JLabel("" + getNetworkID().substring(0, getNetworkID().indexOf('@')) + "", 
				SwingConstants.CENTER);
		this.icone.setFont(new Font(this.icone.getName(), Font.PLAIN, 14));
		this.icone.setForeground(Color.WHITE);
		this.icone.setBounds(0, 0, Const.CAR_SIZE, Const.CAR_SIZE);
		this.icone.setLocation((int) this.pos.x, (int) this.pos.y);
		this.icone.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));

		// icone.setAngle(pos.getAngle());

		/* Add the car to the frame */
		MainProgram.getMainFrame().getSuperposition().add(this.icone, 1, 0);
	}

	/**
	 * Update coordinates and orientation of the car visually
	 * 
	 * @param tmpPos1
	 */
	private void moveTo(OrientedPoint tmpPos1) {
		this.icone.setLocation((int) tmpPos1.x, (int) tmpPos1.y);
		this.icone.setText(getNetworkID().substring(0, getNetworkID().indexOf('@')));
		this.icone.repaint();
		Toolkit.getDefaultToolkit().sync();
		// icone.setAngle(tmpPos.orientation);
	}

	/**
	 * Check for new messages and updates internal parameters
	 */
	private void getNewMessages() {
		Message m = null;

		/* Read all messages */
		while (!isMessageBoxEmpty()) {
			setLogLevel(Level.FINEST);
			m = nextMessage();
			setLogLevel(this.defaultLogLevel);

			if (m instanceof StringMessage) {
				StringMessage message = (StringMessage) m;
				
				this.printings += "--> " + message.getSender().getSimpleAgentNetworkID() + 
						" -> " + message.getReceiver().getSimpleAgentNetworkID() + 
						" : " + message.getContent() + "\n";
				
				/* 
				 * Split string message content
				 * The first element is the message type.
				 * The second is either non existent or a value corresponding to the order 
				 * sent by the train (e.g : "speed:80.0")
				 */ 
				String[] data = message.getContent().split(":"); 

				if (data[0].equals("speed")) { 
					/* Receiving a speed order from the train */
					@SuppressWarnings("boxing")
					double valueFromMessage = Double.valueOf(data[1]);
					this.speedToReach = valueFromMessage;

					this.logger.fine("speedToReach = " + this.speedToReach); 
					this.printings += " objective speed is now " + this.speedToReach + "\n";  

				} else if (data[0].equals("safeD")) { 
					/* Receiving a new value for safeD from the train (not used in the project at the moment) */
					@SuppressWarnings("boxing")
					double valueFromMessage = Double.valueOf(data[1]);
					this.safeD = valueFromMessage;
					this.seeD = 3 * this.safeD;

					this.printings += " Safe distance is now " + this.safeD + "\n";  

				} else if (data[0].equals("crossD")) { 
					/* Receiving a new value for crossD (not used in the projet at the moment) */
					@SuppressWarnings("boxing")
					double valueFromMessage = Double.valueOf(data[1]);
					this.crossingD = valueFromMessage;
					
					this.printings += " following distance is now " + this.crossingD + "\n";
					
				} else if (data[0].equals("printPriority")) { 
					/* User asked to see the priority in the console */
					String priorities = new String(
							"Priorities of " + this.getName() + " (" + this.crossCarStatus.size() + ")");   
					this.logger.info(priorities + this.crossCarStatus);
				} else if (data[0].equals("Print")) { 
					/* User asked to print main attributes to the console */
					this.printingTurn = true;
				}
			} else if (m instanceof ObjectMessage) {
				Object o = ((ObjectMessage<?>) m).getContent();
				
				setLogLevel(this.moreLog);
		
				if (o.getClass().equals(new HashMap().getClass())) {
					
					/* If the object sent is a Map, we know it has Strings as keys */
					@SuppressWarnings("unchecked")
					HashMap<String, Object> dataRetrieved = (HashMap<String, Object>) o;
					
					for (String i : dataRetrieved.keySet()) {
						
						if (i.equals("crossing")) {
							/* The train entered a crossing */
							OrientedPoint orientedPoint = (OrientedPoint) dataRetrieved.get(i);
							this.inCrossing.add(orientedPoint);
							this.logger.fine("Is in crossing " + this.inCrossing);
							
						} else if (i.equals("exitCrossing") && !this.inCrossing.isEmpty()) { 
							OrientedPoint orientedPoint = (OrientedPoint) dataRetrieved.get(i);
							/* The train leaved a crossing */
							if (this.inCrossing.contains(orientedPoint)) {
								this.logger.fine("Out of crossing " + this.inCrossing); 
								this.inCrossing.remove(orientedPoint);
							} else {
								this.logger.fine("Out of a crossing but still in another !"); 
							}
						} else if (i.equals("speedPolynom")) {
							/* A speed polynom has been calculated by the train */
							this.speedPolynom = (SpeedPolynom) dataRetrieved.get(i);
						}
					}
				}
				setLogLevel(this.defaultLogLevel);
			}
			// logger.info(printings);
		}
	}

	public void setGroup(String group2) {
		this.group = group2;
	}

	public void setPosition(int i) {
		this.position = i;

	}

	/**
	 * Exract the number of the train from the group name
	 * 
	 * @param group
	 * @return the number of the train
	 */
	private static int getNumTrain(String group) {
		char c = group.charAt(group.length() - 1);
		return Character.getNumericValue(c);
	}

	/**
	 * Get all vehicles from my train that are within the car's range.
	 * 
	 * This is used for simulating the car's sensors.
	 * 
	 * @param tmpPos1 : position from which the car wants to know what is around itself
	 * @param range : maximum distance between the car and the other cars it can detect
	 * @param population : set of cars in which I want to look for vehicules 
	 * 	(if empty look for all cars) 
	 * @return all cars within range
	 */
	private HashMap<String, OrientedPoint> inRangeWithinMyTrain(OrientedPoint tmpPos1, double range,
			HashMap<String, OrientedPoint> population) {
		
		HashMap<String, OrientedPoint> exclus = new HashMap<>();
		HashMap<String, OrientedPoint> neighbours = new HashMap<>();
		HashMap<String, OrientedPoint> neigh2 = new HashMap<>();

		exclus.put(this.getNetworkID(), this.pos);

		if (population == null) {
			neighbours = MainProgram.getEnv().inRangeWithinMyTrain(tmpPos1, range, this.getNetworkID(), exclus);
		} else {
			neighbours = MainProgram.getEnv().inRangeWithinMyTrain(tmpPos1, range, this.getNetworkID(), population,
					exclus);
		}

		// If there are cars nearby we filter cars behind
		if (neighbours != null && !neighbours.isEmpty()) {
			for (String ad : neighbours.keySet()) {
				if (!Functions.isBehind(neighbours.get(ad), tmpPos1)) {
					neigh2.put(ad, neighbours.get(ad));
				}
			}
		}
		return neigh2;
	}

	/**
	 * Get the name of the first car in from of the car and within its sensor range
	 * 
	 * @param tmpPos1 : position from which the car wants to know what is around itself
	 * @param range : maximum distance between the car and the other cars it can detect
	 * @param population : et of cars in which I want to look for vehicles 
	 * 	(if empty look for all cars) 
	 * @return the car (if any) in from of the itself and that is within range
	 */
	private String inFrontOfMe(OrientedPoint tmpPos1, double range, HashMap<String, OrientedPoint> population) {
		HashMap<String, OrientedPoint> pop2 = inRangeWithinMyTrain(tmpPos1, range, population);
		HashMap<String, OrientedPoint> around = new HashMap<>();
		for (String c : pop2.keySet()) {
			/*
			 * Check car's ID is less than my own (increasing ID in a single
			 * train) This allow us to avoid getting the cars that are behind us
			 * when we reach a corner of the path
			 */
			if (Integer.parseInt(c.substring(0, c.indexOf('@'))) < Integer
					.parseInt(this.getNetworkID().substring(0, this.getNetworkID().indexOf('@')))) {
				around.put(c, pop2.get(c));
			}
		}

		this.logger.fine("inRange : " + around.toString()); 
		String closerCar = null;
		for (String car : around.keySet()) {
			if (closerCar == null) {
				closerCar = car;
			} else {
				if (Functions.manhattan(this.pos, around.get(car)) < Functions.manhattan(this.pos,
						around.get(closerCar))) {
					closerCar = car;
				}
			}
		}
		return closerCar;
	}

	/**
	 * @return the distance between the car and the next car in front and within range (MAX_VALUE if no car in range)
	 */
	private double distance() {
		String inFront = inFrontOfMe(this.tmpPos, this.seeD, null);
		if (inFront != null) {
			this.logger.fine(inFront.toString() + " is in front of me."); 
			/* In reality this will be done using the sensor */
			return Functions.manhattan(this.pos, MainProgram.getEnv().getPositionOf(inFront));
		}
		return Float.MAX_VALUE;
	}

	/**
	 * Execute movement for this turn's iteration
	 * @param newSpeedParameter : speed to which the car should move
	 */
	private void executingRun(double newSpeedParameter) {
		double actualSpeed = newSpeedParameter;
		double distance;
		
		/* Compute acceleration from current speed and new speed */
		double acceleration;
		acceleration = (actualSpeed - this.pos.getSpeed()) / (Const.PAS / 1000.0f);

		/* Limit acceleration and deceleration */
		if (acceleration > Const.ACC) {
			actualSpeed = Const.ACC * (Const.PAS / 1000.0f) + this.pos.getSpeed();
		} else if (acceleration < Const.DECC) {
			actualSpeed = Const.DECC * (Const.PAS / 1000.0f) + this.pos.getSpeed();
		}

		actualSpeed = Math.min(this.maxSpeed, actualSpeed);

		/* We avoid going backward */
		if (actualSpeed > 0) {
			distance = actualSpeed * (Const.PAS / 1000.);

			this.tmpPos = this.carPath.getNextPoint(this.pos, distance, this.numTrain);
			this.logger.fine(actualSpeed + " ->" + distance + " ; from " + this.pos + " to " + this.tmpPos);   
		} else {
			actualSpeed = 0;
			distance = 0;
			this.tmpPos = this.pos;
		}

		this.printings += " fin des questions, la vitesse definitive, c'est : " + actualSpeed + "\n";  

		moveTo(this.tmpPos);
		
		/*
		 * If the leader enters in the zone around a crossing, it tells its
		 * train about it.
		 */
		if (isLeader()) {
			for (OrientedPoint cross : MainProgram.getCarPath().getCrossing()) {
				if (!this.inCrossing.contains(cross) && Functions.manhattan(this.pos, cross) < this.beaconRange
						&& !Functions.isBehind(cross, this.pos)) {
					
					HashMap<String, OrientedPoint> tmp = new HashMap<>();
					tmp.put("crossing", cross); 
					setLogLevel(Level.FINEST);

					ObjectMessage<HashMap<String, OrientedPoint>> msg = new ObjectMessage<>(tmp);
					sendMessage(Const.MY_COMMUNITY, Const.SIMU_GROUP + this.numTrain, Const.TRAIN_ROLE, msg);
					setLogLevel(this.defaultLogLevel);

					this.inCrossing.add(cross); // Add manually to avoid sending the same message twice
				}
			}
			if (this.speedPolynom != null) {
				Iterator<OrientedPoint> it = this.inCrossing.iterator();
				while (it.hasNext()) {
					OrientedPoint cross = it.next();
					
					/* Check for crossing to remove speed polynom and use constant speed instead */
					if (Environment.crossReached(this.tmpPos, cross)) {
						this.speedPolynom = null;
						this.roundSinceSetPolynom = 0;
					}
				}
			}
		}
		/*
		 * If the tail leaves a crossing, it tells its train about it.
		 */
		if (isTail()) {
			Iterator<OrientedPoint> it = this.inCrossing.iterator();
			while (it.hasNext()) {
				OrientedPoint cross = it.next();
				if (Environment.crossPassed(this.tmpPos, cross)) {
					setLogLevel(Level.FINEST);
					this.logger.fine("Sending exitCrossing " + cross); 
					HashMap<String, OrientedPoint> tmp = new HashMap<>();
					tmp.put("exitCrossing", cross); 
					ObjectMessage<HashMap<String, OrientedPoint>> msg = new ObjectMessage<>(tmp);
					sendMessage(Const.MY_COMMUNITY, Const.SIMU_GROUP + this.numTrain, Const.TRAIN_ROLE, msg);
					setLogLevel(this.defaultLogLevel);
					
					it.remove();	//Remove manually
				}
			}
		}

		/* Update the positions stored by the Environment (for stats pursposes only) */
		HashMap<String, OrientedPoint> sendPos = new HashMap<>();
		sendPos.put(this.getNetworkID(), this.tmpPos);
		sendMessage(Const.MY_COMMUNITY, this.group, Const.ENV_ROLE, new ObjectMessage<>(sendPos));
		this.pos = this.tmpPos;

		setLogLevel(this.defaultLogLevel);

		this.pos.setSpeed(actualSpeed);

		/* Print characteristics if asked by the user */
		if (this.printingTurn) {
			System.out.println(this.printings);
			this.logger.finer(this.printings);
		}
		
		this.printings = new String();
		this.printingTurn = false;	// Only display one printings string
	}

	public OrientedPoint getPos() {
		return this.pos;
	}

	public void setPos(OrientedPoint pos) {
		this.pos = pos;
	}

	@Override
	public String toString() {
		String ret = new String();
		ret += "I'm " + this.getName() + ", car " + this.position + " of train " + this.numTrain + ". My color is "    
				+ Const.CAR_COLOR[this.carColor] + ".\n"; 
		ret += this.getNetworkID() + "\n" + this.getSimpleNetworkID() + "\n";  
		ret += "Speed to reach is " + this.speedToReach + ", and safeD is " + this.safeD + "\n";   
		return ret;
	}

	public boolean isLeader() {
		return this.leader;
	}

	public void setLeader(boolean leader) {
		this.leader = leader;
	}

	public boolean isTail() {
		return this.tail;
	}

	public void setTail(boolean tail) {
		this.tail = tail;
	}
}
