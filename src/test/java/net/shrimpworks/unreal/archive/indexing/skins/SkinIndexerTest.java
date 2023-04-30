package net.shrimpworks.unreal.archive.indexing.skins;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import net.shrimpworks.unreal.archive.content.addons.Skin;
import net.shrimpworks.unreal.archive.indexing.Incoming;
import net.shrimpworks.unreal.archive.indexing.IndexLog;
import net.shrimpworks.unreal.archive.indexing.Submission;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SkinIndexerTest {

	@Test
	public void ut2004() throws IOException {
		Path tmpSkin = Files.createTempFile("test-skin", ".zip");
		try (InputStream is = getClass().getResourceAsStream("ut2004skin.zip")) {
			Files.copy(is, tmpSkin, StandardCopyOption.REPLACE_EXISTING);

			Submission sub = new Submission(tmpSkin);
			IndexLog log = new IndexLog();
			Incoming incoming = new Incoming(sub, log).prepare();

			SkinIndexHandler indexer = new SkinIndexHandler();
			indexer.index(incoming, new Skin(), r -> {
				assertEquals("Unreal Tournament 2004", r.content.game);
				assertEquals("Nasdarek", r.content.name);
				assertTrue(r.content.skins.contains("Nasdarek"));
			});

		} finally {
			Files.deleteIfExists(tmpSkin);
		}
	}
}
