package org.unrealarchive.common;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflection utilities.
 */
public class Reflect {

	private static final Map<Class<?>, Map<String, Field>> typeFields = new ConcurrentHashMap<>();

	/**
	 * Gets map of lowercase field names to fields for the type of `thing`
	 * provided.
	 * <p>
	 * The results are cached for subsequent accesses.
	 */
	public static Map<String, Field> classLowercaseFields(Object thing) {
		return typeFields.computeIfAbsent(thing.getClass(), a -> {
			Field[] newFields = a.getFields();
			Map<String, Field> classFields = new HashMap<>(newFields.length);
			for (Field field : newFields) classFields.put(field.getName().toLowerCase(), field);
			return classFields;
		});
	}
}
