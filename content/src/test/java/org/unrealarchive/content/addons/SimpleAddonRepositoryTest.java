package org.unrealarchive.content.addons;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleAddonRepositoryTest {

	@Test
	public void filterTest() throws IOException {
		SimpleAddonRepository repo = new SimpleAddonRepository.FileRepository(Files.createTempDirectory("ua_test"));
		testData(repo);

		assertEquals(2, repo.filter("game", "Unreal Tournament").size());
		assertEquals(3, repo.filter("game", "Unreal*").size());

		assertEquals(1, repo.filter("contentType", "map").size());

		assertEquals(1, repo.filter("skins", "*Something*").size());
	}

	public void testData(SimpleAddonRepository repo) throws IOException {
		Map m = new Map();
		m.contentType = "MAP";
		m.game = "Unreal Tournament";
		m.gametype = "Capture the Flag";
		m.author = "Bob";
		m.name = "CTF-DeckUnlimited";
		m.title = "Deck Unlimited";
		m.description = "Really cool map";
		m.releaseDate = "2023-07";
		m.hash = "10000000";
		repo.put(m);

		MapPack p = new MapPack();
		p.contentType = "MAP_PACK";
		p.author = "Various";
		p.game = "Unreal Tournament";
		p.gametype = "Mixed";
		p.name = "All the Decks";
		p.maps.add(new MapPack.PackMap("CTF-DeckUnlimited", "Deck Unlimited", "Bob"));
		p.maps.add(new MapPack.PackMap("DM-Deck16][", "Deck 16", "Epic Games"));
		p.maps.add(new MapPack.PackMap("DM-RandomMap", "Some Random Map", "Another Person"));
		p.hash = "20000000";
		repo.put(p);

		Skin s = new Skin();
		s.contentType = "SKIN";
		s.author = "Dude";
		s.game = "Unreal Tournament 3";
		s.name = "The Cool Skin";
		s.skins.add("Cool Skin");
		s.skins.add("Something");
		s.hash = "30000000";
		repo.put(s);
	}
}
