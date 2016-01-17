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
	
	@SuppressWarnings("boxing")
	public  MainFrame() {
		
		/* Frame */
		this.myFrame = new JFrame("Car Crossing"); //$NON-NLS-1$
		this.myFrame.setSize(1800, 1030);
		this.myFrame.setLocationRelativeTo(null); // center depending on the screen
		this.myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		BoxLayout mainLayout = new BoxLayout(this.myFrame.getContentPane(), BoxLayout.Y_AXIS);
		this.myFrame.setLayout(mainLayout);
		
		/* Menu */
		Box menuLayout = new Box(BoxLayout.X_AXIS);
		this.myMenu = new Menu(this.myFrame);
	    menuLayout.add(this.myMenu);
		
		/* Background */
	    CarPath carPath = MainProgram.getCarPath();
	    JLabel fond = new JLabel(carPath.getBackground());
		fond.setBounds(0,0,carPath.getBackground().getIconWidth(), carPath.getBackground().getIconHeight());
		
		/* Special layout to superpose components */
		this.superposition = new JLayeredPane();
		this.superposition.add(fond,0,0);
		
		/* Add the components to the frame */
		this.myFrame.add(menuLayout);
		this.myFrame.add(this.superposition);
		this.myFrame.setResizable(false);
		this.myFrame.setVisible(true);
	}

	public Menu getMyMenu() {
		return this.myMenu;
	}

	public JLayeredPane getSuperposition() {
		return this.superposition;
	}
}
