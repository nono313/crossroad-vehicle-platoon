package fr.utbm.ia54.gui;


import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fr.utbm.ia54.agents.Environment;

/**
 * Menu bar class.
 * @author Alexis Florian
 */
public class Menu extends JPanel implements ActionListener{

	private static final long serialVersionUID = 1L;

	private JComboBox<String> carList;
	private JButton flag = new JButton("Checkpoint");
	private JButton priority = new JButton("Check Priorities");
	private JButton quit = new JButton("Quit");
	//private JSpinner spinner;
	private JFrame myFrame;
	private Environment environnement;
	
	
	/**
	 * Creates the application's menu
	 */
	public Menu(JFrame myFrame) {
	    super(true);
	    this.myFrame = myFrame;
	    
	    // BoxLayout
	    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	    Box box1 = new Box(BoxLayout.X_AXIS);
	    // Buttons
	    carList = new JComboBox<String>(){

	        @Override
	        public Dimension getMaximumSize() {
	            Dimension max = super.getMaximumSize();
	            max.height = getPreferredSize().height;
	            return max;
	        }

	    };
	    carList.setVisible(false);
	    
	    //box1.add(new Label("Print Tour"));
	    box1.add(carList);
	    box1.add(flag);
	    box1.add(priority);
	    box1.add(quit);
	    
	    // Listeners
	    carList.addActionListener(this);
		flag.addActionListener(this);
		priority.addActionListener(this);
		quit.addActionListener(this);

	    this.add(box1);
	  }
	
	public Environment getEnvironnement() {
		return environnement;
	}

	public void setEnvironnement(Environment environnement) {
		this.environnement = environnement;
	}

	/**
	 * Listeners onClick fonction
	 */
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();

		if (source == flag) {
			System.out.println("<---------------------------- CHECKPOINT ---------------------------->");
		}
		else if (source == priority) {
			System.out.println("<--------------------- CHECK of CARS PRIORITIES --------------------->");
			environnement.printAllPriorities();
		}
		else if (source == carList) {
			System.out.println("<--------------------- CHECK of A CAR SAYNGS --------------------->");
			environnement.printAllTurn(carList.getSelectedItem());
		} 
		else if (source == quit)
			myFrame.dispatchEvent(new WindowEvent(myFrame, WindowEvent.WINDOW_CLOSING));
    }

	public void addCarList(List<String> list) {
		for(String tmp : list) {
			carList.addItem(tmp);
		}
	    carList.setVisible(true);
		
	}
}

