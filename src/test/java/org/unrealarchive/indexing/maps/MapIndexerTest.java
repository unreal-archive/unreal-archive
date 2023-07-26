package org.unrealarchive.indexing.maps;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.unrealarchive.content.addons.Map;
import org.unrealarchive.content.addons.MapGameTypes;
import org.unrealarchive.content.addons.SimpleAddonType;
import org.unrealarchive.indexing.AddonClassifier;
import org.unrealarchive.indexing.Incoming;
import org.unrealarchive.indexing.IndexLog;
import org.unrealarchive.indexing.Submission;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class MapIndexerTest {

	@Test
	public void gametypeDetection() {
		assertEquals("Greed", MapGameTypes.forMap("CTF-GRD-Cake").name);
		assertEquals("Greed", MapGameTypes.forMap("VCTF-GRD-Cake").name);
		assertEquals("Capture The Flag", MapGameTypes.forMap("CTF-Lies").name);
		assertEquals("DeathMatch", MapGameTypes.forMap("DM-DeathMatch").name);
	}

	@Test
	public void utMap() throws IOException {
		Path tmpMap = Files.createTempFile("test-dm-longestyard", ".zip");
		try (InputStream is = getClass().getResourceAsStream("../maps/dm-longestyard.zip")) {
			Files.copy(is, tmpMap, StandardCopyOption.REPLACE_EXISTING);

			Submission sub = new Submission(tmpMap);
			IndexLog log = new IndexLog();
			Incoming incoming = new Incoming(sub, log).prepare();

			MapIndexHandler indexer = new MapIndexHandler();
			Map map = AddonClassifier.newContent(AddonClassifier.identifierForType(SimpleAddonType.MAP), incoming);
			indexer.index(incoming, map, r -> {
				assertEquals("Unreal Tournament", r.content.game);
				assertEquals("The Longest Yard", r.content.title);
				assertEquals("DeathMatch", r.content.gametype);
				assertFalse(r.files.isEmpty());
			});
			incoming.close();
		} finally {
			Files.deleteIfExists(tmpMap);
		}
	}

	@Test
	public void ut3Map() throws IOException {
		Path tmpMap = Files.createTempFile("test-ctf-power", ".rar");

		try (InputStream is = getClass().getResourceAsStream("../maps/ctf-power.rar")) {
			Files.copy(is, tmpMap, StandardCopyOption.REPLACE_EXISTING);

			Submission sub = new Submission(tmpMap);
			IndexLog log = new IndexLog();
			Incoming incoming = new Incoming(sub, log).prepare();

			MapIndexHandler indexer = new MapIndexHandler();
			Map map = AddonClassifier.newContent(AddonClassifier.identifierForType(SimpleAddonType.MAP), incoming);
			indexer.index(incoming, map, r -> {
				assertEquals("Unreal Tournament 3", r.content.game);
				assertEquals("Power", r.content.title);
				assertEquals("Capture The Flag", r.content.gametype);
				assertEquals("RedSteels_Fury", r.content.author);
			});
			incoming.close();
		} finally {
			Files.deleteIfExists(tmpMap);
		}
	}
}
