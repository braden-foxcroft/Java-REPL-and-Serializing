import java.io.BufferedReader;
import java.io.InputStreamReader;

// The real terminal.
public class TerminalWrapper implements Terminal {
	
	BufferedReader r;
	
	// Sets everything up
	public TerminalWrapper() {
		r = new BufferedReader(new InputStreamReader(System.in));
	}
	
	// Gets a line from the actual terminal.
	@Override
	public String read() {
		System.out.print("> ");
		try
		{
			return r.readLine();
		} catch (Exception e) {
			return "";
		}
	}
	
	// writes a string to the terminal
	@Override
	public void write(String s) {
		System.out.println(s);
	}
	
}
