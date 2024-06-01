package org.unrealarchive.content.managed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.stream.Collectors;

import org.unrealarchive.common.ArchiveUtil;
import org.unrealarchive.common.Platform;
import org.unrealarchive.common.YAML;
import org.unrealarchive.content.Download;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ManagedContentTest {

	@Test
	public void managedYaml() throws IOException {
		final Managed man = mockContent();

		// serialise and de-serialise between YAML and instance
		final String stringMan = YAML.toString(man);
		final Managed newMan = YAML.fromString(stringMan, Managed.class);

		assertEquals(man, newMan);
		assertNotSame(man, newMan);
		assertEquals(man.downloads.get(0), newMan.downloads.get(0));

		// fake syncing the download, downloads don't count as changes, so they can be managed while syncing
		newMan.downloads.get(0).downloads.add(new Download("https://cool-files.dl/file.exe"));
		newMan.downloads.get(0).synced = true;

		assertEquals(man, newMan);
		assertNotEquals(man.downloads.get(0), newMan.downloads.get(0));
	}

	@Test
	public void contentProcess() throws IOException {
		final Managed man = mockContent();

		// create a simple on-disk structure containing a test document and metadata
		final Path tmpRoot = Files.createTempDirectory("test-managed");
		try {
			final Path outPath = Files.createDirectories(tmpRoot.resolve("test"));
			Files.writeString(outPath.resolve("managed.yml"), YAML.toString(man), StandardOpenOption.CREATE);

			// prepare a fake document
			try (InputStream is = getClass().getResourceAsStream("readme.md")) {
				assertNotNull(is);
				Files.copy(is, outPath.resolve(man.document));
			}

			final ManagedContentRepository cm = new ManagedContentRepository.FileRepository(tmpRoot);
			assertTrue(cm.all().contains(man));

			try (BufferedReader reader = new BufferedReader(Channels.newReader(cm.document(man), StandardCharsets.UTF_8))) {
				assertNotNull(reader);

				String docContent = reader.lines().collect(Collectors.joining("\n"));
				assertTrue(docContent.contains("Testing Document"));
			}
		} finally {
			// cleanup temp files
			ArchiveUtil.cleanPath(tmpRoot);
		}
	}

	private Managed mockContent() {
		final Managed man = new Managed();
		man.createdDate = LocalDate.now().minusDays(3);
		man.updatedDate = LocalDate.now();
		man.group = "Testing & Stuff";
		man.game = "General";
		man.subGroup = "Tests";
		man.title = "Testing Things";
		man.author = "Bob";
		man.description = "There is no description";
		man.homepage = "https://unreal.com/";
		man.document = "readme.md";

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
