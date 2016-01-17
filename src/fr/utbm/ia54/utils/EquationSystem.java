package fr.utbm.ia54.utils;

public class EquationSystem {
	private Equation eqA;
	private Equation eqB;
	
	public EquationSystem() {
		
	}

	public Equation getEqA() {
		return eqA;
	}

	public void setEqA(Equation eqA) {
		this.eqA = eqA;
	}

	public Equation getEqB() {
		return eqB;
	}

	public void setEqB(Equation eqB) {
		this.eqB = eqB;
	}
	
	public void resolve() {
		System.out.println("Resolve IN");
		System.out.println(eqA);
		System.out.println(eqB);
		
		eqA.multiplyBy(-1./eqA.getCoeffB());	// B in function of A
		eqB.setCoeffA(eqB.getCoeffA()+eqA.getCoeffA()*eqB.getCoeffB());
		eqB.setConstant(eqB.getConstant()+eqA.getConstant()*eqB.getCoeffB());
		eqB.setCoeffB(0);
		eqB.multiplyBy(-1./eqB.getCoeffA());
		
		double finalA = eqB.getConstant();
		
		eqA.setConstant(eqA.getConstant()+finalA*eqA.getCoeffA());
		eqA.setCoeffA(0);
		double finalB = eqA.getConstant();
		System.out.println(eqB);
		System.out.println(eqA);
		System.out.println(finalA);
		System.out.println(finalB);
	}
	
}
