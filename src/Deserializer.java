import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
		populateObjectList(objs,root);
		return objs.get(0);
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
			} else if (String.class.isAssignableFrom(c)) {
				// string
				obj = e.getChild("value").getText(); // Gets the string contents directly
			} else if (Integer.class.isAssignableFrom(c)) {
				// Integer
				obj = Integer.valueOf(e.getChild("value").getText());
			} else {
				// Normal object or collection.
				Constructor<?> cn = c.getConstructor(new Class<?>[] {});
				cn.setAccessible(true);
				obj = cn.newInstance(new Object[] {});
			}
			objs.add(obj);
		}
	}
	
	private void populateObjectList(Vector<Object> objs, Element root) throws Exception {
		Iterator<?> i = root.getChildren().iterator();
		int index = -1; // since it is incremented at the start of the loop.
		while (i.hasNext()) {
			Element e = (Element)i.next();
			index++;
			Class<?> c = Class.forName(e.getAttributeValue("class"));
			Object obj = objs.get(index);
			if (c.isArray()) {
				// Array
				if (c.getComponentType().isPrimitive()) {
					populatePrimitiveArray(c.getComponentType(), obj, e.getChildren().iterator());
				} else {
					populateObjectArray(c.getComponentType(), (Object[])obj, e.getChildren().iterator(), objs);
				}
			} else if (obj instanceof String || obj instanceof Integer) {
				continue;
			} else if (obj instanceof Collection) {
				// Collection
				populateCollection((Collection<?>)obj,objs,e.getChildren().iterator());
			} else {
				// regular object. Mix of attributes.
				populateRegularObject(obj,e.getChildren().iterator(), objs);
			}
		}
	}
	
	private void populateRegularObject(Object obj, Iterator<?> i, Vector<Object> objs) throws Exception {
		while (i.hasNext()) {
			Element e = (Element)i.next();
			// Get the field of the object.
			Class<?> c;
			Field f;
			// Get class
			try {
				c = Class.forName(e.getAttributeValue("declaringclass"));
			} catch (Exception ex) {
				throw new Exception("failed to get class: " + e.getAttributeValue("declaringclass"));
			}
			// Get field
			try {
				f = c.getDeclaredField(e.getAttributeValue("name"));
			} catch (Exception ex) {
				throw new Exception("failed to get field: "
						+ e.getAttributeValue("declaringclass") + "." +
						e.getAttributeValue("name"));
			}
			// Make field visible.
			try {
				f.setAccessible(true);
			} catch (Exception ex) {
				System.err.println("failed to set visibility of field: "
						+ e.getAttributeValue("declaringclass") + "." +
						e.getAttributeValue("name"));
				continue; // Skip this field
			}
			
			if (f.getType().isPrimitive()) {
				// primitive field
				f.set(obj,castToPrimitive(f.getType(),e.getChildText("value")));
			} else {
				// Non-primitive field
				f.set(obj, getRef(objs,e.getChild("reference")));
			}
		}
	}

	// We have to be careful because we are assuming it is a collection of objects.
	// (I don't see how it could be a collection of anything else...)
	private void populateCollection(Collection<?> obj, Vector<Object> objs, Iterator<?> i) {
		while (i.hasNext()) {
			Element e = (Element)i.next();
			Object nw = getRef(objs,e);
			// The next line is just 'obj.add(nw)', but assuming 'obj instanceof Collection<Object>'
			try {
				obj.getClass().getMethod("add", new Class<?>[] {Object.class}).invoke(obj, nw);
			} catch (Exception ex) {
				ex.printStackTrace(); // This should never happen.
				return;
			}
		}
	}

	// Takes a primitive array and an iterator over the elements within,
	// and populates it with the desired values.
	private void populatePrimitiveArray(Class<?> compType, Object obj, Iterator<?> i) {
		int index = 0;
		while (i.hasNext()) {
			Element e = (Element)i.next();
			Array.set(obj, index, castToPrimitive(compType, e.getText()));
			index++;
		}
	}
	
	// Takes an array of non-primitives and an iterator over the elements within,
	// and populates it with the desired pointers.
	private void populateObjectArray(Class<?> compType, Object[] arr, Iterator<?> i, Vector<Object> objs) {
		int index = 0;
		while (index < arr.length) {
			Element e = (Element)i.next();
			arr[index] = getRef(objs,e);
			index++;
		}
	}
	
	// Gets a reference object from a reference Element.
	private Object getRef(Vector<Object> objs, Element e) {
		int index = Integer.valueOf(e.getText());
		if (index == -1) {return null;}
		return objs.get(index);
	}
	
	// Takes a primitive class and a string, and casts it to the wrapper object for that primitive type.
	private Object castToPrimitive(Class<?> prim, String value) {
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
