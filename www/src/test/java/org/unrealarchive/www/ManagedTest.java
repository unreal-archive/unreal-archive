package org.unrealarchive.www;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;

import org.unrealarchive.common.ArchiveUtil;
import org.unrealarchive.common.Platform;
import org.unrealarchive.common.YAML;
import org.unrealarchive.content.managed.Managed;
import org.unrealarchive.content.managed.ManagedContentRepository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ManagedTest {

	@Test
	public void contentWww() throws IOException {
		Managed man = mockContent();

		// create a simple on-disk structure containing a test document and metadata
		Path tmpRoot = Files.createTempDirectory("test-managed");
		Path wwwRoot = Files.createTempDirectory("test-managed-www");
		try {
			final Path outPath = Files.createDirectories(tmpRoot.resolve("test"));
			Files.writeString(outPath.resolve("managed.yml"), YAML.toString(man), StandardOpenOption.CREATE);

			try (InputStream is = getClass().getResourceAsStream("test.md")) {
				Files.copy(is, outPath.resolve(man.document));
			}

			final ManagedContentRepository cm = new ManagedContentRepository.FileRepository(tmpRoot);
			assertTrue(cm.all().contains(man));

			ManagedContent content = new ManagedContent(cm, wwwRoot, wwwRoot, SiteFeatures.ALL);
			assertEquals(3, content.generate().size());
		} finally {
			// cleanup temp files
			ArchiveUtil.cleanPath(tmpRoot);
			ArchiveUtil.cleanPath(wwwRoot);
		}
	}

	private Managed mockContent() {
		final Managed man = new Managed();
		man.createdDate = LocalDate.now().minusDays(3);
		man.updatedDate = LocalDate.now();
		man.game = "General";
		man.group = "Testing & Stuff";
		man.subGroup = "Tests";
		man.title = "Testing Things";
		man.author = "Bob";
		man.description = "There is no description";
		man.homepage = "https://unreal.com/";
		man.document = "test.md";

		final Managed.ManagedFile file = new Managed.ManagedFile();
		file.platform = Platform.WINDOWS;
		file.localFile = "file.exe";
		file.synced = false;
		file.title = "The File";
		file.version = "1.0";

		man.downloads.add(file);

		return man;
	}

}
