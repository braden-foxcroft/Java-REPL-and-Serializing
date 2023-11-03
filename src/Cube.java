public class Cube extends Square {
	private float height;
	
	public Cube() {
		super();
		this.height = 0;
	}
	
	public Cube(int length, int width, float height) {
		this.length = length;
		this.width = width;
		this.height = height;
	}
	
	public float volume() {
		return length * width * height;
	}
	
	public String toString() {
		return "Cube(length=" + Integer.toString(length) +
				", width=" + Integer.toString(width) +
				", height=" + Float.toString(height) + ")";
	}
}
