import java.util.Vector;

public class GND {
	private GND l = null;
	private GND r = null;
	private int value = 0;
	
	
	public GND() {}
	
	public GND(int v) {
		value = v;
	}
	
	public GND(GND lf, int v, GND rt) {
		l = lf;
		value = v;
		r = rt;
	}
	
	public String toString() {
		Vector<GND> all = new Vector<>();
		all.add(this);
		for (int i = 0; i < all.size(); i++) {
			addSet(all,all.get(i).l);
			addSet(all,all.get(i).r);
		}
		String res = "";
		for (int i = 0; i < all.size(); i++) {
			GND g = all.get(i);
			res = res + lookup(i) +
					": " + lookup(all.indexOf(g.l)) +
					", " + Integer.toString(g.value) +
					", " + lookup(all.indexOf(g.r)) + "\n";
		}
		return res.substring(0, res.length() - 1);
	}
	
	// Gives a name to the node.
	private String lookup(int i) {
		if (i == -1) {return "null";};
		if (i < 26) {
			return "abcdefghijklmnopqrstuvwxyz".charAt(i) + "";
		}
		return Integer.toString(i);
	}
	
	// Adds an item if it isn't already present.
	private void addSet(Vector<GND> all, GND n) {
		if (n == null) {return;}
		if (all.contains(n)) {return;}
		all.add(n);
	}
	
}
