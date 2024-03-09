package org.unrealarchive.indexing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContentEditorTest {

	@Test
	void shouldAddToStringList() throws ReflectiveOperationException {
		Something thing = new Something();
		ContentEditor.applyAttribute(thing, "stringList", "addme");
		ContentEditor.applyAttribute(thing, "stringList", "again");

		ContentEditor.applyAttribute(thing, "stringMap", "myKey", "yourValue");

		assertTrue(thing.stringList.contains("addme"));
		assertEquals("yourValue", thing.stringMap.get("myKey"));
	}

	public static class Something {

		public List<String> stringList = new ArrayList<>();
		public Map<String, String> stringMap = new HashMap<>();
		public String stringValue;
	}
}
