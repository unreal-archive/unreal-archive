package org.unrealarchive.indexing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IndexingTest {

	@Test
	public void authorDetect() {
		String[] lines = {
				"Cool Skin  (by : Jim Bob - website: jimbob.com, e-mail: jim@jimbob.com)",
				"Author: Jim Bob"
		};

		final Pattern authorPattern = Pattern.compile("(.+)?(author|by)([\\s:]+)?([A-Za-z0-9 _]+)(.+)?", Pattern.CASE_INSENSITIVE);

		for (String s : lines) {
			Matcher m = authorPattern.matcher(s);
			assertTrue(m.matches());
			assertEquals("Jim Bob", m.group(4).trim());
		}
	}
}
