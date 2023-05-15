package org.unrealarchive.content;

import java.util.HashMap;
import java.util.Map;
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
	public void autoAliasTest() {
		AuthorNames name = new AuthorNames(new HashMap<>(), Set.of());
		name.maybeAutoAlias("Mike \"Mr. Banan\" Bananas");
		assertEquals("Mike \"Mr. Banan\" Bananas", name.cleanName("Mike \"Mr. Banan\" Bananas"));
		assertEquals("Mike \"Mr. Banan\" Bananas", name.cleanName("Mike Bananas"));
		assertEquals("Mike \"Mr. Banan\" Bananas", name.cleanName("Mr. Banan"));

		name.maybeAutoAlias("M. \"Banan\" Bananas");
		assertEquals("M. \"Banan\" Bananas", name.cleanName("Banan"));
		assertEquals("M. \"Banan\" Bananas", name.cleanName("M. Bananas"));
	}

	@Test
	public void accentAliasTest() {
		AuthorNames name = new AuthorNames(Map.of("mike bénané", "Mike Bananas"), Set.of());
		assertEquals("Mike Bananas", name.cleanName("Mike Bénané"));
	}

	@Test
	public void excludeAliasTest() {
		AuthorNames name = new AuthorNames(new HashMap<>(), Set.of("Mike \"Mr. Banan\" Bananas"));
		name.maybeAutoAlias("Mike \"Mr. Banan\" Bananas");
		name.maybeAutoAlias("Mr. Banan");
		assertEquals("Mike \"Mr. Banan\" Bananas", name.cleanName("Mike \"Mr. Banan\" Bananas"));
		assertEquals("Mr. Banan", name.cleanName("Mr. Banan"));
	}

	@Test
	public void emailAddressString() {
		AuthorNames name = new AuthorNames(new HashMap<>(), Set.of());
		assertEquals("Mike Bananas", name.cleanName("Mike Bananas mike@banan.com"));
		assertEquals("Mike Bananas", name.cleanName("Mike Bananas mike@banan.co.uk"));
	}

	@Test
	public void emailAddressName() {
		AuthorNames name = new AuthorNames(new HashMap<>(), Set.of());
		assertEquals("mike", name.cleanName("mike@banan.co.uk"));
	}

}
