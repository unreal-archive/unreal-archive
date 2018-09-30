package net.shrimpworks.unreal.archive.indexer.skins;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import net.shrimpworks.unreal.archive.indexer.ContentType;
import net.shrimpworks.unreal.archive.indexer.Incoming;
import net.shrimpworks.unreal.archive.indexer.IndexLog;
import net.shrimpworks.unreal.archive.indexer.Submission;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SkinClassifierTest {

	@Test
	public void ut2004() throws IOException {
		Path tmpSkin = Files.createTempFile("test-skin", ".zip");
		try (InputStream is = getClass().getResourceAsStream("ut2004skin.zip")) {
			Files.copy(is, tmpSkin, StandardCopyOption.REPLACE_EXISTING);

			Submission sub = new Submission(tmpSkin);
			IndexLog log = new IndexLog(sub);
			Incoming incoming = new Incoming(sub, log).prepare();

			assertEquals(ContentType.SKIN, ContentType.classify(incoming));
		} finally {
			Files.deleteIfExists(tmpSkin);
		}
	}
}
