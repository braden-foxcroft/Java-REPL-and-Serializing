import java.lang.reflect.*;
import java.util.HashMap;


public class ObjectTerminal {
	
	public static void main(String argv[]) {
		TerminalWrapper tw = new TerminalWrapper();
		HashMap<String, Object> vars = new HashMap<>();
		// TODO network stuff
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
				handleAssignment(cs[0].strip(),cs[1].strip(),vars,t);
			} else if (c.startsWith("send ")) {
				// TODO 'send' command
			} else {
				handleExpression(c, vars, t);
			}
		}
	}
	
	// Handles a variable assignment. A wrapper for handling error-messages.
	public static void handleAssignment(String res, String expr, HashMap<String,Object> vars, Terminal t) {
		try {
			t.write(doAssignment(res, expr, vars).toString());
		} catch (Exception e) {
			t.write(e.toString());
		}
	}
	
	// Handles an expression command. A wrapper for handling error-messages.
	public static void handleExpression(String expr, HashMap<String,Object> vars, Terminal t) {
		try {
			t.write(doExpression(expr, vars).toString());
		} catch (Exception e) {
			t.write(e.toString());
		}
	}
	
	// Sends a thing over the server
	public static void handleSend(String toSend, HashMap<String,Object> vars, Terminal t) {
		// TODO sending behavior
		t.write("TODO");
	}
	
	// Actually performs an assignment. May throw exceptions, but nothing fatal.
	// Returns the result of the assignment
	public static Object doAssignment(String res, String expr, HashMap<String,Object> vars) throws Exception {
		if (res.contains("[") || res.contains("]")) {
			// an array
			// TODO
			
			// Some string parsing.
			String[] ress = res.split("[",2);
			ress[1] = ress[1].strip();
			if (!ress[1].matches(".*\\]$")) {
				throw new Exception("missing ']'");
			}
			ress[1] = ress[1].substring(0, ress[1].length()-2); // remove last char
			ress[0] = ress[0].strip();
			// Check for var
			if (!vars.containsKey(ress[0])) {
				throw new Exception("var not found: " + ress[1]);
			}
			Object arr = vars.get(ress[0]); // The array
			if (!arr.getClass().isArray()) {
				throw new Exception("array access on non-array!");
			}
			Object val = doExpression(expr, vars);
			Array.set(arr, Integer.valueOf(ress[1]), val);
			return val;
		} else if (res.contains(".")) {
			// a field
			String[] parts = res.split(".", 2);
			if (vars.containsKey(parts[0].strip())) {
				Object outp = vars.get(parts[0].strip());
				Class<?> c = outp.getClass();
				Field f = c.getField(parts[1]); // might fail.
				Object val = doExpression(expr, vars);
				f.set(outp, val); // Performs assignment.
				return val;
			} else {
				throw new Exception("Cannot find var: " + parts[0].strip());
			}
		} else {
			// a variable
			Object val = doExpression(expr,vars);
			vars.put(res, val);
			return val;
		}
		// throw new Exception("this shouldn't happen...");
	}
	
	
	// Performs an expression. May throw exceptions.
	// Returns the result as an Object.
	public static Object doExpression(String expr, HashMap<String,Object> vars) {
		// TODO
	}
	
	public static void exprHelp(Terminal t) {
		t.write("expr :: var");
		t.write("expr :: var.field");
		t.write("expr :: var.method()");
		t.write("expr :: new Class(<expr>,<expr>...)");
		t.write("expr :: new ClassName[] {<expr>,<expr>...}");
		t.write("expr :: var[index]");
		t.write("expr :: <int>");
		t.write("expr :: f<float>");
		t.write("expr :: \"<string>\"");
		t.write("expr :: '<char>'");
		t.write("expr :: true");
		t.write("expr :: false");
		t.write("note: Due to lazy parsing, we cannot nest comma-separated lists. (split-on-comma)");
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
		t.write("To send an object:");
		t.write("send var");
		t.write("");
		t.write("type 'help' to see this again, or 'help expr' to see the expression syntax");
	}
}
