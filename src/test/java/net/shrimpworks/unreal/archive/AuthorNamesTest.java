package net.shrimpworks.unreal.archive;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthorNamesTest {

	@Test
	public void namesTest() {
		AuthorNames name = new AuthorNames(Map.of());
		assertEquals(name.cleanName("Eenocks/MH-Conversion by Barbie"), "Eenocks/Barbie");
	}
}
