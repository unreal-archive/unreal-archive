package net.shrimpworks.unreal.archive;

import java.util.HashMap;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthorNamesTest {

	@Test
	public void namesTest() {
		AuthorNames name = new AuthorNames(new HashMap<>(), Set.of());
		assertEquals(name.cleanName("Eenocks/MH-Conversion by Barbie"), "Eenocks/Barbie");
	}

	@Test
	public void akaAliasTest() {
		AuthorNames name = new AuthorNames(new HashMap<>(), Set.of());
		name.maybeAutoAlias("Mike Bananas A.K.A. Mr. Banan");
		assertEquals("Mike Bananas", name.cleanName("Mike Bananas A.K.A. Mr. Banan"));
		assertEquals("Mike Bananas", name.cleanName("Mr. Banan"));
	}

	@Test
	public void handleAliasTest() {
		AuthorNames name = new AuthorNames(new HashMap<>(), Set.of());
		name.maybeAutoAlias("Mike \"Mr. Banan\" Bananas");
		assertEquals("Mike \"Mr. Banan\" Bananas", name.cleanName("Mike \"Mr. Banan\" Bananas"));
		assertEquals("Mike \"Mr. Banan\" Bananas", name.cleanName("Mike Bananas"));
		assertEquals("Mike \"Mr. Banan\" Bananas", name.cleanName("Mr. Banan"));
	}

	@Test
	public void excludeAliasTest() {
		AuthorNames name = new AuthorNames(new HashMap<>(), Set.of("Mike \"Mr. Banan\" Bananas"));
		name.maybeAutoAlias("Mike \"Mr. Banan\" Bananas");
		name.maybeAutoAlias("Mr. Banan");
		assertEquals("Mike \"Mr. Banan\" Bananas", name.cleanName("Mike \"Mr. Banan\" Bananas"));
		assertEquals("Mr. Banan", name.cleanName("Mr. Banan"));
	}

}
