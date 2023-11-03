
public class Linked {
	Object next = null;
	int val = 0;
	
	public Linked() {}
	
	public Linked(int val, Object o) {
		this.val = val;
		this.next = o;
	}
	
	public Linked(int val) {
		this.val = val;
	}
	
	public String toString() {
		return toStr(20);
	}
	
	public String toStr(int i) {
		if (i < 1) {
			return "...";
		} else if (next instanceof Linked) {
			return Integer.toString(val) + " -> " + ((Linked)next).toStr(i-1);
		} else {
			return Integer.toString(val) + " -> " + String.valueOf(next);
		}
	}
	
}
