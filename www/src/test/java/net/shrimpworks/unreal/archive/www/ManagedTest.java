package net.shrimpworks.unreal.archive.www;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;

import net.shrimpworks.unreal.archive.common.ArchiveUtil;
import net.shrimpworks.unreal.archive.common.Platform;
import net.shrimpworks.unreal.archive.common.YAML;
import net.shrimpworks.unreal.archive.content.managed.Managed;
import net.shrimpworks.unreal.archive.content.managed.ManagedContentRepository;

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
			Files.write(outPath.resolve("managed.yml"), YAML.toString(man).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);

			try (InputStream is = getClass().getResourceAsStream("test.md")) {
				Files.copy(is, outPath.resolve(man.document));
			}

			final ManagedContentRepository cm = new ManagedContentRepository.FileRepository(tmpRoot);
			assertTrue(cm.all().contains(man));

			ManagedContent content = new ManagedContent(cm, wwwRoot, wwwRoot, SiteFeatures.ALL);
			assertEquals(4, content.generate().size());
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
		man.group = "Testing & Stuff";
		man.game = "General";
		man.path = "Tests";
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
