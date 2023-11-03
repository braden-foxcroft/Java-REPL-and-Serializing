import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import org.jdom.Document;
import org.jdom.Element;

public class Deserializer {
	
	
	// Takes a document and deserializes it to a collection of objects.
	// Returns 'null' if something goes wrong.
	public Object deserialize(Document document) {
		try {
			return trueDeserialize(document); // To allow for unhandled errors internally.
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private Object trueDeserialize(Document document) throws Exception {
		Vector<Object> objs = new Vector<>();
		Element root = (Element)document.getContent(0);
		// Make objects, but doesn't populate them.
		// (except Strings, lists, and primitive arrays)
		generateBlankObjects(objs,root);
		
		
		
	}
	
	// Generates blank objects. Doesn't populate them, except in the case of Strings, Integers,
	// and arrays of primitives
	private void generateBlankObjects(Vector<Object> objs, Element root) throws Exception {
		Iterator<?> i = root.getChildren().iterator();
		while (i.hasNext()) {
			Element e = (Element)i.next();
			// Make object for this element. We do pointers on a second walk-through.
			Class<?> c = Class.forName(e.getAttributeValue("class"));
			Object obj; // The object to store to the vector.
			if (c.isArray()) {
				// array
				// Make the array of the right type.
				obj = Array.newInstance(c.getComponentType(), e.getChildren().size());
				if (c.getComponentType().isPrimitive()) {
					populatePrimitiveArray(c.getComponentType(), obj, e.getChildren().iterator());
				}
			} else if (String.class.isAssignableFrom(c)) {
				// string
				obj = e.getChild("value").getText(); // Gets the string contents directly
			} else if (Integer.class.isAssignableFrom(c)) {
				// Integer
				obj = Integer.valueOf(e.getChild("value").getText());
			} else {
				// Normal object or collection.
				obj = c.getConstructor(new Class<?>[] {}).newInstance(new Object[] {});
			}
			objs.add(obj);
		}
	}
	
	// Takes a primitive array and an iterator over the elements within,
	// and populates it with the desired values.
	private void populatePrimitiveArray(Class<?> compType, Object obj, Iterator<?> i) {
		int index = 0;
		while (i.hasNext()) {
			Element e = (Element)i.next();
			Element val = e.getChild("value");
			Array.set(obj, index, castToPrimitive(compType, val.getText()));
			index++;
		}
	}
	
	// Takes a primitive class and a string, and casts it to the wrapper object for that primitive type.
	private Object castToPrimitive(Class prim, String value) {
		assert(prim.isPrimitive());
		if (prim.getName().equals("int")) {
			return Integer.valueOf(value);
		} else if (prim.getName().equals("float")) {
			return Float.valueOf(value);
		} else if (prim.getName().equals("byte")) {
			return Byte.valueOf(value);
		} else if (prim.getName().equals("short")) {
			return Short.valueOf(value);
		} else if (prim.getName().equals("long")) {
			return Long.valueOf(value);
		} else if (prim.getName().equals("double")) {
			return Double.valueOf(value);
		} else if (prim.getName().equals("boolean")) {
			return Boolean.valueOf(value);
		} else if (prim.getName().equals("char")) {
			return value.charAt(0);
		} else {
			assert(false); // Should never happen!
			return null;
		}
	}
	
}
