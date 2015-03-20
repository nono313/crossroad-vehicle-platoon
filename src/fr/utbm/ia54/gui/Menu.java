package fr.utbm.ia54.gui;


import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Menu bar class.
 * @author Alexis Florian
 */
public class Menu extends JPanel implements ActionListener{

	private static final long serialVersionUID = 1L;
	
	private JButton flag = new JButton("Checkpoint");
	private JButton quit = new JButton("Quit");
	private JSpinner spinner;
	private JFrame myFrame;
	
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
	    box1.add(flag);
	    box1.add(quit);
		
	    // Input field (spinner)
	    box1.add(new JLabel("Speed : "));
	    SpinnerModel model = new SpinnerNumberModel(50, 0, 100, 10);     
	    spinner = new JSpinner(model);
	    spinner.setMaximumSize(new Dimension( 50, 24 ));
	    box1.add(spinner);
	    
	    // Listeners
		flag.addActionListener(this);
		quit.addActionListener(this);
		spinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				// TODO
			}
	    });
	    
	    this.add(box1);
	  }
	
	/**
	 * Listeners onClick fonction
	 */
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();

		if (source == flag) {
			System.out.println("<---------------------------- CHECKPOINT ---------------------------->");
		} 
		else if (source == quit)
			myFrame.dispatchEvent(new WindowEvent(myFrame, WindowEvent.WINDOW_CLOSING));
    }
}

