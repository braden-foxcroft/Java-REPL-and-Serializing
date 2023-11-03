import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Reciever {
	
	public static void main(String[] argv) {
		Terminal t = new TerminalWrapper();
		String target;
		if (argv.length == 2) {
			target = argv[1];
		} else {
			t.write("Please enter an 'address:port' to connect to:");
			target = t.read();
		}
		Socket s = makeConnection(target,t);
		if (s == null) {return;} // failed to open socket.
		t.write("connected.");
		getObjects(s,t);
		ObjectTerminal.closeSock(s);
	}
	
	public static Socket makeConnection(String target, Terminal t) {
		if (!target.contains(":")) {
			t.write("colon missing! Quitting...");
			return null;
		}
		// TODO fix use of static 't'. May be uninitialized.
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
	
	// Handles the receiving of objects and printing them out.
	private static void getObjects(Socket s, Terminal t) {
		InputStream in;
		try {
			in = s.getInputStream();
		} catch (Exception e) {
			t.write("could not get inputStream");
			return;
		}
		while (true) {
			String res = getMessage(in,t);
			if (res == null) {break;}
			t.write("----------------------------------------------------------------");
			t.write(res);
		}
	}
	
	private static String getMessage(InputStream i, Terminal t) {
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
	
}
