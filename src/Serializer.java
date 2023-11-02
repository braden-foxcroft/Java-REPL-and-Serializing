
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Vector;

import org.jdom.*;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class Serializer {
	
	// Combines serialize and docToXML
	public static String objToXML(Object o) {
		return docToXML(new Serializer().serialize(o));
	}
	
	public static String docToXML(Document d) {
		XMLOutputter x = new XMLOutputter(Format.getPrettyFormat());
		return x.outputString(d);
	}
	
	public Document serialize(Object obj) {
		Vector<Object> all = new Vector<>();
		all.add(obj);
		addAllObjs(all); // Add the rest, iteratively.
		
		// A list of elements to add to the document.
		Element root = new Element("serialized");
		for (Object o : all) {
			Element newEle = makeElement(o,all);
			root.addContent(newEle);
		}

		Document doc = new Document();
		doc.addContent(root);
		return doc;
	}
	
	private Element makeElement(Object o, Vector<Object> all) {
		Element res = new Element("object");
		res.setAttribute("class",o.getClass().getName());
		res.setAttribute("id", Integer.toString(all.indexOf(o))); // This can fail, but never should.
		if (o.getClass().isArray()) {
			res.setAttribute("length", Integer.toString(Array.getLength(o)));
			// Case for primitive datatypes
			if (o.getClass().getComponentType().isPrimitive()) {
				for (int i = 0; i < Array.getLength(o); i++) {
					Element e = new Element("value");
					Text t = new Text(Array.get(o, i).toString());
					e.addContent(t);
					res.addContent(e);
				}
			} else {
				// Otherwise, use pointers
				for (int i = 0; i < Array.getLength(o); i++) {
					Element e = new Element("reference");
					Text t = new Text(Integer.toString(all.indexOf(Array.get(o, i))));
					e.addContent(t);
					res.addContent(e);
				}
			}
			return res;
		}
		if (o instanceof String) {
			// Just a single 'value' item.
			Element e = new Element("value");
			e.addContent(new Text((String) o));
			res.addContent(e);
			return res;
		}
		if (o instanceof Integer) {
			// Special case: sending integers
			Element e = new Element("value");
			e.addContent(new Text(o.toString()));
			res.addContent(e);
			return res;
		}
		if (o instanceof Collection<?>) {
			// Extract an array from it.
			Object[] col = ((Collection<?>)o).toArray();
			// Otherwise, use pointers
			for (Object c : col) {
				Element e = new Element("reference");
				Text t = new Text(Integer.toString(all.indexOf(c)));
				e.addContent(t);
				res.addContent(e);
			}
		}
		for (Field f : ObjectTerminal.allFields(o.getClass())) {
			try {
				f.setAccessible(true); // If we can't, then so be it.
				Element eFL = new Element("field");
				eFL.setAttribute("name", f.getName());
				eFL.setAttribute("declaringclass", f.getDeclaringClass().getName());
				if (f.getType().isPrimitive()) {
					Element val = new Element("value");
					val.addContent(new Text(f.get(o).toString()));
					eFL.addContent(val);
				} else {
					Element ref = new Element("reference");
					ref.addContent(new Text(Integer.toString(all.indexOf(f.get(o)))));
					eFL.addContent(ref);
				}
				res.addContent(eFL);
			} catch (Throwable th) {} // Ignore failures; nothing we can do about them.
		}
		return res;
	}
	
	// Adds all objects to the list, including downstream references.
	// Potentially awful time complexity, which could be improved considerably with a hashset.
	// That would break the ordering, though.
	private void addAllObjs(Vector<Object> objs) {
		for (int i = 0; i < objs.size(); i++) {
			Object o = objs.get(i);
			// Handle case where object is an array
			if (o.getClass().isArray()) {
				// This object is an array
				if (o.getClass().getComponentType().isPrimitive()) {continue;}
				for (Object n : (Object[])o) {
					if (objs.contains(n) || n==null) {continue;}
					objs.add(n);
				}
				continue; // No other attributes of note.
			} else if (o instanceof Collection) {
				// This object is a collection. Since we can't actually read the protected variables,
				//  we instead have to just turn it into an array, and traverse that instead.
				// We will eventually treat the collection like an array, when generating the XML.
				for (Object n : ((Collection<?>)o).toArray()) {
					if (objs.contains(n) || n==null) {continue;}
					objs.add(n);
				}
			}
			if (o instanceof String || o instanceof Integer) {continue;} // Skip Strings and Integers
			// Add new objects
			for (Object n : allRefs(o)) {
				if (objs.contains(n) || n==null) {continue;} // Skip existing objects.
				objs.add(n);
			}
		}
	}
	
	// Gets a list of object references for fields of the object
	private Vector<Object> allRefs(Object o) {
		Vector<Object> all = new Vector<>();
		for (Field f : allFields(o.getClass())) {
			try {
				if (f.getType().isPrimitive()) {continue;}
				f.setAccessible(true);
				Object n = f.get(o);
				all.add(n);
			} catch (Exception e) {}
		}
		return all;
	}
	
	// Gets a list of fields for a class. Used for construction.
	private Vector<Field> allFields(Class<?> c) {
		Vector<Field> res = new Vector<>();
		for (Field f : c.getDeclaredFields()) {
			res.add(f);
		}
		if (c.getSuperclass() != null) {
			res.addAll(allFields(c.getSuperclass()));
		}
		return res;
	}
}
