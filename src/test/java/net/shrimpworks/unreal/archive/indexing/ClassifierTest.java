package net.shrimpworks.unreal.archive.indexing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClassifierTest {

	@Test
	public void invalid() throws IOException {
		Path tmp = Files.createTempFile("classify-invalid", ".txt");
		try {
			Files.write(tmp, "hello".getBytes(StandardCharsets.UTF_8));

			Submission sub = new Submission(tmp);
			IndexLog log = new IndexLog();

			// will fail to process text file as a valid content file
			assertThrows(UnsupportedOperationException.class, () -> new Incoming(sub, log).prepare());
		} finally {
			Files.deleteIfExists(tmp);
		}
	}

	@Test
	public void logTest() throws IOException {
		Path tmpMap = Files.createTempFile("test-dm-longestyard", ".zip");
		try (InputStream is = getClass().getResourceAsStream("maps/dm-longestyard.zip")) {
			Files.copy(is, tmpMap, StandardCopyOption.REPLACE_EXISTING);

			IndexLog log = new IndexLog();

			assertTrue(log.ok());

			log.log(IndexLog.EntryType.INFO, "Some information");

			assertTrue(log.ok());

			log.log(IndexLog.EntryType.FATAL, "Stuff broke");

			assertFalse(log.ok());

		} finally {
			Files.deleteIfExists(tmpMap);
		}
	}
}
