import java.lang.reflect.*;
import java.util.HashMap;


public class ObjectTerminal {
	
	public static void main(String argv[]) throws ClassNotFoundException {
		TerminalWrapper tw = new TerminalWrapper();
		HashMap<String, Object> vars = new HashMap<>();
		// TODO network stuff
		mainTerminal(tw,vars);
	}
	
	public static void mainTerminal(Terminal t, HashMap<String,Object> vars) {
		while (true) {
			String c = t.read();
			if (c.equals("exit")) {
				break;
			} else if (c.equals("help")) {
				help(t);
			} else if (c.equals("help expr")) {
				exprHelp(t);
			} else if (c.matches(".*=.*")) {
				// assignment
				String[] cs = c.split(" = ", 2); // split halves
				handleAssignment(cs[0].strip(),cs[1].strip(),vars,t);
			} else if (c.startsWith("send ")) {
				// TODO 'send' command
				c = c.substring(5);
				handleSend(c, vars, t);
			} else {
				handleExpression(c, vars, t);
			}
		}
		t.write("done");
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
			String[] ress = res.split("\\[",2);
			ress[1] = ress[1].strip();
			if (!ress[1].matches(".*\\]$")) {
				throw new Exception("missing ']'");
			}
			ress[1] = ress[1].substring(0, ress[1].length()-1); // remove last char
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
			String[] parts = res.split("\\.", 2);
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
	public static Object doExpression(String expr, HashMap<String,Object> vars) throws Exception {
		if (expr.equals("true")) {
			return true;
		} else if (expr.equals("false")) {
			return false;
		} else if (isNum(expr)) {
			return Integer.valueOf(expr);
		} else if (expr.startsWith("f:")) {
			return Float.valueOf(expr);
		} else if (expr.startsWith("\"")) {
			// string
			if (!expr.endsWith("\"")) {
				throw new Exception("Bad string, no end quote");
			}
			expr = expr.substring(1, expr.length()-1); // remove quotes
			return expr; // A literal string.
		} else if (expr.startsWith("'")) {
			// char
			if (!expr.endsWith("'")) {
				throw new Exception("bad char: missing single quote");
			}
			if (expr.length() != 3) {
				throw new Exception("bad char: not 1 char");
			}
			return expr.charAt(1); // get the relevant char
		} else if (expr.startsWith("new ") && expr.endsWith("}")) {
			// Array constructor
			// TODO
			
		} else if (expr.startsWith("new ") && expr.endsWith(")")) {
			// Simple constructor
			// TODO
			
			
		} else if (expr.endsWith("()") && expr.contains(".")) {
			// method call. (Constructors already filtered)
			// split into var and method
			String[] exprs = expr.split("\\.", 2);
			exprs[0] = exprs[0].strip();
			exprs[1] = exprs[1].substring(0, exprs[1].length()-2); // remove brackets
			// Perform call
			if (!vars.containsKey(exprs[0])) {
				throw new Exception("var not found: " + exprs[0]);
			}
			Object var = vars.get(exprs[0]);
			Class<?> c = var.getClass();
			Method m = c.getMethod(exprs[1], new Class<?>[] {}); // no params
			m.setAccessible(true);
			Object res = m.invoke(var, new Object[] {}); // invoke
			return res;
		} else if (expr.contains(".")) {
			// Field
			String[] exprs = expr.split("\\.", 2);
			exprs[0] = exprs[0].strip();
			exprs[1] = exprs[1].strip();
			// Perform call
			if (!vars.containsKey(exprs[0])) {
				throw new Exception("var not found: " + exprs[0]);
			}
			Object var = vars.get(exprs[0]);
			Class<?> c = var.getClass();
			Field f = c.getField(exprs[1]);
			f.setAccessible(true);
			return f.get(var);
		}
		throw new Exception("TODO");
		// TODO
	}
	
	public static void exprHelp(Terminal t) {
		t.write("expr :: var");
		t.write("expr :: var.field");
		t.write("expr :: var.method()"); // g
		t.write("expr :: new Class(<expr>,<expr>...)");
		t.write("expr :: new Class[] {<expr>,<expr>...}");
		t.write("expr :: var[index]");
		t.write("expr :: <int>"); // g
		t.write("expr :: f:<float>"); // g
		t.write("expr :: \"<string>\""); // g
		t.write("expr :: '<char>'"); // g
		t.write("expr :: true"); // g
		t.write("expr :: false"); // g
		t.write("note: Due to lazy parsing, we cannot nest comma-separated lists. (split-on-comma)");
	}
	
	public static void help(Terminal t) {
		t.write("Assignments:");
		t.write("var = <expr>"); // g
		t.write("var.field = <expr>"); // g
		t.write("var[index] = <expr>");
		t.write("");
		t.write("To print an expression result:");
		t.write("<expr>"); // g
		t.write("");
		t.write("To send an object:");
		t.write("send var");
		t.write("");
		t.write("type 'help' to see this again, or 'help expr' to see the expression syntax");
	}
	
	private static boolean isNum(String s) {
		try {
			Integer.valueOf(s);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
