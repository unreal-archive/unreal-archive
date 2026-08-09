package org.unrealarchive.indexing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IndexUtilsTest {

	@Test
	public void findAuthor() throws IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("TestReadMe.txt")))) {
			assertEquals("Thåt Guy", IndexUtils.findAuthor(br.lines().toList()));
		}
	}

	@Test
	public void cleanString() {
		// plain strings are left alone, other than whitespace trimming
		assertEquals("Thåt Guy", IndexUtils.cleanString("  Thåt Guy\n"));

		// colour markup is an ESC followed by an RGB triplet, anywhere in the string
		assertEquals("Hello", IndexUtils.cleanString("\u001bÿ\u0001\u0001Hello"));
		assertEquals("ab", IndexUtils.cleanString("a\u001bÿ\u0001\u0001b"));

		// a real UT2004 author, one colour code per character
		assertEquals("snare", IndexUtils.cleanString(
			"\u001b\u040e\u040e\u040es\u001b\u201a\u044a<n\u001b\u040e\u040e\u040ea\u001b\u201a\u044a<r\u001b\u040e\u040e\u040ee\u001b\u0440\u0440\u0440"));

		// truncated markup at the end of a string must not overrun it
		assertEquals("done", IndexUtils.cleanString("done\u001b"));
		assertEquals("done", IndexUtils.cleanString("done\u001bÿ"));

		// stray control characters go too, but newlines and tabs are content
		assertEquals("a\tb\nc", IndexUtils.cleanString("a\tb\u0000\u0007\nc"));
	}
}
