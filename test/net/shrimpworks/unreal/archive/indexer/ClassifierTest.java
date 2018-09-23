package net.shrimpworks.unreal.archive.indexer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import net.shrimpworks.unreal.archive.indexer.ContentClassifier;
import net.shrimpworks.unreal.archive.indexer.ContentSubmission;
import net.shrimpworks.unreal.archive.indexer.Incoming;
import net.shrimpworks.unreal.archive.indexer.IndexLog;

import org.junit.Test;

import static org.junit.Assert.*;

public class ClassifierTest {

	@Test(expected = UnsupportedOperationException.class)
	public void invalid() throws IOException {
		Path tmp = Files.createTempFile("classify-invalid", ".txt");
		try {
			Files.write(tmp, "hello".getBytes(StandardCharsets.UTF_8));

			ContentSubmission sub = new ContentSubmission(tmp);
			IndexLog log = new IndexLog(sub);

			// will fail to process text file as a valid content file
			new Incoming(sub, log);
		} finally {
			Files.deleteIfExists(tmp);
		}

		fail("Did not throw exception");
	}

	@Test
	public void map() throws IOException {
		Path tmpMap = Files.createTempFile("test-dm-longestyard", ".zip");
		try (InputStream is = getClass().getResourceAsStream("dm-longestyard.zip")) {
			Files.copy(is, tmpMap, StandardCopyOption.REPLACE_EXISTING);

			ContentSubmission sub = new ContentSubmission(tmpMap);
			IndexLog log = new IndexLog(sub);
			Incoming incoming = new Incoming(sub, log);

			assertEquals(ContentClassifier.ContentType.MAP, ContentClassifier.classify(incoming, log));
		} finally {
			Files.deleteIfExists(tmpMap);
		}
	}

	@Test
	public void logTest() throws IOException {
		Path tmpMap = Files.createTempFile("test-dm-longestyard", ".zip");
		try (InputStream is = getClass().getResourceAsStream("dm-longestyard.zip")) {
			Files.copy(is, tmpMap, StandardCopyOption.REPLACE_EXISTING);

			ContentSubmission sub = new ContentSubmission(tmpMap);
			IndexLog log = new IndexLog(sub);

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
