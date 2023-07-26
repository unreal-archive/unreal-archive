package org.unrealarchive.indexing.models;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.unrealarchive.content.addons.Model;
import org.unrealarchive.indexing.Incoming;
import org.unrealarchive.indexing.IndexLog;
import org.unrealarchive.indexing.Submission;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModelIndexerTest {

	@Test
	public void ut3Characters() throws IOException {
		Path tmpSkin = Files.createTempFile("test-model", ".zip");
		try (InputStream is = getClass().getResourceAsStream("gow-chars.zip")) {
			Files.copy(is, tmpSkin, StandardCopyOption.REPLACE_EXISTING);

			Submission sub = new Submission(tmpSkin);
			IndexLog log = new IndexLog();
			Incoming incoming = new Incoming(sub, log).prepare();

			ModelIndexHandler indexer = new ModelIndexHandler();
			indexer.index(incoming, new Model(), r -> {
				assertTrue(r.content.models.contains("Locust: Drone"));
				assertTrue(r.content.models.contains("COG: Marcus"));
			});
			incoming.close();
		} finally {
			Files.deleteIfExists(tmpSkin);
		}
	}
}
