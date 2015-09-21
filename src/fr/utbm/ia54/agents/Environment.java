package fr.utbm.ia54.agents;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import fr.utbm.ia54.consts.Const;
import fr.utbm.ia54.gui.Menu;
import fr.utbm.ia54.main.MainProgram;
import fr.utbm.ia54.path.CarPath;
import fr.utbm.ia54.utils.Functions;
import fr.utbm.ia54.utils.OrientedPoint;
import madkit.kernel.Agent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import madkit.message.ObjectMessage;
import madkit.message.StringMessage;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.DomainOrder;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.SeriesException;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimePeriodValue;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeTableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYDatasetTableModel;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * @author Alexis Florian
 * This agent control every car's trajectory and warn cars about collisions.
 */
public class Environment extends Agent{
	private HashMap<String, OrientedPoint> positions;
	private HashMap<String, AgentAddress> addresses;
	private static List<List<String>> carsId; // List of car's networkId (one list by train)
	private Integer beaconRange;
	private Menu menu;

	/**
	 * This is the first activated behavior in the life cycle of a MaDKit agent.
	 */
	@Override
	protected void activate() {
		// Initialization
		positions = new HashMap<String, OrientedPoint>();
		addresses = new HashMap<String, AgentAddress>();
		carsId = new ArrayList<List<String>>();
		String group = new String();
		beaconRange = 300;
		
		for(int i=0; i<Const.NB_TRAIN;i++) {
			group = Const.SIMU_GROUP+i;
			requestRole(Const.MY_COMMUNITY, group, Const.ENV_ROLE);
			carsId.add(new ArrayList<String>());
		}
	}

	@Override
    protected void live() {
		HashMap<OrientedPoint, List<String>> map = new HashMap<OrientedPoint, List<String>>(); // List of trains by crossing
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
		//TODO Work only for 2 trains
		menu.addCarList(carsId.get(0));
		menu.addCarList(carsId.get(1));

	/*********************stats*****************************/
		int interdistance;
		Long runningT = System.currentTimeMillis();
		List<XYSeriesCollection> seriesInterD = new ArrayList<XYSeriesCollection>();
		List<XYSeriesCollection> seriesSpeed = new ArrayList<XYSeriesCollection>();
		
		
		Frame frame = new Frame("Simulation Stats");
		frame.setLayout(new GridLayout(2, 0));

		for (int i = 0 ; i <= carsId.size(); i++) {
			//by train we prepare 2 graphics, one for interdistance between cars and the other for cars speed
			
			XYSeriesCollection dataD = new XYSeriesCollection( );
			seriesInterD.add(dataD);
			XYSeriesCollection dataV = new XYSeriesCollection( );
			seriesSpeed.add(dataV);
			
			for (int j = 0; j<carsId.get(i).size(); j++) {
				final XYSeries serieD = new XYSeries ( "Car" + j + " and Car" + j+1 );
				dataD.addSeries(serieD);
				final XYSeries serieV = new XYSeries ( "Car" + j );
				dataV.addSeries(serieV);
			}
			final XYSeries serieV = new XYSeries ( "Car" + carsId.get(i).size() );
			dataV.addSeries(serieV);
			
			
			JFreeChart xylineChartD = ChartFactory.createXYLineChart(
		         	"interdistance for train0",
		         	"time" ,
		         	"distance from previous car" ,
		         	dataD,
		         	PlotOrientation.VERTICAL ,
		         	true , true , false);
			JFreeChart xylineChartV = ChartFactory.createXYLineChart(
		         	"speed for train0",
		         	"time" ,
		         	"speed of car" ,
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
			frame.add(panelD);
			ChartPanel panelV = new ChartPanel(xylineChartV);
			frame.add(panelV);
		}
		
		
		/*List<XYSeries> series2 = new ArrayList<XYSeries>();
		final XYSeries  train1car2 = new XYSeries ( "train1car2" ); 
		series2.add(train1car2);
		final XYSeries  train1car3 = new XYSeries ( "train1car3" ); 
		series2.add(train1car3);
		final XYSeries  train1car4 = new XYSeries ( "train1car4" ); 
		series2.add(train1car4);
		final XYSeries  train1car5 = new XYSeries ( "train1car5" ); 
		series2.add(train1car5);  
		XYSeriesCollection data2 = new XYSeriesCollection( );
		data2.addSeries(train1car2);
		data2.addSeries(train1car3);
		data2.addSeries(train1car4);
		data2.addSeries(train1car5);
		JFreeChart xylineChart2 = ChartFactory.createXYLineChart(
		         "interdistance for train1",
		         "time" ,
		         "distance from previous car" ,
		         data2 ,
		         PlotOrientation.VERTICAL ,
		         true , true , false);
		xylineChart2.setBackgroundPaint(Color.gray);
		//xylineChart2.getPlot().addRangeMarker(new Marker(110, Color.white, new BasicStroke(1), Color.red, 1f));
		//ChartFrame frame2 = new ChartFrame("INTERDISTANCE TRAIN 2", xylineChart2);
		//frame2.pack();
		//frame2.setVisible(true);
		
		
		List<XYSeries> series3 = new ArrayList<XYSeries>();
		final XYSeries  train1car2 = new XYSeries ( "train1car2" ); 
		series3.add(train1car2);
		final XYSeries  train1car3 = new XYSeries ( "train1car3" ); 
		series3.add(train1car3);
		final XYSeries  train1car4 = new XYSeries ( "train1car4" ); 
		series3.add(train1car4);
		final XYSeries  train1car5 = new XYSeries ( "train1car5" ); 
		series3.add(train1car5);  
		XYSeriesCollection data3 = new XYSeriesCollection( );
		data3.addSeries(train1car2);
		data3.addSeries(train1car3);
		data3.addSeries(train1car4);
		data3.addSeries(train1car5);
		JFreeChart xylineChart3 = ChartFactory.createXYLineChart(
		         "interdistance for train1",
		         "time" ,
		         "distance from previous car" ,
		         data3 ,
		         PlotOrientation.VERTICAL ,
		         true , true , false);
		xylineChart3.setBackgroundPaint(Color.gray);
		
		
		
		ChartPanel panel2 = new ChartPanel(xylineChart2);
		frame.add(panel2);
		ChartPanel panel3 = new ChartPanel(xylineChart3);
		frame.add(panel3);*/
		
		frame.pack();
		frame.setVisible(true);
		
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
			
			getNewMessages();
			
			for(OrientedPoint cross : carPath.getCrossing()){
				// Get the trains which are on the cross
				groups = map.get(cross);
				if(groups == null){
					groups = new ArrayList<>();
				}
				
				// For all trains
				for(int i=0; i<carsId.size();i++){
					carGroup = Const.SIMU_GROUP + String.valueOf(i);
					
					//if the train is checked in, we verify it's still in
					//TODO : do it when updates of positions fo first and last car
					if(groups.contains(carGroup)) {
						carId = carsId.get(i).get(carsId.get(i).size()-1);
						carPos = positions.get(carId);
						//System.out.println("crosspassed : " + carId + ", and " +cross);
						if(crossPassed(carPos,cross)) {
							groups.remove(groups.indexOf(carGroup));
							
							HashMap<String, OrientedPoint> tmp = new HashMap<String,OrientedPoint>();
							tmp.put("exitCrossing", cross);
							
							ObjectMessage<HashMap<String,OrientedPoint>> msg = new ObjectMessage<HashMap<String,OrientedPoint>>(tmp);
							sendMessage(Const.MY_COMMUNITY, carGroup, Const.TRAIN_ROLE, msg);
						}
					}
					else {// otherwise we check if it's entering the crossing
						carId = carsId.get(i).get(0);
						carPos = positions.get(carId);
						//System.out.println("train"+i+"car"+carId+"?");
						if(Functions.manhattan(carPos,cross) < beaconRange ){//&& carPath.isInPath(carPos, i, cross, beaconRange)){
							//System.out.println("a train in a crossing, great");
							//we add he train to the cross and alert him
							HashMap<String, OrientedPoint> tmp = new HashMap<String,OrientedPoint>();
							tmp.put("crossing", cross);
							
							ObjectMessage<HashMap<String,OrientedPoint>> msg = new ObjectMessage<HashMap<String,OrientedPoint>>(tmp);
							sendMessage(Const.MY_COMMUNITY, carGroup, Const.TRAIN_ROLE, msg);
							groups.add(carGroup);
							map.put(cross,groups);
						}
					}
				}
			}
			
			if(runningT + Const.PAS <= System.currentTimeMillis()) {
				runningT = System.currentTimeMillis();
				
				for (int i = 0 ; i <= carsId.size(); i++) {
					for (int j = 0; j<carsId.get(i).size(); j++) {
						interdistance = Functions.manhattan(positions.get(carsId.get(i).get(j)),positions.get(carsId.get(i).get(j+1)));
						seriesInterD.get(i).get(j).add(runningT.intValue(), interdistance); 
						seriesSpeed.get(i).get(j).add(runningT.intValue(), positions.get(carsId.get(i).get(j).getSpeed());
					}
					seriesSpeed.get(i).get(j).add(runningT.intValue(), positions.get(carsId.get(i).get(j).getSpeed());
				}
				/*for (int j=0; j<series.size();j++) {

				interdistance = Functions.manhattan(positions.get(carsId.get(0).get(j)),positions.get(carsId.get(0).get(j+1)));
				series.get(j).add(runningT.intValue(), interdistance); 	
					//System.out.println("train"+i+"car"+j+", interD :"+ interdistance);
				}
				for (int j=0; j<series2.size();j++) {
				interdistance = Functions.manhattan(positions.get(carsId.get(1).get(j)),positions.get(carsId.get(1).get(j+1)));
				series2.get(j).add(runningT.intValue(), interdistance); 	
					//System.out.println("train"+i+"car"+j+", interD :"+ interdistance);
				}*/
			}
		}
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
				/*if(tmp.get("transfert") != null) {
					Iterator it = tmp.keySet().iterator();
					String tmpName = new String();
					do {
						tmpName = (String) it.next();
					}while(tmpName == "transfert");
					String[] data = tmpName.split(":");
					HashMap<String, OrientedPoint> tmpData = new HashMap<String, OrientedPoint>();
					tmpData.put(data[2],tmp.get(tmpName));
					ObjectMessage<HashMap<String,OrientedPoint>> msg = new ObjectMessage<HashMap<String,OrientedPoint>>(tmpData);
					sendMessage(Const.MY_COMMUNITY, data[0], data[1], msg);
				} else {*/
					positions.putAll(tmp);
					// substring to convert agent network id to network id
					addresses.put(message.getSender().getAgentNetworkID().substring(0, message.getSender().getAgentNetworkID().length()-2), message.getSender());
					
					String address = message.getSender().getAgentNetworkID();
					String group = message.getSender().getGroup();
					//System.out.println(address +" has send it's position");
					if(!carsId.get(getNumTrain(group)).contains(address.substring(0,address.length()-2))){
						//System.out.println("adresse : " + address + ", and on stocke : "+ address.substring(0,address.length()-2));
						carsId.get(getNumTrain(group)).add(address.substring(0,address.length()-2));
					//}
				}
			}
		}
	}
	
	public HashMap<String, OrientedPoint> inRange(OrientedPoint target, int range, HashMap<String, OrientedPoint> exclus) {
		return inRange(target, range, positions, exclus);
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
	public HashMap<String, OrientedPoint> inRange(OrientedPoint target, int range, HashMap<String, OrientedPoint> population, HashMap<String, OrientedPoint> exclus) {
		
		if(!positions.equals(null)) {
			HashMap<String, OrientedPoint> voisins = new HashMap<String, OrientedPoint>();
			float distance = 0;
			
			for(String ad : population.keySet()) {
				distance = Functions.manhattan(population.get(ad), target);
				if(distance < range && distance > 0 && !exclus.containsKey(ad)) {
					voisins.put(ad, population.get(ad));
				}
			}
			return voisins;
		}
		return null;
	}
	
	public void sendMessageToId(String netwId, String message) {
		AgentAddress addr = addresses.get(netwId);
		if (addr != null) {
			sendMessage(addr, new StringMessage(message));
		}
	}
	
	/**
	 * Get the number of the train
	 * @param group
	 * @return
	 */
	public int getNumTrain(String group) {
		char c = group.charAt(group.length()-1);
		return Character.getNumericValue(c);
	}
	
	/**
	 * Does the car 
	 * @param car
	 * @param cross
	 * @return
	 */
	public boolean crossPassed(OrientedPoint car, OrientedPoint cross){
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
	 * To detect which cars passed the cross
	 * @param car
	 * @param cross
	 * @return
	 */
	public boolean crossPassedBis(OrientedPoint car, OrientedPoint cross){
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
		
		StringMessage msg = new StringMessage("printPriority");
		
		for (int i=0; i<carsId.size();i++) {
			String carGroup = Const.SIMU_GROUP + String.valueOf(i);
			broadcastMessage(Const.MY_COMMUNITY, carGroup, Const.CAR_ROLE, msg);
		}
	}

	public Menu getMenu() {
		return menu;
	}

	public void setMenu(Menu menu) {
		this.menu = menu;
	}

	public void printAllTurn(Object selectedItem) {
		sendMessage(addresses.get(selectedItem), new StringMessage("Print"));
	}
}
