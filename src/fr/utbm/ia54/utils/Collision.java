package fr.utbm.ia54.utils;

public class Collision {
	private String a;
	private String b;
	
	public Collision(String a, String b) {
		super();
		this.a = a;
		this.b = b;
	}
	
	public String getA() {
		return this.a;
	}
	public void setA(String a) {
		this.a = a;
	}
	public String getB() {
		return this.b;
	}
	public void setB(String b) {
		this.b = b;
	}
	
	@Override
	public int hashCode() {
		int result = 1;
		result += ((this.a == null) ? 0 : this.a.hashCode());
		result += ((this.b == null) ? 0 : this.b.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Collision other = (Collision) obj;
		if(this.a.equals(other.a) && this.b.equals(other.b))
			return true;
		if(this.a.equals(other.b) && this.b.equals(other.a))
			return true;
		
		return false;
	}

	@Override
	public String toString() {
		return "Collision [a=" + this.a + ", b=" + this.b + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	
	
}
