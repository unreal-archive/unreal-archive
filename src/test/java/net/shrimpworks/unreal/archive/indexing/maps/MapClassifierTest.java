package net.shrimpworks.unreal.archive.indexing.maps;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import net.shrimpworks.unreal.archive.content.addons.SimpleAddonType;
import net.shrimpworks.unreal.archive.indexing.AddonClassifier;
import net.shrimpworks.unreal.archive.indexing.Incoming;
import net.shrimpworks.unreal.archive.indexing.IndexLog;
import net.shrimpworks.unreal.archive.indexing.Submission;

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
