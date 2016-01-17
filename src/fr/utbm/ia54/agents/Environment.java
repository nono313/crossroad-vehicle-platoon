package fr.utbm.ia54.agents;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import fr.utbm.ia54.consts.Const;
import fr.utbm.ia54.gui.Menu;
import fr.utbm.ia54.main.MainProgram;
import fr.utbm.ia54.path.CarPath;
import fr.utbm.ia54.utils.Collision;
import fr.utbm.ia54.utils.Functions;
import fr.utbm.ia54.utils.OrientedPoint;
import madkit.kernel.Agent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import madkit.message.ObjectMessage;
import madkit.message.StringMessage;

/**
 * @author Alexis Florian
 * This agent control every car's trajectory and warn cars about collisions.
 */
public class Environment extends Agent{
	private HashMap<String, OrientedPoint> positions;
	private HashMap<String, AgentAddress> addresses;
	private static List<List<String>> carsId; // List of car's networkId (one list by train)
	private double beaconRange;
	private Menu menu;
	private Frame frame;
	private Level defaultLogLevel = Level.FINE;
	private Level moreLog = Level.FINE;
	private List<Collision> collisions;

	/**
	 * This is the first activated behavior in the life cycle of a MaDKit agent.
	 */
	@Override
	protected void activate() {
		setLogLevel(this.defaultLogLevel);
		// Initialization
		this.positions = new HashMap<>();
		this.addresses = new HashMap<>();
		this.collisions = new ArrayList<>();
		carsId = new ArrayList<>();
		String group = new String();
		this.beaconRange = 500;	// Distance from which a train is considered entering a crossing
		
		for(int i=0; i<Const.NB_TRAIN;i++) {
			group = Const.SIMU_GROUP+i;
			requestRole(Const.MY_COMMUNITY, group, Const.ENV_ROLE);
			carsId.add(new ArrayList<String>());
		}
	}

	@Override
    protected void live() {
		//System.out.println(this.getClass().getSimpleName()+" is living.");
		HashMap<OrientedPoint, List<String>> trainsPerCrossing = new HashMap<>(); // List of trains by crossing
		int nb = 0;
		CarPath carPath = MainProgram.getCarPath();
		
		
		// Waiting for car initialization
		while(nb != carsId.size()*Const.NB_CAR_BY_TRAIN) {
			getNewMessages();
			nb = 0;
			for(int i=0; i<carsId.size();i++){
				nb += carsId.get(i).size();
			}
		}
		for(int i=0; i<carsId.size();i++){
			this.menu.addCarList(carsId.get(i));
		}

	/*********************stats*****************************/
		double interdistance;
		Long runningT = System.currentTimeMillis();
		List<XYSeriesCollection> seriesInterD = new ArrayList<>();
		List<XYSeriesCollection> seriesSpeed = new ArrayList<>();
		
		
		this.frame = new Frame("Simulation Stats"); //$NON-NLS-1$
		this.frame.setLayout(new GridLayout(2, 0));

		for (int i = 0 ; i < carsId.size(); i++) {
			//by train we prepare 2 graphics, one for inter-distance between cars and the other for cars speed
			
			XYSeriesCollection dataD = new XYSeriesCollection( );
			seriesInterD.add(dataD);
			XYSeriesCollection dataV = new XYSeriesCollection( );
			seriesSpeed.add(dataV);
			
			for (int j = 0; j<carsId.get(i).size()-1; j++) {
				final XYSeries serieD = new XYSeries ( "Car" + j + " and Car" + (j+1) ); //$NON-NLS-1$ //$NON-NLS-2$
				dataD.addSeries(serieD);
				final XYSeries serieV = new XYSeries ( "Car" + j ); //$NON-NLS-1$
				dataV.addSeries(serieV);
			}
			final XYSeries serieV = new XYSeries ( "Car" + carsId.get(i).size() ); //$NON-NLS-1$
			dataV.addSeries(serieV);
			
			
			JFreeChart xylineChartD = ChartFactory.createXYLineChart(
		         	"interdistance for train"+i, //$NON-NLS-1$
		         	"time" , //$NON-NLS-1$
		         	"distance from previous car" , //$NON-NLS-1$
		         	dataD,
		         	PlotOrientation.VERTICAL ,
		         	true , true , false);
			JFreeChart xylineChartV = ChartFactory.createXYLineChart(
		         	"speed for train"+i, //$NON-NLS-1$
		         	"time" , //$NON-NLS-1$
		         	"speed of car" , //$NON-NLS-1$
		         	dataV,
		         	PlotOrientation.VERTICAL ,
		         	true , true , false);
		        if(i%2==0) {
				xylineChartD.setBackgroundPaint(Color.white);
				xylineChartV.setBackgroundPaint(Color.gray);
		        }	
			else {
				xylineChartV.setBackgroundPaint(Color.white);
				xylineChartD.setBackgroundPaint(Color.gray);
			}
			
			ChartPanel panelD = new ChartPanel(xylineChartD);
			this.frame.add(panelD);
			ChartPanel panelV = new ChartPanel(xylineChartV);
			this.frame.add(panelV);
		}
		
		this.frame.pack();
		this.frame.setVisible(false);
		
		/*Here we simulate the environment with beaconised's crossings
		  When a train's first car enter it's range, we send a message to the train (upcoming crossing).
		  
		  To avoid sending the message every turn until the car passed the crossing,
		  we keep a list of train by crossing. To update that list we check if the last car passed the crossing
		  At that time we also inform the train, so it can return to usual speed and distance orders
		  
		  IMPROVEMENT : IRL only cars have sensors, so the first car should recieve the event,
		  and send it to its train, probably
		  */
		while(true) {
			
			List<String> groups;
			OrientedPoint carPos;
			String carGroup;
			String carId;
			
			/* Receiving messages from cars indicating their locations */
			getNewMessages();
			
			// Checking all the crossings
			for(OrientedPoint cross : carPath.getCrossing()){
				// Get the trains which are on the cross
				groups = trainsPerCrossing.get(cross);
				if(groups == null){
					groups = new ArrayList<>();
				}
				
				// For all trains
				for(int i=0; i<carsId.size();i++){
					carGroup = Const.SIMU_GROUP + String.valueOf(i);
					
					//if the train is checked in, we verify it's still in
					//TODO : do it when updates of positions fo first and last car
					if(groups.contains(carGroup)) {
						/*carId = carsId.get(i).get(0);
						carPos = positions.get(carId);
						if(Functions.manhattanCar(carPos,cross) < Const.CAR_SIZE ){
							//System.out.println("a train in a crossing, great");
							//we add he train to the cross and alert him
							HashMap<String, OrientedPoint> tmp = new HashMap<String,OrientedPoint>();
							tmp.put("enteringCrossing", cross);
							
							ObjectMessage<HashMap<String,OrientedPoint>> msg = new ObjectMessage<HashMap<String,OrientedPoint>>(tmp);
							sendMessage(Const.MY_COMMUNITY, carGroup, Const.TRAIN_ROLE, msg);
							groups.add(carGroup);
							map.put(cross,groups);
						}*/
						
						carId = carsId.get(i).get(carsId.get(i).size()-1);	//Get last car of the train
						carPos = this.positions.get(carId);
						if(crossPassed(carPos,cross)) {	// When last car of the train went through the crossing
							this.logger.info("groups : " + groups.toString()); //$NON-NLS-1$
							groups.remove(groups.indexOf(carGroup));
							this.logger.info("groups : " + groups.toString()); //$NON-NLS-1$
							
							/*
							HashMap<String, OrientedPoint> tmp = new HashMap<String,OrientedPoint>();
							tmp.put("exitCrossing", cross);
							
							ObjectMessage<HashMap<String,OrientedPoint>> msg = new ObjectMessage<HashMap<String,OrientedPoint>>(tmp);
							sendMessage(Const.MY_COMMUNITY, carGroup, Const.TRAIN_ROLE, msg);
							*/
						}
					}
					else {// otherwise we check if it's entering the crossing
						carId = carsId.get(i).get(0);
						carPos = this.positions.get(carId);
						
						//System.out.println("train"+i+"car"+carId+"?");
						if(Functions.manhattan(carPos,cross) < this.beaconRange && !Functions.isBehind(cross, carPos)){//&& carPath.isInPath(carPos, i, cross, beaconRange)){
							//logger.fine("a train in a crossing, great");
							//we add he train to the cross and alert him
							HashMap<String, OrientedPoint> tmp = new HashMap<>();
							tmp.put("crossing", cross); //$NON-NLS-1$
							
							/*
							ObjectMessage<HashMap<String,OrientedPoint>> msg = new ObjectMessage<>(tmp);
							sendMessage(Const.MY_COMMUNITY, carGroup, Const.TRAIN_ROLE, msg);
							*/
							groups.add(carGroup);
							trainsPerCrossing.put(cross,groups);
						}
					}
				}
			}
			
			
			
			Iterator<Collision> itCol = collisions.iterator();
			while(itCol != null && itCol.hasNext()) {
				Collision c = itCol.next();
				OrientedPoint o1 = positions.get(c.getA());
				OrientedPoint o2 = positions.get(c.getB());
				if(!(Math.abs(o1.getX() - o2.getX()) < Const.CAR_SIZE && o1.getY() == o2.getY() ||
						Math.abs(o1.getY() - o2.getY()) < Const.CAR_SIZE && o1.getX() == o2.getX())) {
					itCol.remove();
				}
			}
			for(String car1 : carsId.get(0)) {
				for(String car2 : carsId.get(1)) {
					OrientedPoint o1 = positions.get(car1);
					OrientedPoint o2 = positions.get(car2);
					
					if(Math.abs(o1.getX() - o2.getX()) < Const.CAR_SIZE && o1.getY() == o2.getY() ||
							Math.abs(o1.getY() - o2.getY()) < Const.CAR_SIZE && o1.getX() == o2.getX()) {
						Collision col = new Collision(car1, car2);

						if(!collisions.contains(col)) {
							collisions.add(col);
							logger.warning("! Collision between "+car1 +" and " +car2);
						}
					}
				}
			}
			//*/
			// If the time step is reached since the last iteration
			if(runningT + Const.PAS <= System.currentTimeMillis()) {
				runningT = System.currentTimeMillis();
				
				/*
				 * This part only concerns the statistics displayed with JFreeChat!
				 * In reality, the environment doesn't need to know about the interdistance
				 */
				for (int i = 0 ; i < carsId.size(); i++) {
					for (int j = 0; j<carsId.get(i).size()-1; j++) {
						interdistance = Functions.manhattan(this.positions.get(carsId.get(i).get(j)),this.positions.get(carsId.get(i).get(j+1)));
						seriesInterD.get(i).getSeries(j).add(runningT.intValue(), interdistance); 
						seriesSpeed.get(i).getSeries(j).add(runningT.intValue(), this.positions.get(carsId.get(i).get(j)).getSpeed());
					}
					seriesSpeed.get(i).getSeries(carsId.get(i).size()-1).add(runningT.intValue(), this.positions.get(carsId.get(i).get(carsId.get(i).size()-1)).getSpeed());
				}
			}
		}
	}
	
	/**
	 * Override function to get some log data
	 * @author nathan
	 */
	@Override
	public ReturnCode sendMessage(final String community, final String group, final String role, final Message message) {
		setLogLevel(this.moreLog);
		ReturnCode ret = super.sendMessage(community,  group, role, message);
		setLogLevel(this.defaultLogLevel);
		return ret;
	}

	
	
	@Override
    protected void end() {
		// Nothing to do
	}
	
	/**
	 * Check for new messages about agent's positions
	 */
	private void getNewMessages() {
		Message m = null;
		
		// we manage all messages
		while (!isMessageBoxEmpty()){
			m = nextMessage();
			
			if (m instanceof ObjectMessage) {
				@SuppressWarnings("unchecked")
				ObjectMessage<HashMap<String, OrientedPoint>> message = (ObjectMessage<HashMap<String, OrientedPoint>>) m;
				HashMap<String, OrientedPoint> tmp = message.getContent();
				//TODO transmit to other group
				this.positions.putAll(tmp);
				
				String address = message.getSender().getAgentNetworkID();
				String group = message.getSender().getGroup();
				
				// substring to convert agent network id to network id
				/* 
				 * Update addresses map
				 * This only needs to be done once for each agent because our agents are not moving on the network ! 
				 * */
				String simpleName = address.substring(0,address.lastIndexOf('-'));
				if(this.addresses.get(simpleName) == null) {
					this.addresses.put(simpleName, message.getSender());
					this.logger.fine("Update addresses at "+message.getSender().getAgentNetworkID().substring(0, message.getSender().getAgentNetworkID().lastIndexOf('-')) + " with : " + message.getSender()); //$NON-NLS-1$ //$NON-NLS-2$
				}
				
				/* Store the composition of each trains inside the Environment */
				if(!carsId.get(getNumTrain(group)).contains(simpleName)){
					carsId.get(getNumTrain(group)).add(simpleName);
					this.logger.fine(carsId.toString());
				}
			}
		}
	}
	
	public HashMap<String, OrientedPoint> inRange(OrientedPoint target, int range, HashMap<String, OrientedPoint> exclus) {
		return inRange(target, range, this.positions, exclus);
	}
		
	
	/**
	 * 
	 * Search for oriented points within rand distance of target.
	 * Population contain potential targets and exclus targets not to select
	 * @param target
	 * @param range
	 * @param population
	 * @param exclus
	 * @return
	 */
	public HashMap<String, OrientedPoint> inRange(OrientedPoint target, double range, HashMap<String, OrientedPoint> population, HashMap<String, OrientedPoint> exclus) {
		
		if(!this.positions.equals(null)) {
			HashMap<String, OrientedPoint> voisins = new HashMap<>();
			double distance = 0;
			
			for(String ad : population.keySet()) {
				distance = Functions.manhattan(population.get(ad), target);
				//logger.fine("distance from " + population.get(ad) + " to "+target+" = "+distance);
				if(distance < range && distance > 0 && !exclus.containsKey(ad)) {
					voisins.put(ad, population.get(ad));
				}
			}
			return voisins;
		}
		return null;
	}

	public HashMap<String, OrientedPoint> inRangeWithinMyTrain(OrientedPoint target, double range, String networkId, HashMap<String, OrientedPoint> exclus) {
		return inRangeWithinMyTrain(target, range, networkId, this.positions, exclus);
	}
	
	public HashMap<String, OrientedPoint> inRangeWithinMyTrain(OrientedPoint target, double range, String networkId, @SuppressWarnings("unused") HashMap<String, OrientedPoint> population, HashMap<String, OrientedPoint> exclus) {
		HashMap<String, OrientedPoint> pop = new HashMap<>();
		HashMap<String, OrientedPoint> ret = null;
		for(List<String> l : carsId) {
			if(l.contains(networkId)) {
				for(String car : l) {
					pop.put(car, this.positions.get(car));
				}
				ret = inRange(target, range, pop, exclus);
			}
		}
		return ret;
	}

	
	
	public void sendMessageToId(String netwId, String message) {
		AgentAddress addr = this.addresses.get(netwId);
		if (addr != null) {
			sendMessage(addr, new StringMessage(message));
		}
	}
	
	/**
	 * Get the number of the train
	 * @param group
	 * @return
	 */
	public static int getNumTrain(String group) {
		char c = group.charAt(group.length()-1);
		return Character.getNumericValue(c);
	}
	
	/**
	 * Does the car 
	 * @param car
	 * @param cross
	 * @return
	 */
	public static boolean crossPassed(OrientedPoint car, OrientedPoint cross){
		if(Math.toDegrees(car.getAngle()) == 90){ 		// Right
			if(car.y == cross.y)
				return (car.x - Const.CAR_SIZE > cross.x) ? true:false;
		}
		else if(Math.toDegrees(car.getAngle()) == 180){ 	// Down
			if(car.x == cross.x)
				return (car.y - Const.CAR_SIZE > cross.y) ? true:false;
		}
		else if(Math.toDegrees(car.getAngle()) == 270){ 	// Left
			if(car.y == cross.y)
				return (car.x + Const.CAR_SIZE < cross.x) ? true:false;
		}
		else{ 							// Up
			if(car.x == cross.x)
				return (car.y + Const.CAR_SIZE < cross.y) ? true:false;
		}
		return false;
	}
	
	/**
	 * Does the car 
	 * @param car
	 * @param cross
	 * @return
	 */
	public static boolean crossReached(OrientedPoint car, OrientedPoint cross){
		if(Math.toDegrees(car.getAngle()) == 90){ 		// Right
			if(car.y == cross.y)
				return (car.x >= cross.x) ? true:false;
		}
		else if(Math.toDegrees(car.getAngle()) == 180){ 	// Down
			if(car.x == cross.x)
				return (car.y >= cross.y) ? true:false;
		}
		else if(Math.toDegrees(car.getAngle()) == 270){ 	// Left
			if(car.y == cross.y)
				return (car.x <= cross.x) ? true:false;
		}
		else{ 							// Up
			if(car.x == cross.x)
				return (car.y <= cross.y) ? true:false;
		}
		return false;
	}
	
	/**
	 * To detect which cars passed the cross
	 * @param car
	 * @param cross
	 * @return
	 */
	public static boolean crossPassedBis(OrientedPoint car, OrientedPoint cross){
		if(Math.toDegrees(car.getAngle()) == 90){ 			// Right
			return (car.x > cross.x) ? true:false;
		}
		else if(Math.toDegrees(car.getAngle()) == 180){ 	// Down
			return (car.y > cross.y) ? true:false;
		}
		else if(Math.toDegrees(car.getAngle()) == 270){ 	// Left
			return (car.x < cross.x) ? true:false;
		}
		else{ 												// Up
			return (car.y < cross.y) ? true:false;
		}
	}
	
	
	public static boolean isInMyTrain(String me, String neighbour) {
		boolean isInTrain = false;
		int trainCounter = 1;
		int carTrain = -1;
		int otherCarTrain = -2;
		for (List<String> i: carsId) {
			trainCounter++;
			for(String j: i) {
				if (j.equals(me))
					carTrain = trainCounter;
				if (j.equals(neighbour))
					otherCarTrain = trainCounter;
			}
		}
		
		if(carTrain == otherCarTrain){
			isInTrain = true;
		}
		
		return isInTrain;
	}

	public void printAllPriorities() {
		
		StringMessage msg = new StringMessage("printPriority"); //$NON-NLS-1$
		
		for (int i=0; i<carsId.size();i++) {
			String carGroup = Const.SIMU_GROUP + String.valueOf(i);
			broadcastMessage(Const.MY_COMMUNITY, carGroup, Const.CAR_ROLE, msg);
		}
	}

	public Menu getMenu() {
		return this.menu;
	}

	public void setMenu(Menu menu) {
		this.menu = menu;
	}

	public void printAllTurn(Object selectedItem) {
		sendMessage(this.addresses.get(selectedItem), new StringMessage("Print")); //$NON-NLS-1$
	}
	public void printStats(boolean print) {
		this.frame.setVisible(print);
	}
	public OrientedPoint getPositionOf(String car) {
		return this.positions.get(car);
	}

	public double getBeaconRange() {
		return this.beaconRange;
	}
}
