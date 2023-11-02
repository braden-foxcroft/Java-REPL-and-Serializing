
public class Square {

	protected int length;
	protected int width;
	
	public Square(int length, int width) {
		super();
		this.length = length;
		this.width = width;
	}
	
	public Square() {
		this.length = 0;
		this.width = 0;
	}
	
	public int area() {
		return length * width;
	}
	
	public String info() {
		return "Square(length=" + Integer.toString(length) +
				", width=" + Integer.toString(width) + ")";
	}

}