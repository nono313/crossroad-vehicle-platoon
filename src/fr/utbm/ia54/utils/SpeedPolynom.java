package fr.utbm.ia54.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpeedPolynom {
	
	private Map<Integer, Double> coefficients;
	
	public SpeedPolynom() {
		coefficients = new HashMap<>();
		
	}
	
	public void setCoeff(int at, double coeff) {
		coefficients.put(at, coeff);
	}
	
	public double getValue(double x) {
		double value = 0;
		for(int key : coefficients.keySet()) {
			value += coefficients.get(key)*Math.pow(x, key);
		}
		return value;
	}
	
	public void interpolation(double speed0, double speedFinal, double distance, double atFinal) {
		// t = 0
		setCoeff(0, speed0);
		// t = atFinal
		Equation e = new Equation();
		// Try to find a and b
		// a x² + b x + c, using previously found c and replacing x by atFinal
		e.setCoeffA(Math.pow(atFinal, 2));
		e.setCoeffB(atFinal);
		e.setConstant(speed0-speedFinal);
		System.out.println("Equation e : " + e);
		
		Equation prim = new Equation();
		prim.setCoeffA(Math.pow(atFinal, 3)/3.);
		prim.setCoeffB(Math.pow(atFinal, 2)/2.);
		prim.setConstant(speed0*atFinal);
		prim.setConstant(prim.getConstant()-distance);
		System.out.println("Equation prim : "+prim);
		
		EquationSystem system = new EquationSystem();
		system.setEqA(e);
		system.setEqB(prim);
		system.resolve();
		this.coefficients.put(1, system.getEqA().getConstant());	// final B coeff
		this.coefficients.put(2, system.getEqB().getConstant());	// final A coeff
		
		
		
	}

	@Override
	public String toString() {
		return "SpeedPolynom [coefficients=" + coefficients + "]";
	}
	
}
