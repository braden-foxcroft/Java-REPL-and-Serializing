import static org.junit.jupiter.api.Assertions.*;

// A terminal for unit-testing. Pretends to provide inputs and outputs, but doesn't actually.
public class FalseTerminalForTesting implements Terminal {
	String[] inputs; // The list of things to read from the terminal
	String[] outputs; // The list of things to print to the terminal
	int[] order; // The order to receive inputs and outputs. Input=0, Output=1
	
	// How far through each list you are.
	int inPos = 0;
	int outPos = 0;
	int ordPos = 0;
	Boolean conNext = false; // Checks if to expect a "$ " before a read.
	
	
	public FalseTerminalForTesting(String[] inputs, String[] outputs, int[] order) {
		this.inputs = inputs;
		this.outputs = outputs;
		this.order = order;
		// Make sure the inputs are well-formed.
		// 2 items per input, 1 per output.
		assert(inputs.length + outputs.length == order.length);
		conNext = true;
	}
	
	@Override
	public String read() {
		// Make sure the action is expected.
		if (ordPos >= order.length) {
			fail("Tried to read when terminal should have closed.");
		}
		if (order[ordPos] != 0) {
			fail("Tried to read from terminal when write expected.");
		}
		// get result
		String res = inputs[inPos];
		// update pointers
		inPos += 1;
		ordPos += 1;
		conNext = true;
		return res;
	}

	@Override
	public void write(String s) {
		// Make sure the action is expected.
		if (ordPos >= order.length) {
			fail("Tried to write when terminal should have closed.");
		}
		if (order[ordPos] != 1) {
			// write when read expected
			if (conNext && s.compareTo("$ ") == 0) {
				// For printing "$ " before a read.
				conNext = false;
				return;
			}
			fail("Tried to write to terminal when read expected:\n\t".concat(s));
		}
		// get result
		String res = outputs[outPos];
		// update pointers
		outPos += 1;
		ordPos += 1;
		conNext = true;
		// Make sure result is correct
		assertEquals(res, s);
	}
	
	// Makes sure no inputs/outputs were ignored.
	public void finish() {
		assertEquals(order.length, ordPos, "Finished before the end!");
	}
	
}
