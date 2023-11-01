
public class GND {
	private GND l = null;
	private GND r = null;
	private int value = 0;
	private String name = "?";
	private boolean traversed = false;
	
	
	public GND() {}
	
	public GND(String n) {
		name = n;
	}
	
	public GND(String n, int v) {
		name = n;
		value = v;
	}
	
	public GND(GND lf, int v, GND rt, String n) {
		l = lf;
		value = v;
		r = rt;
		this.name = n;
	}
	
	public String toString() {
		if (traversed) {
			return name;
		}
		traversed = true;
		String result = "(" + String.valueOf(l) + "," + Integer.toString(value) + "," + String.valueOf(r) + ")";
		traversed = false;
		return result;
	}
	
}
