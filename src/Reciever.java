import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Reciever {
	
	static Terminal t;
	
	public static void main(String[] argv) {
		t = new TerminalWrapper();
		String target;
		if (argv.length == 2) {
			target = argv[1];
		} else {
			t.write("Please enter an 'address:port' to connect to:");
			target = t.read();
		}
		if (!target.contains(":")) {
			t.write("colon missing! Quitting...");
		}
		String[] tPair = target.split(":",2);
		t.write("Trying to connect...");
		Socket s = openConnection(tPair);
		if (s == null) {return;} // failed to open socket.
		t.write("connected.");
		getObjects(s);
		ObjectTerminal.closeSock(s);
	}
	
	private static Socket openConnection(String[] tPair) {
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
	private static void getObjects(Socket s) {
		InputStream in;
		try {
			in = s.getInputStream();
		} catch (Exception e) {
			t.write("could not get inputStream");
			return;
		}
		while (true) {
			String res = getMessage(in);
			if (res == null) {break;}
			t.write("----------------------------------------------------------------");
			t.write(res);
		}
	}
	
	private static String getMessage(InputStream i) {
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
