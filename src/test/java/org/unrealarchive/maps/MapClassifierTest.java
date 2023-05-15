package org.unrealarchive.maps;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.unrealarchive.content.addons.SimpleAddonType;
import org.unrealarchive.indexing.AddonClassifier;
import org.unrealarchive.indexing.Incoming;
import org.unrealarchive.indexing.IndexLog;
import org.unrealarchive.indexing.Submission;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapClassifierTest {

	@Test
	public void utMap() throws IOException {
		Path tmpMap = Files.createTempFile("test-dm-longestyard", ".zip");
		try (InputStream is = getClass().getResourceAsStream("../maps/dm-longestyard.zip")) {
			Files.copy(is, tmpMap, StandardCopyOption.REPLACE_EXISTING);

			Submission sub = new Submission(tmpMap);
			IndexLog log = new IndexLog();
			Incoming incoming = new Incoming(sub, log).prepare();

			assertEquals(SimpleAddonType.MAP, AddonClassifier.classify(incoming).contentType());
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

			assertEquals(SimpleAddonType.MAP, AddonClassifier.classify(incoming).contentType());
		} finally {
			Files.deleteIfExists(tmpMap);
		}
	}

}
