package fr.utbm.ia54.gui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;

import fr.utbm.ia54.main.MainProgram;
import fr.utbm.ia54.path.CarPath;

/**
 * @author Florian & Alexis
 * Generate the background map and path coordinate
 */
public class MainFrame {
	
	private JFrame myFrame;
	private JLayeredPane superposition;
	private Menu myMenu;
	
	public  MainFrame() {
		
		/* Frame */
		myFrame = new JFrame("Car Crossing");
		myFrame.setSize(1800, 1030);
		myFrame.setLocationRelativeTo(null); // center depending on the screen
		myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		BoxLayout mainLayout = new BoxLayout(myFrame.getContentPane(), BoxLayout.Y_AXIS);
		myFrame.setLayout(mainLayout);
		
		/* Menu */
		Box menuLayout = new Box(BoxLayout.X_AXIS);
		myMenu = new Menu(myFrame);
	    menuLayout.add(myMenu);
		
		/* Background */
	    CarPath carPath = MainProgram.getCarPath();
	    JLabel fond = new JLabel(carPath.getBackground());
		fond.setBounds(0,0,carPath.getBackground().getIconWidth(), carPath.getBackground().getIconHeight());
		
		/* Special layout to superpose components */
		superposition = new JLayeredPane();
		superposition.add(fond,0,0);
		
		/* Add the components to the frame */
		myFrame.add(menuLayout);
		myFrame.add(superposition);
		myFrame.setResizable(false);
		myFrame.setVisible(true);
	}

	public Menu getMyMenu() {
		return myMenu;
	}

	public JLayeredPane getSuperposition() {
		return superposition;
	}
}
