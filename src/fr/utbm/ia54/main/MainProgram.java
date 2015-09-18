package fr.utbm.ia54.main;


import java.io.File;
import java.io.IOException;

import fr.utbm.ia54.agents.Environment;
import fr.utbm.ia54.agents.Train;
import fr.utbm.ia54.consts.Const;
import fr.utbm.ia54.gui.MainFrame;
import fr.utbm.ia54.path.CarPath;
import fr.utbm.ia54.utils.ReadXmlFile;
import madkit.kernel.Agent;

/**
 * Main class.
 * @author Alexis Florian
 */
public class MainProgram extends Agent{
	
	private static MainFrame mainFrame;
	private static CarPath carPath;
	private static Environment env;
	
	
	/**
	 * Main function of the project.
	 * @param args
	 * @throws IOException
	 */
	public static void main(String [] args) throws IOException {
		
		carPath = new CarPath();
		
		/* Build the path to follow by each train */
		ReadXmlFile read = new ReadXmlFile();		
		carPath.setPath(read.parse(new File(Const.RESOURCES_DIR+"/circuit6.xml")));
		Const.NB_TRAIN = carPath.getPath().size();
		carPath.generateCrossing();
		
		/* Generate GUI */
		mainFrame = new MainFrame();
		
        executeThisAgent(1,false); // 1 = nb of agent, false = no GUI
	}
	
	
	@Override
    protected void activate() {

		// 1 : Create groups
		for(int i=0; i<Const.NB_TRAIN;i++) {
			createGroup(Const.MY_COMMUNITY, Const.SIMU_GROUP+i);
		}
		createGroup(Const.MY_COMMUNITY, Const.TRAIN_ROLE);
		createGroup(Const.MY_COMMUNITY, Const.CAR_ROLE);
		
		// 2 : Create environment
        env = new Environment();
        launchAgent(env);
        mainFrame.getMyMenu().setEnvironnement(env);
        env.setMenu(mainFrame.getMyMenu());
		
        // 3 : Create trains 
        Train[] trains = new Train[Const.NB_TRAIN];
		for(int i=0; i<Const.NB_TRAIN;i++) {
			trains[i] = new Train();
			launchAgent(trains[i]);
			pause(1000);
		}
	}
	

	@Override
    protected void live() {
		for(int i = 0; i<1 ; i++) {
			pause(5000);
		}
	}
	
	@Override
    protected void end() {
	}
	
	public static MainFrame getMainFrame() {
		return mainFrame;
	}


	public static CarPath getCarPath() {
		return carPath;
	}

	public static Environment getEnv() {
		return env;
	}
}
