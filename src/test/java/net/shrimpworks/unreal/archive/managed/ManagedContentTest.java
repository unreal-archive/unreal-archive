package net.shrimpworks.unreal.archive.managed;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;

import com.github.rjeschke.txtmark.Processor;

import net.shrimpworks.unreal.archive.ArchiveUtil;
import net.shrimpworks.unreal.archive.YAML;
import net.shrimpworks.unreal.archive.www.ManagedContent;

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

		// fake syncing the download, should appear as a change
		newMan.downloads.get(0).downloads.add("https://cool-files.dl/file.exe");
		newMan.downloads.get(0).synced = true;

		assertNotEquals(man, newMan);
		assertNotEquals(man.downloads.get(0), newMan.downloads.get(0));
	}

	@Test
	public void contentProcess() throws IOException {
		final Managed man = mockContent();

		// create a simple on-disk structure containing a test document and metadata
		final Path tmpRoot = Files.createTempDirectory("test-managed");
		try {
			final Path outPath = Files.createDirectories(tmpRoot.resolve("test"));
			Files.write(outPath.resolve("managed.yml"), YAML.toString(man).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);

			try (InputStream is = getClass().getResourceAsStream("readme.md")) {
				Files.copy(is, outPath.resolve(man.document));
			}

			final ManagedContentManager cm = new ManagedContentManager(tmpRoot, "tests");
			assertTrue(cm.all().contains(man));

			try (ReadableByteChannel docChan = cm.document(man)) {
				assertNotNull(docChan);

				String markdown = Processor.process(Channels.newInputStream(docChan));
				assertNotNull(markdown);
				assertTrue(markdown.contains("Testing Document"));
			}
		} finally {
			// cleanup temp files
			ArchiveUtil.cleanPath(tmpRoot);
		}
	}

	@Test
	public void contentWww() throws IOException {
		Managed man = mockContent();

		// create a simple on-disk structure containing a test document and metadata
		Path tmpRoot = Files.createTempDirectory("test-managed");
		Path wwwRoot = Files.createTempDirectory("test-managed-www");
		try {
			final Path outPath = Files.createDirectories(tmpRoot.resolve("test"));
			Files.write(outPath.resolve("managed.yml"), YAML.toString(man).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);

			try (InputStream is = getClass().getResourceAsStream("readme.md")) {
				Files.copy(is, outPath.resolve(man.document));
			}

			final ManagedContentManager cm = new ManagedContentManager(tmpRoot, "tests");
			assertTrue(cm.all().contains(man));

			ManagedContent content = new ManagedContent(cm, wwwRoot, wwwRoot, "Testing Stuff");
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
		man.game = "General";
		man.path = "Tests";
		man.title = "Testing Things";
		man.author = "Bob";
		man.description = "There is no description";
		man.homepage = "https://unreal.com/";
		man.document = "readme.md";

		final Managed.ManagedFile file = new Managed.ManagedFile();
		file.platform = Managed.Platform.WINDOWS;
		file.localFile = "file.exe";
		file.synced = false;
		file.title = "The File";
		file.version = "1.0";

		man.downloads.add(file);

		return man;
	}

}
