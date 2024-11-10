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
}
