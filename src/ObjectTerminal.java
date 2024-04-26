import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;


public class ObjectTerminal {
	
	private static ServerSocket serverSock = null; // the server socket
	private static Socket sock = null; // The client-communication socket
	private static String lastTarget = "noTarget";
	
	public static void main(String argv[]) throws ClassNotFoundException {
		TerminalWrapper tw = new TerminalWrapper();
		HashMap<String, Object> vars = new HashMap<>();
		mainTerminal(tw,vars);
		if (serverSock != null) {serverSock = closeSSock(serverSock);}
		if (sock != null) {sock = closeSock(sock);}
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
				c = c.substring(5);
				handleSend(c, vars, t);
			} else if (c.startsWith("recv ")) {
				handleRecv(c.substring(5), vars, t);
			} else if (c.startsWith("xml ")) {
				c = c.substring(4);
				handleXML(c, vars, t);
			} else if (c.equals("sock")) {
				handleServ(t); // Make a server socket, show all socket info
			} else if (c.equals("acc")) {
				handleAcc(t); // Accept a connection from a server socket.
			} else if (c.startsWith("conn ")) {
				handleConn(t,c.substring(5)); // Accept a connection from a server socket.
			} else if (c.equals("conn")) {
				handleConn(t,""); // Accept a connection from a server socket. (use last address)
			} else if (c.equals("break")) {
				handleBreak(t); // Accept a connection from a server socket. (use last address)
			} else if (c.equals("listen")) {
				handleListen(t,vars); // Accept a connection from a server socket.
			} else if (c.startsWith("info ")) {
				c = c.substring(5);
				handleInfo(c, vars, t);
			} else {
				handleExpression(c, vars, t);
			}
		}
		t.write("done");
	}
	
	// Handles a variable assignment. A wrapper for handling error-messages.
	public static void handleAssignment(String res, String expr, HashMap<String,Object> vars, Terminal t) {
		try {
			doAssignment(res, expr, vars);
		} catch (Exception e) {
			t.write(e.toString());
		}
	}
	
	// Handles an expression command. A wrapper for handling error-messages.
	public static void handleExpression(String expr, HashMap<String,Object> vars, Terminal t) {
		try {
			t.write(myToString(doExpression(expr, vars)));
		} catch (Exception e) {
			t.write(e.toString());
		}
	}
	
	// Sends a thing over the server
	public static void handleSend(String toSend, HashMap<String,Object> vars, Terminal t) {
		if (sock == null) {
			t.write("no client socket open! Run 'acc' first!");
			return;
		}
		String xml;
		try {
			xml = doXML(toSend,vars,t);
		} catch (Exception e) {
			t.write(e.getMessage());
			return;
		}
		if (!putMessage(sock, xml)) {
			t.write("socket crashed.");
			sock = closeSock(sock);
		} else {
			t.write(xml);
		}
	}
	
	// Receive a value from a socket into a variable.
	// Returns the success status.
	public static boolean handleRecv(String toRecv, HashMap<String,Object> vars, Terminal t) {
		if (sock == null) {
			t.write("no client socket open! Run 'acc' first!");
			return false;
		}
		String xml;
		try {
			xml = getMessage(sock.getInputStream(), t);
		} catch (Exception e) {
			t.write("socket crashed:");
			t.write(e.toString());
			sock = closeSock(sock);
			return false;
		}
		if (xml == null) {
			sock = closeSock(sock);
			return false;
		} // Socket closed.
		Document xmlDoc;
		try {
			ByteArrayInputStream iStr = new ByteArrayInputStream(xml.getBytes());
			xmlDoc = new SAXBuilder().build(iStr);
		} catch (Exception e) {
			t.write("XML parsing error:");
			t.write(e.toString());
			return false;
		}
		Object result;
		try {
			result = new Deserializer().deserialize(xmlDoc);
		} catch (Exception e) {
			t.write("Object construction error:");
			t.write(e.toString());
			return false;
		}
		vars.put(toRecv, result); // store to the variable.
		try {
			t.write(myToString(result));
		} catch (Exception e) {
			t.write("cannot print object:");
			t.write(e.toString());
		}
		return true;
	}
	
	public static void handleXML(String varName, HashMap<String,Object> vars, Terminal t) {
		try {
			t.write(doXML(varName,vars,t));
		} catch (Exception e) {
			t.write(e.getMessage());
			e.printStackTrace();
		}
	}
	
	// Repeatedly receives objects without waiting for user input.
	public static void handleListen(Terminal t, HashMap<String,Object> vars) {
		while (true) {
			t.write("----------------------------------------------------------");
			if (!handleRecv("", vars, t)) {break;}
			handleInfo("", vars, t);
		}
	}
	
	// Make a server if needed, and print its address.
	public static void handleServ(Terminal t) {
		
		// Make a server
		if (serverSock == null) {
			try {
				serverSock = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
			} catch (Exception e) {
				t.write("could not make server");
				return;
			}
			t.write("Made a server socket. Try any of the below addresses:");
			
		} else {
			t.write("Server already exists:");
		}
		t.write("     " + serverSock.getInetAddress().getHostAddress() + ":" + Integer.toString(serverSock.getLocalPort()));
		t.write("     " + serverSock.getInetAddress().getCanonicalHostName() + ":" + Integer.toString(serverSock.getLocalPort()));
		t.write("     " + serverSock.getInetAddress().getHostName() + ":" + Integer.toString(serverSock.getLocalPort()));
		t.write("");
		
		// Check the state of the client connection.
		if (sock != null) {
			if (sock.isClosed() || !sock.isConnected()) {
				sock = null;
				t.write("Client socket just crashed");
			} else {
				t.write("Client connected.");
			}
		} else {
			t.write("No client. Use 'acc' to accept connection.");
		}
	}
	
	// Recieve a connection from the server socket.
	public static void handleAcc(Terminal t) {
		if (serverSock == null) {
			t.write("no server! Call 'sock' first!");
			return;
		}
		if (sock != null) {
			t.write("closing existing connection first.");
			sock = closeSock(sock);
		}
		try {
			t.write("waiting for connection...");
			sock = serverSock.accept();
		} catch (Exception e) {
			t.write("Connection failed:");
			t.write(e.toString());
			return;
		}
		t.write("Connection established!");
		return;
	}
	
	// Recieve a connection from the server socket.
	public static void handleBreak(Terminal t) {
		if (sock != null) {
			t.write("closing existing connection.");
			sock = closeSock(sock);
		} else {
			t.write("no connection to close!");
		}
	}
	
	// Create a connection to another terminal
	public static void handleConn(Terminal t, String target) {
		if (sock != null) {
			t.write("closing existing connection first.");
			sock = closeSock(sock);
		}
		if (target == "") {target = lastTarget;} // Re-use last target
		sock = makeConnection(target, t);
		if (sock == null) {return;}
		lastTarget = target;
		t.write("Connection established!");
		return;
	}
	
	
	public static void handleInfo(String name, HashMap<String,Object> vars, Terminal t) {
		if (!vars.containsKey(name)) {
			t.write("Cannot find var: " + name);
			return;
		}
		Object obj = vars.get(name);
		// Print the result:
		try {
			Inspector.inspect(obj, t);
		} catch (Exception e) {
			t.write("Could not inspect:");
			t.write(e.toString());
		}
	}
	
	private static String doXML(String varName, HashMap<String, Object> vars, Terminal t) throws Exception {
		// Why is this reflective?
		// So that, if serializer can't find its dependencies,
		// we can still run the main code.
		if (!vars.containsKey(varName)) {
			throw new Exception("Could not find var: " + varName);
		}
		Object var = vars.get(varName);
		Class<?> c;
		try {
			c = Class.forName("Serializer");
		} catch (Throwable th) {
			throw new Exception("For whatever reason, serializer couldn't load.");
		}
		Method objToXML = c.getMethod("objToXML", new Class<?>[] {Object.class});
		return (String)objToXML.invoke(null,new Object[] {var});
	}
	
	// Actually performs an assignment. May throw exceptions, but nothing fatal.
	// Returns the result of the assignment
	public static Object doAssignment(String res, String expr, HashMap<String,Object> vars) throws Exception {
		if (res.contains("[") || res.contains("]")) {
			// an array
			
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
				Field f = myGetField(c,parts[1]); // might fail.
				f.setAccessible(true);
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
		expr = expr.strip();
		if (expr.equals("true")) {
			return true;
		} else if (expr.equals("false")) {
			return false;
		} else if (expr.equals("null")) {
			return null;
		} else if (isNum(expr)) {
			return Integer.valueOf(expr);
		} else if (expr.startsWith("f:")) {
			return Float.valueOf(expr.substring(2));
		} else if (expr.startsWith("b:")) {
			return Byte.valueOf(expr.substring(2));
		} else if (expr.startsWith("s:")) {
			return Short.valueOf(expr.substring(2));
		} else if (expr.startsWith("l:")) {
			return Long.valueOf(expr.substring(2));
		} else if (expr.startsWith("d:")) {
			return Double.valueOf(expr.substring(2));
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
			expr = expr.substring(4); // Remove 'new '
			String[] exprs = expr.split(" ", 2);
			exprs[0].strip();
			exprs[1] = exprs[1].substring(1,exprs[1].length()-1); // remove '{', '}'.
			if (!exprs[0].endsWith("[]")) {
				throw new Exception("Array initialization missing '[]'");
			}
			exprs[0] = exprs[0].substring(0, exprs[0].length()-2);
			String[] params;
			if (exprs[1] == "") {
				params = new String[0]; // no params
			} else {
				params = exprs[1].split(",");
			}
			Object arr = makeArrayOf(exprs[0],params.length);
			
			for (int i = 0; i < params.length; i++) {
				Object o = doExpression(params[i], vars);
				Array.set(arr, i, o); // Set value appropriately
			}
			return arr;
		} else if (expr.startsWith("new ") && expr.endsWith(")")) {
			expr = expr.substring(4); // Remove 'new '
			String[] exprs = expr.split("\\(", 2);
			exprs[0].strip();
			exprs[1] = exprs[1].substring(0,exprs[1].length()-1); // remove close-bracket.
			String[] params;
			if (exprs[1] == "") {
				params = new String[0]; // no params
			} else {
				params = exprs[1].split(",");
			}
			Object[] pObjs = new Object[params.length];
			for (int i = 0; i < params.length; i++) {
				Object o = doExpression(params[i], vars); // turn to object
				pObjs[i] = o;
			}
			Class<?> c = Class.forName(exprs[0]);
			for (Constructor<?> cn : c.getConstructors()) {
				try {
					cn.setAccessible(true);
					Object o = cn.newInstance(pObjs);
					return o;
				} catch (Exception e) {
					continue; // Wrong constructor, try again
				}
			}
			throw new Exception("Could not find valid constructor!");
		} else if (expr.endsWith(")") && expr.contains(".")) {
			// method call. (Constructors already filtered)
			// split into var and method
			String[] exprs = expr.split("\\(", 2);
			String varAndFunc = exprs[0];
			String params = exprs[1].substring(0, exprs[1].length()-1); // remove ')'
			String[] varAndFuncList = varAndFunc.split("\\.");
			String varName = varAndFuncList[0];
			String funcName = varAndFuncList[1];
			// Find variable
			if (!vars.containsKey(varName)) {
				throw new Exception("var not found: " + varName);
			}
			Object var = vars.get(varName);
			Class<?> c = var.getClass();
			// Get param count and list.
			Object[] objParams;
			String[] paramList;
			if (params.equals("")) {
				paramList = new String[0];
			} else {
				paramList = params.split(",");
			}
			objParams = new Object[paramList.length];
			for (int i = 0; i < paramList.length; i++) {
				objParams[i] = doExpression(paramList[i].strip(), vars);
			}
			for (Method m : allMethods(c)) {
				try {
					if (!m.getName().equals(funcName)) {continue;}
					m.setAccessible(true); // might fail
					return m.invoke(var, objParams); // invoke the method. (Might have wrong param types)
				} catch (InvocationTargetException e) {
					// Found the method, but got an exception
					throw e; // Throw a conventional exception
				} catch (Exception e) {continue;}
			}
			throw new Exception("Cannot find method for: " + expr);
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
			Field f = myGetField(c,exprs[1]);
			f.setAccessible(true);
			return f.get(var);
		} else if (expr.endsWith("]") && expr.contains("[")) {
			// array index.
			// Split on '['
			String[] exprs = expr.substring(0,expr.length()-1).split("\\[",2);
			// Perform call
			if (!vars.containsKey(exprs[0])) {
				throw new Exception("var not found: " + exprs[0]);
			}
			Object var = vars.get(exprs[0]);
			int i = Integer.valueOf(exprs[1]);
			return Array.get(var, i);
		} else {
			if (!vars.containsKey(expr)) {
				throw new Exception("var not found: " + expr);
			}
			return vars.get(expr);
		}
	}
	
	public static void exprHelp(Terminal t) {
		t.write("expr :: var");
		t.write("expr :: var.field");
		t.write("expr :: var.method()");
		t.write("expr :: var.method(<expr>,<expr>,<expr>)");
		t.write("expr :: new Class(<expr>,<expr>...)");
		t.write("expr :: new Class[] {<expr>,<expr>...}");
		t.write("expr :: var[index]");
		t.write("expr :: <int>");
		t.write("expr :: f:<float>");
		t.write("expr :: b:<byte>");
		t.write("expr :: s:<short>");
		t.write("expr :: s:<long>");
		t.write("expr :: d:<double>");
		t.write("expr :: \"<string>\"");
		t.write("expr :: '<char>'");
		t.write("expr :: true");
		t.write("expr :: false");
		t.write("expr :: null");
		t.write("note: Due to overly-simplified parsing, we cannot nest comma-separated lists.");
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
		t.write("To print the socket info: (creates a server socket if needed)");
		t.write("    sock");
		t.write("To accept a socket connection:");
		t.write("    acc");
		t.write("To initiate a socket connection with another terminal:");
		t.write("    conn <target>");
		t.write("    conn             // re-uses last target");
		t.write("To break a connection:");
		t.write("    break");
		t.write("To send an object:");
		t.write("    send var");
		t.write("To recieve object:");
		t.write("    recv var");
		t.write("To recieve objects passively and print their info until the connection closes:");
		t.write("    listen");
		t.write("To print the xml for an object:");
		t.write("   xml var");
		t.write("To inspect an object:");
		t.write("   info var");
		t.write("");
		t.write("type 'help' to see this again, or 'help expr' to see the expression syntax");
		t.write("type 'exit' to exit.");
	}
	
	// Gets a field, even if private or from parent.
	private static Field myGetField(Class<?> c, String name) throws Exception {
		try {
			return c.getDeclaredField(name);
		} catch (Exception e) {
			if (c.getSuperclass() != null) {
				return myGetField(c.getSuperclass(), name);
			} else {
				throw new Exception("Could not find field: '" + name + "'");
			}
		}
	}
	
	// Get all fields. Used externally, by serializer and info.
	public static Vector<Field> allFields(Class<?> c) {
		Vector<Field> result = new Vector<>();
		for (Field m : c.getDeclaredFields()) {
			result.add(m);
		}
		if (c.getSuperclass() != null) {
			result.addAll(allFields(c.getSuperclass()));
		}
		return result;
	}
	
	// Gets a field, even if private or from parent.
	/*
	private static Method myGetMethod(Class<?> c, String name, Class<?>[] args) throws Exception {
		try {
			return c.getDeclaredMethod(name,args);
		} catch (Exception e) {
			if (c.getSuperclass() != null) {
				return myGetMethod(c.getSuperclass(), name, args);
			} else {
				throw new Exception("Could not get method: '" + name + "'");
			}
		}
	}
	*/
	
	private static Vector<Method> allMethods(Class<?> c) {
		Vector<Method> result = new Vector<>();
		for (Method m : c.getDeclaredMethods()) {
			result.add(m);
		}
		if (c.getSuperclass() != null) {
			result.addAll(allMethods(c.getSuperclass()));
		}
		return result;
	}
	
	// Makes an array with the given element type and length
	private static Object makeArrayOf(String s, int length) throws Exception {
		// simple case
		if (!s.endsWith("[]")) {
			Class<?> c;
			if (s.equals("int")) {
				c = int.class;
			} else if (s.equals("byte")) { 
				c = byte.class;
			} else if (s.equals("short")) { 
				c = short.class;
			} else if (s.equals("long")) { 
				c = long.class;
			} else if (s.equals("float")) { 
				c = float.class;
			} else if (s.equals("double")) { 
				c = double.class;
			} else if (s.equals("boolean")) { 
				c = boolean.class;
			} else if (s.equals("char")) { 
				c = char.class;
			} else {
				c = Class.forName(s);
			}
			return Array.newInstance(c, length);
		}
		String trueName = "";
		while (s.endsWith("[]")) {
			trueName = trueName + "[";
			s = s.substring(0,s.length()-2);
		}
		trueName = trueName + toArrayNaming(s); // add whatever needs to be appended.
		return Array.newInstance(Class.forName(trueName), length);
	}
	
	public static String toArrayNaming(String s) {
		if (s.equals("boolean")) {
			return "Z";
		} else if (s.equals("byte")) {
			return "B";
		} else if (s.equals("char")) {
			return "C";
		} else if (s.equals("double")) {
			return "D";
		} else if (s.equals("float")) {
			return "F";
		} else if (s.equals("int")) {
			return "I";
		} else if (s.equals("long")) {
			return "J";
		} else if (s.equals("short")) {
			return "S";
		} else {
			return "L" + s;
		}
	}
	
	// A nicer toString for arrays
	public static String myToString(Object o) throws Exception {
		try {
			String s = (String)o;
			return "\"" + s + "\"";
		} catch (Exception e) {}
		if (o == null) {
			return "null";
		} else if (o.getClass().isArray() && !o.getClass().getComponentType().isPrimitive()) {
			return Arrays.toString((Object[])o);
		} else if (o.getClass().isArray()) {
			Method m = Arrays.class.getDeclaredMethod("toString",
					new Class<?>[] {o.getClass()});
			return (String)m.invoke(null, new Object[] {o});
		} else {
			return o.toString();
		}
	}
	
	public static Socket makeConnection(String target, Terminal t) {
		if (!target.contains(":")) {
			t.write("colon missing! Cannot connect.");
			return null;
		}
		String[] tPair = target.split(":",2);
		t.write("Trying to connect...");
		Socket s = openConnectionFromPair(tPair,t);
		return s;
	}
	
	private static Socket openConnectionFromPair(String[] tPair, Terminal t) {
		try {
			Socket s = new Socket(tPair[0],Integer.valueOf(tPair[1]));
			return s;
		} catch (Exception e) {
			t.write("could not open socket:");
			t.write(e.toString());
			return null;
		}
	}
	
	public static String getMessage(InputStream i, Terminal t) {
		int len;
		try {
			byte[] bLen = i.readNBytes(4);
			len = ByteBuffer.wrap(bLen).getInt();
		} catch (Exception e) {
			t.write("Socket closed.");
			return null; // This means the socket closed normally.
		}
		try {
			byte[] bStr = i.readNBytes(len);
			return new String(bStr);
		} catch (Exception e) {
			t.write("Socket crashed mid-message.");
			return null; // Failed.
		}
	}
	
	// Safely close a socket, returning null
	public static Socket closeSock(Socket s) {
		if (sock != null) {
			try {sock.close();} catch (Exception e) {};
			sock = null;
		}
		return null;
	}
	
	// Safely close a server socket, returning null
	private static ServerSocket closeSSock(ServerSocket s) {
		if (sock != null) {
			try {sock.close();} catch (Exception e) {};
			sock = null;
		}
		return null;
	}
	
	// Sends a message on a socket.
	private static boolean putMessage(Socket s, String message) {
		try {
			OutputStream o = s.getOutputStream();
			byte[] bMes = message.getBytes();
			int len = bMes.length;
			byte[] bLen = ByteBuffer.allocate(4).putInt(len).array();
			o.write(bLen); // write the length
			o.write(bMes); // write the message
			return true;
		} catch (Exception e) {
			return false; // Failed.
		}
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
