package fr.utbm.ia54.gui;


import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

import fr.utbm.ia54.agents.Environment;
import fr.utbm.ia54.consts.Const;

/**
 * Menu bar class.
 * @author Alexis Florian
 */
public class Menu extends JPanel implements ActionListener{

	private static final long serialVersionUID = 1L;

	private JComboBox<String> carList;
	private JButton flag = new JButton("Checkpoint"); //$NON-NLS-1$
	private JButton priority = new JButton("Check Priorities"); //$NON-NLS-1$
	private JButton stats = new JButton("Print Statistics"); //$NON-NLS-1$
	private JButton quit = new JButton("Quit"); //$NON-NLS-1$
	private JButton accelerate = new JButton("Accelerate"); //$NON-NLS-1$
	//private JSpinner spinner;
	private JFrame myFrame;
	private Environment environnement;
	
	private boolean statsPrinted = false;
	
	
	/**
	 * Creates the application's menu
	 */
	@SuppressWarnings("serial")
	public Menu(JFrame myFrame) {
	    super(true);
	    this.myFrame = myFrame;
	    
	    // BoxLayout
	    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	    Box box1 = new Box(BoxLayout.X_AXIS);
	    // Buttons
	    this.carList = new JComboBox<String>(){

	        @Override
	        public Dimension getMaximumSize() {
	            Dimension max = super.getMaximumSize();
	            max.height = getPreferredSize().height;
	            return max;
	        }

	    };
	    this.carList.setVisible(false);
	    
	    //box1.add(new Label("Print Tour"));
	    box1.add(this.carList);
	    box1.add(this.flag);
	    box1.add(this.stats);
	    box1.add(this.priority);
	    box1.add(this.accelerate);
	    box1.add(this.quit);
	    
	    // Listeners
	    this.carList.addActionListener(this);
		this.flag.addActionListener(this);
		this.stats.addActionListener(this);
		this.priority.addActionListener(this);
		this.quit.addActionListener(this);
		this.accelerate.addActionListener(this);

	    this.add(box1);
	  }
	
	public Environment getEnvironnement() {
		return this.environnement;
	}

	public void setEnvironnement(Environment environnement) {
		this.environnement = environnement;
	}

	/**
	 * Listeners onClick fonction
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();

		if (source == this.flag) {
			System.out.println("<---------------------------- CHECKPOINT ---------------------------->"); //$NON-NLS-1$
		}
		else if (source == this.priority) {
			System.out.println("<--------------------- CHECK of CARS PRIORITIES --------------------->"); //$NON-NLS-1$
			this.environnement.printAllPriorities();
		}
		else if (source == this.carList) {
			System.out.println("<--------------------- CHECK of A CAR SAYNGS --------------------->"); //$NON-NLS-1$
			this.environnement.printAllTurn(this.carList.getSelectedItem());
		} 
		else if (source == this.stats) {
			this.statsPrinted = !this.statsPrinted;
			System.out.println("<--------------------- print of stats = "+this.statsPrinted+" --------------------->"); //$NON-NLS-1$ //$NON-NLS-2$
			this.environnement.printStats(this.statsPrinted);
		} 
		else if(source == this.accelerate) {
			if(Const.debugAccelerator == 1.f) 
				Const.debugAccelerator = .5f;
			else
				Const.debugAccelerator = 1.f;
		}
		else if (source == this.quit)
			this.myFrame.dispatchEvent(new WindowEvent(this.myFrame, WindowEvent.WINDOW_CLOSING));
    }

	public void addCarList(List<String> list) {
		for(String tmp : list) {
			this.carList.addItem(tmp);
		}
	    this.carList.setVisible(true);
		
	}
}

