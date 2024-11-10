package org.unrealarchive.mirror;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.unrealarchive.content.addons.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LocalMirrorclientTest {

	static Map m = new Map();

	static {
		m.contentType = "MAP";
		m.game = "Unreal Tournament";
		m.gametype = "Capture the Flag";
		m.author = "Bob";
		m.name = "CTF-DeckUnlimited";
		m.title = "Deck Unlimited";
		m.description = "Really cool map";
		m.releaseDate = "2023-07";
		m.hash = "10000000";
	}

	@Test
	public void pathResolution() throws IOException {
		final LocalMirrorClient client = new LocalMirrorClient(1, (total, remaining, last) -> {
		});

		String path = String.format("%s/{game}/{contentType}/{gameType}/plain word/{poop}", System.getProperty("java.io.tmpdir"));
		Path output = Paths.get(path);

		Path p = Files.createDirectories(client.outputPath(m, output));

		assertEquals(Paths.get(
			String.format("%s/Unreal Tournament/MAP/Capture the Flag/plain word/unknown", System.getProperty("java.io.tmpdir"))
		), p);
	}
}
