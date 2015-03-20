package fr.utbm.ia54.utils;

import javax.swing.ImageIcon;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import fr.utbm.ia54.consts.Const;
import fr.utbm.ia54.main.MainProgram;
import fr.utbm.ia54.utils.OrientedPoint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Florian & Alexis
 * Convert an XML file into a list of OrientedPoint.
 */
public class ReadXmlFile {
	
	private static final String TRAIN 		= "train";
	private static final String POINT 		= "point";
	private static final String X 			= "x";
	private static final String Y			= "y";
	private static final String ANGLE 		= "angle";
	private static final String BACKGROUND 	="background";
	
	/**
	 * @param xmlFile the file to parse
	 * @return list containing all path for all trains
	 */
	public List<List<OrientedPoint>> parse(File xmlFile) {

		List<List<OrientedPoint>> list = new ArrayList<List<OrientedPoint>>();
		int x, y;
		double angle;
		
	    try {
	 
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();
			
			// Set the background
			MainProgram.getCarPath().setBackground(new ImageIcon(Const.RESOURCES_DIR+"/"+doc.getDocumentElement().getAttribute(BACKGROUND)));
			
			// Get the trains
			NodeList trainList = doc.getElementsByTagName(TRAIN);

			for (int i = 0; i < trainList.getLength(); i++) {
				// Sublist containing the path of a train
				List<OrientedPoint> trainPath = new ArrayList<OrientedPoint>();
				Node train = trainList.item(i);

				if (train.getNodeType() == Node.ELEMENT_NODE) {
					// Get the points of the train
					Element eTrain = (Element) train;
					NodeList pointList = eTrain.getElementsByTagName(POINT);
					
					// For each point, create an OrientedPoint
					for (int j = 0; j < pointList.getLength(); j++) {
						 
						Node nNode = pointList.item(j);
				 
						if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				 
							Element eElement = (Element) nNode;
							x = Integer.valueOf(eElement.getElementsByTagName(X).item(0).getTextContent());
							y = Integer.valueOf(eElement.getElementsByTagName(Y).item(0).getTextContent());
							angle = Math.toRadians(Integer.valueOf(eElement.getElementsByTagName(ANGLE).item(0).getTextContent()));
							
							trainPath.add(new OrientedPoint(x,y,angle));
						}
					}
				}
				// Add the train path to the main list
				list.add(trainPath);
			}
	    } catch (ParserConfigurationException | SAXException | IOException e) {
	    	// File not found or Parse exception
	    	e.printStackTrace();
	    }
	    return list;
	}
}
