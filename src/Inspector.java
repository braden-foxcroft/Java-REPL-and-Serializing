import java.lang.reflect.*;
import java.util.Vector;

class Inspector {
	
	// The method we are asked to implement.
	public static void inspect(Object obj, boolean recursive) {
		// The list of objects. A singleton if recursion is off.
		Vector<Object> objs = new Vector<Object>();
		// Add the original object
		objs.add(obj);
		// Add the rest recursively if needed
		if (recursive) {
			appendChildren(obj,objs);
		}
		for (Object o : objs) {
			System.out.println(objRep(o));
			System.out.println("\n"); // spacing.
		}
	}
	
	// Recursively appends the children of the object to the output list.
	static void appendChildren(Object obj, Vector<Object> objs) {
		// Array handling
		if (obj.getClass().isArray()) {
			if (obj.getClass().getComponentType().isPrimitive()) {
				return; // Skip, because it is an array of primitives
			}
			Object[] os = (Object[])obj;
			for (Object newO : os) {
				if (newO != null && !objs.contains(newO)) {
					objs.add(newO);
					appendChildren(newO, objs); // recurse
				}
			}
			return;
		}
		// search through the fields
		for (Field f : allFields(obj.getClass())) {
			// Skip if 'f' is not a complex object.
			if (f.getType().isPrimitive()) {
				continue;
			}
			try {
				f.trySetAccessible(); // For safety.
			} catch (Exception e) {} // For if we fail.
			Object newO;
			try {
				// If we fail to get this, it's not for lack of trying.
				 newO = f.get(obj);
			} catch (Exception e) {
				newO = null; // Cannot retrieve the object, so assume null.
			}
			// Add it if it is not already present.
			// Also recurse on its children.
			if (newO != null && !objs.contains(newO)) {
				objs.add(newO);
				appendChildren(newO, objs); // recurse
			}
		}
	}
	
	// Gets a list of every possible field, including those from parent classes.
	// (Operates recursively)
	static Vector<Field> allFields(Class<?> c) {
		Vector<Field> res = new Vector<Field>();
		if (c == null) {
			// For special cases.
			return res;
		}
		for (Field f : c.getDeclaredFields()) {
			res.add(f);
		}
		// Add superclass fields.
		res.addAll(allFields(c.getSuperclass()));
		// Add interface fields (static only, of course)
		for (Class<?> cI : c.getInterfaces()) {
			res.addAll(allFields(cI));
		}
		return res;
	}
	
	// Gets a list of every possible interface, including those from parent classes.
	// (Operates recursively)
	static Vector<Class<?>> allInterfaces(Class<?> c) {
		Vector<Class<?>> res = new Vector<Class<?>>();
		if (c == null) {
			// For special cases.
			return res;
		}
		// Add self, if needed.
		if (c.isInterface()) {
			res.add(c);
		}
		// Add superclass interfaces.
		res.addAll(allInterfaces(c.getSuperclass()));
		// Add interfaces recursively
		for (Class<?> cI : c.getInterfaces()) {
			res.addAll(allInterfaces(cI));
		}
		return res;
	}
	
	// Gets a list of every possible method, including those from parent classes.
	// (Operates recursively)
	static Vector<Method> allMethods(Class<?> c) {
		Vector<Method> res = new Vector<Method>();
		if (c == null) {
			// For special cases.
			return res;
		}
		for (Method m : c.getDeclaredMethods()) {
			res.add(m);
		}
		// Add superclass methods.
		res.addAll(allMethods(c.getSuperclass()));
		// ignore interface methods, since these are already implemented.
		return res;
	}
	
	// Gets a list of every possible field, including those from parent classes.
	// (Operates recursively)
	static Vector<Constructor<?>> allConstructors(Class<?> c) {
		Vector<Constructor<?>> res = new Vector<Constructor<?>>();
		if (c == null) {
			// For special cases.
			return res;
		}
		for (Constructor<?> cn : c.getDeclaredConstructors()) {
			res.add(cn);
		}
		// Add superclass constructors.
		res.addAll(allConstructors(c.getSuperclass()));
		// Interfaces provide no constructors
		return res;
	}
	
	// Gets a full string representation of an object.
	static String objRep(Object obj) {
		
		
		String res = ""; // The result
		// object tag
		res = res.concat(objectShortForm(obj)).concat("\n").concat(indent(1));
		// Class info
		res = res.concat(getClassInfo(obj.getClass())).concat("\n\n");
		// Array contents, if applicable
		if (obj.getClass().isArray()) {
			res = res.concat(indent(1)).concat("array contents = ").
					concat(niceValue(obj, false)).concat("\n\n");
		}
		// Method info
		res = res.concat(getMethodInfo(obj.getClass())).concat("\n\n");
		// Constructor info
		res = res.concat(getConstructorInfo(obj.getClass())).concat("\n\n");
		// Field info
		res = res.concat(getFieldInfo(obj)).concat("\n\n");
		
		
		return res;
	}
	
	// Gets a string representation of the class info of an object.
	static String getClassInfo(Class<?> c) {
		String res = "";
		// Append superclass
		if (c.getSuperclass() != null) {
			res = res.concat("extends ").
					concat(getNiceClassName(c.getSuperclass()));
		}
		// Get a list of interfaces
		res = res.concat(listInterfaces(c));
		
		return res;
	}
	
	// Gets all interface info.
	static String listInterfaces(Class<?> c) {
		String res = "";
		for (Class<?> i : allInterfaces(c)) {
			res = res.concat("\n").concat(indent(1)).concat("implements ").
					concat(getNiceClassName(i));
		}
		return res;
	}
	
	// Gets all method info
	static String getMethodInfo(Class<?> c) {
		String res = indent(1).concat("Methods:\n");
		for (Method m : allMethods(c)) {
			res = res.concat(indent(2)).concat(getNiceMethod(m)).concat("\n\n");
		}
		return res;
	}
	
	// Gets all Constructor info
	static String getConstructorInfo(Class<?> c) {
		String res = indent(1).concat("Constructors:\n");
		for (Constructor<?> cns : allConstructors(c)) {
			res = res.concat(indent(2)).concat(getNiceConstructor(cns)).concat("\n\n");
		}
		return res;
	}
	
	// Gets all method info
	static String getFieldInfo(Object o) {
		String res = indent(1).concat("Fields:\n");
		for (Field f : allFields(o.getClass())) {
			res = res.concat(indent(2)).concat(getNiceField(o,f)).concat("\n\n");
		}
		return res;
	}
	
	// A nice formatting of a method.
	// Assumes base indentation of 2.
	static String getNiceMethod(Method m) {
		String res = "";
		res = res.concat("// Declared by ").concat(getNiceClassName(m.getDeclaringClass())).concat("\n");
		res = res.concat(indent(2)).
				concat(Modifier.toString(m.getModifiers())).concat(" ").
				concat(getNiceClassName(m.getReturnType())).concat(" ").
				concat(m.getName()).concat("(").
				concat(listClasses(m.getParameterTypes())).concat(")");
		// Add exceptions if needed
		if (m.getExceptionTypes().length != 0) {
			res = res.concat(" throws ").concat(listClasses(m.getExceptionTypes()));
		}
		res = res.concat(";");
		
		return res;
	}
	
	// A nice formatting of a method.
	// Assumes base indentation of 2.
	static String getNiceConstructor(Constructor<?> m) {
		String res = "";
		res = res.
				concat(Modifier.toString(m.getModifiers())).concat(" ").
				concat(m.getName()).concat("(").
				concat(listClasses(m.getParameterTypes())).concat(");");
		
		return res;
	}
	
	// A nice formatting of a method.
	// Assumes base indentation of 2.
	static String getNiceField(Object o, Field f) {
		String res = "";
		res = res.concat("// Declared by ").concat(getNiceClassName(f.getDeclaringClass()))
				.concat("\n").concat(indent(2));
		res = res.
				concat(Modifier.toString(f.getModifiers())).concat(" ").
				concat(getNiceClassName(f.getType())).concat(" ").
				concat(f.getName()).concat(" = ").
				concat(getNiceFieldValue(o,f));
		
		return res;
	}
	
	// Gets the value from the field of an object. Returns nicely.
	static String getNiceFieldValue(Object o, Field f) {
		// Try to make it readable.
		try {
			f.trySetAccessible();
		} catch (Exception e) {}
		
		Object res;
		try {
			// Get results
			res = f.get(o);
			
		} catch (Exception e) {
			return "unknown";
		}
		return niceValue(res,f.getType().isPrimitive());
	}
	
	// prints a value nicely. Even works on lists.
	// The second parameter is for if it is a wrapped primitive.
	static String niceValue(Object o, boolean forcePrimitive) {
		if (o == null) {
			return "null";
		}
		try {
			return (String)o; // Guess that it is a string
		} catch (Exception e) {}
		if (o.getClass().isPrimitive() || forcePrimitive) {
			return o.toString();
		}
		// Array handling.
		if (o.getClass().isArray()) {
			Object[] os = (Object[]) o;
			String res = "[";
			for (int i = 0; i < os.length; i++) {
				res = res.concat(niceValue(os[i],os.getClass().getComponentType().isPrimitive()));
				if (i != os.length - 1) {
					res = res.concat(", ");
				}
			}
			res = res.concat("]");
			return res;
		}
		
		return objectShortForm(o); // The default for unknown object types
	}
	
	// Makes a nice list of classes, on a single line.
	static String listClasses(Class<?>[] cs) {
		String res = "";
		for (int i = 0; i < cs.length; i++) {
			Class<?> c = cs[i];
			// Add class name
			res = res.concat(getNiceClassName(c));
			// Add comma, unless we are at the last item.
			if (i + 1 != cs.length) {
				res = res.concat(", ");
			}
		}
		return res;
	}
	
	// Gets a class name, but in a pretty format.
	static String getNiceClassName(Class<?> c) {
		if (!c.isArray()) {
			return c.getName();
		}
		return getNiceClassName(c.getComponentType()).concat("[]");
	}
	
	// Returns a short string representation of the object.
	static private String objectShortForm(Object o) {
		try {
			// Try cast to string, so that we can show the contents.
			String s = (String)o;
			String name = getNiceClassName(o.getClass());
			String pointer = Integer.toString(o.hashCode());
			return "<".concat(name).concat(" \"").concat(s).concat("\" @").concat(pointer).concat(">");
		} catch (Exception e) {}
		
		String name = getNiceClassName(o.getClass());
		String pointer = Integer.toString(o.hashCode());
		return "<".concat(name).concat(" @").concat(pointer).concat(">");
	}
	
	// generates the desired depth of indentation.
	static private String indent(int depth) {
		String s = "";
		while (depth > 0) {
			s = s.concat("    ");
			depth--;
		}
		return s;
	}
}
