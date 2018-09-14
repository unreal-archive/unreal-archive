package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ClassifierTest {

	@Test(expected = IllegalArgumentException.class)
	public void invalid() throws IOException {
		Path tmp = Files.createTempFile("classify-invalid", "txt");
		try {
			Files.write(tmp, "hello".getBytes(StandardCharsets.UTF_8));

			ContentSubmission sub = new ContentSubmission(tmp);

			ContentClassifier.classify(sub);
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

			assertEquals(ContentClassifier.ContentType.MAP, ContentClassifier.classify(sub));
		} finally {
			Files.deleteIfExists(tmpMap);
		}
	}
}
