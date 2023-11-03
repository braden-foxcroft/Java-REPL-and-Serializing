import java.util.HashMap;

import org.junit.jupiter.api.Test;

class ObjectTerminalTest {

	@Test
	void testExit() {
		String[] in = {"exit"};
		String[] out = {"done"};
		int[] ord = {0,1};
		HashMap<String,Object> vars = new HashMap<>();
		FalseTerminalForTesting t = new FalseTerminalForTesting(in, out, ord);
		ObjectTerminal.mainTerminal(t, vars);
		t.finish();
	}
	
	@Test
	void testAttributes() {
		String[] in = {"abc.length","abc.width","abc.height","exit"};
		String[] out = {"3","4","5.0","done"};
		int[] ord = {0,1,0,1,0,1,0,1};
		HashMap<String,Object> vars = new HashMap<>();
		vars.put("abc",new Cube(3,4,5));
		FalseTerminalForTesting t = new FalseTerminalForTesting(in, out, ord);
		ObjectTerminal.mainTerminal(t, vars);
		t.finish();
	}
	
	@Test
	void testMethods() {
		String[] in = {"abc.volume()","exit"};
		String[] out = {"60.0","done"};
		int[] ord = {0,1,0,1};
		HashMap<String,Object> vars = new HashMap<>();
		vars.put("abc",new Cube(3,4,5));
		FalseTerminalForTesting t = new FalseTerminalForTesting(in, out, ord);
		ObjectTerminal.mainTerminal(t, vars);
		t.finish();
	}
	
	@Test
	void testXMLCube() {
		String[] in = {"xml abc","exit"};
		String[] out = {"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
				+ "<serialized>\r\n"
				+ "  <object class=\"Cube\" id=\"0\">\r\n"
				+ "    <field name=\"height\" declaringclass=\"Cube\">\r\n"
				+ "      <value>5.0</value>\r\n"
				+ "    </field>\r\n"
				+ "    <field name=\"length\" declaringclass=\"Square\">\r\n"
				+ "      <value>3</value>\r\n"
				+ "    </field>\r\n"
				+ "    <field name=\"width\" declaringclass=\"Square\">\r\n"
				+ "      <value>4</value>\r\n"
				+ "    </field>\r\n"
				+ "  </object>\r\n"
				+ "</serialized>\r\n"
				+ "\r\n"
				+ "","done"};
		int[] ord = {0,1,0,1};
		HashMap<String,Object> vars = new HashMap<>();
		vars.put("abc",new Cube(3,4,5));
		FalseTerminalForTesting t = new FalseTerminalForTesting(in, out, ord);
		ObjectTerminal.mainTerminal(t, vars);
		t.finish();
	}
	
	@Test
	void testXMLLinked() {
		String[] in = {"xml abc","exit"};
		String[] out = {"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
				+ "<serialized>\r\n"
				+ "  <object class=\"Linked\" id=\"0\">\r\n"
				+ "    <field name=\"next\" declaringclass=\"Linked\">\r\n"
				+ "      <reference>1</reference>\r\n"
				+ "    </field>\r\n"
				+ "    <field name=\"val\" declaringclass=\"Linked\">\r\n"
				+ "      <value>3</value>\r\n"
				+ "    </field>\r\n"
				+ "  </object>\r\n"
				+ "  <object class=\"Linked\" id=\"1\">\r\n"
				+ "    <field name=\"next\" declaringclass=\"Linked\">\r\n"
				+ "      <reference>-1</reference>\r\n"
				+ "    </field>\r\n"
				+ "    <field name=\"val\" declaringclass=\"Linked\">\r\n"
				+ "      <value>4</value>\r\n"
				+ "    </field>\r\n"
				+ "  </object>\r\n"
				+ "</serialized>\r\n"
				+ "\r\n"
				+ "","done"};
		int[] ord = {0,1,0,1};
		HashMap<String,Object> vars = new HashMap<>();
		vars.put("abc",new Linked(3,new Linked(4)));
		FalseTerminalForTesting t = new FalseTerminalForTesting(in, out, ord);
		ObjectTerminal.mainTerminal(t, vars);
		t.finish();
	}
	
	@Test
	void testXMLCycle() {
		String[] in = {"xml abc","exit"};
		String[] out = {"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
				+ "<serialized>\r\n"
				+ "  <object class=\"Linked\" id=\"0\">\r\n"
				+ "    <field name=\"next\" declaringclass=\"Linked\">\r\n"
				+ "      <reference>1</reference>\r\n"
				+ "    </field>\r\n"
				+ "    <field name=\"val\" declaringclass=\"Linked\">\r\n"
				+ "      <value>3</value>\r\n"
				+ "    </field>\r\n"
				+ "  </object>\r\n"
				+ "  <object class=\"Linked\" id=\"1\">\r\n"
				+ "    <field name=\"next\" declaringclass=\"Linked\">\r\n"
				+ "      <reference>0</reference>\r\n"
				+ "    </field>\r\n"
				+ "    <field name=\"val\" declaringclass=\"Linked\">\r\n"
				+ "      <value>4</value>\r\n"
				+ "    </field>\r\n"
				+ "  </object>\r\n"
				+ "</serialized>\r\n"
				+ "\r\n"
				+ "","done"};
		int[] ord = {0,1,0,1};
		HashMap<String,Object> vars = new HashMap<>();
		Linked a = new Linked(3,new Linked(4));
		((Linked)a.next).next = a;
		vars.put("abc",a);
		FalseTerminalForTesting t = new FalseTerminalForTesting(in, out, ord);
		ObjectTerminal.mainTerminal(t, vars);
		t.finish();
	}
	
	
}
