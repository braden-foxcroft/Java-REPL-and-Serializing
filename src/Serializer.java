
import java.lang.reflect.Field;
import java.util.Vector;

import org.jdom.*;

public class Serializer {
	
	public Document serialize(Object obj) {
		Vector<Object> all = new Vector<>();
		all.add(obj);
		
		
		
		
		Document res = new Document();
		return res;
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
