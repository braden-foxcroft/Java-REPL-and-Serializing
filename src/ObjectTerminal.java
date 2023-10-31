import java.lang.reflect.*;
import java.util.HashMap;


// Valid commands:

// Assignments:
// var = <expr>
// var.field = <expr>
// var[index] = <expr>

// Prints
// <expr>

// Expressions:
// expr :: var
// expr :: var.field
// expr :: var.method(var,var...)
// expr :: Class(<expr>,<expr>)
// expr :: var[index]
// expr :: (ClassName) literal
// expr :: "string"
// expr :: 'char'
// expr :: false
// expr :: true


public class ObjectTerminal {
	
	public static void main(String argv[]) {
		TerminalWrapper tw = new TerminalWrapper();
		HashMap<String, Object> vars = new HashMap<>();
		// TODO 
		mainTerminal(tw,vars);
	}
	
	public static void mainTerminal(Terminal t, HashMap<String,Object> vars) {
		while (true) {
			String c = t.read();
			if (c.equals("help")) {
				help(t);
			} else if (c.equals("help expr")) {
				exprHelp(t);
			} else if (c.matches(".*=.*")) {
				// assignment
				String[] cs = c.split(" = ", 2); // split halves
				handleAssignment(cs[0].strip(),cs[1].strip(),vars);
			}
			
		}
	}
	
	// Handles a variable assignment. A wrapper for handling error-messages.
	public static void handleAssignment(String res, String expr, HashMap<String,Object> vars, Terminal t) {
		try {
			t.write(doAssignment(res, expr, vars));
		} catch (Exception e) {
			t.write(e.toString());
		}
	}
	
	public static void doAssignment() {
		
	}
	
	public static void help(Terminal t) {
		t.write("Assignments:");
		t.write("var = <expr>");
		t.write("var.field = <expr>");
		t.write("var[index] = <expr>");
		
		t.write("");

		t.write("To print an expression result:");
		t.write("<expr>");
		
		t.write("");
		
		t.write("type 'help' to see this again, or 'help expr' to see the expression syntax");
	}
	
	public static void exprHelp(Terminal t) {
		t.write("expr :: var");
		t.write("expr :: var.field");
		t.write("expr :: var.method(var,var...)");
		t.write("expr :: Class(<expr>,<expr>)");
		t.write("expr :: var[index]");
		t.write("expr :: (ClassName) literal");
		t.write("expr :: \"string\"");
		t.write("expr :: 'char'");
		t.write("expr :: false");
		t.write("expr :: true");
	}
}
