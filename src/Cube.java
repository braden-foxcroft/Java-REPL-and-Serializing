public class Cube {
	private int length;
	private int width;
	private float height;
	
	public Cube() {
		this.length = 0;
		this.width = 0;
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
	
}
