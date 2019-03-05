package net.shrimpworks.unreal.archive.docs;

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
import net.shrimpworks.unreal.archive.www.Documents;

import org.junit.Test;

import static org.junit.Assert.*;

public class DocumentTest {

	@Test
	public void documentYaml() throws IOException {
		Document doc = new Document();
		doc.createdDate = LocalDate.now().minusDays(3);
		doc.updatedDate = LocalDate.now();
		doc.game = "General";
		doc.path = "Tests";
		doc.title = "Testing Things";
		doc.author = "Bob";
		doc.description = "There is no description";

		// serialise and de-serialise between YAML and instance
		String stringDoc = YAML.toString(doc);
		Document newDoc = YAML.fromString(stringDoc, Document.class);

		assertEquals(doc, newDoc);
		assertNotSame(doc, newDoc);
	}

	@Test
	public void documentProcess() throws IOException {
		Document doc = new Document();
		doc.createdDate = LocalDate.now().minusDays(3);
		doc.updatedDate = LocalDate.now();
		doc.name = "testdoc.md";
		doc.game = "General";
		doc.path = "Tests";
		doc.title = "Testing Things";
		doc.author = "Bob";
		doc.description = "There is no description";

		// create a simple on-disk structure containing a test document and metadata
		Path tmpRoot = Files.createTempDirectory("test-docs");
		try {
			Path docPath = Files.createDirectories(tmpRoot.resolve("test-doc"));
			Files.write(docPath.resolve("document.yml"), YAML.toString(doc).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);

			try (InputStream is = getClass().getResourceAsStream("test.md")) {
				Files.copy(is, docPath.resolve(doc.name));
			}

			DocumentManager dm = new DocumentManager(tmpRoot);
			assertTrue(dm.all().contains(doc));

			try (ReadableByteChannel docChan = dm.document(doc)) {
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
	public void documentWww() throws IOException {
		Document doc = new Document();
		doc.createdDate = LocalDate.now().minusDays(3);
		doc.updatedDate = LocalDate.now();
		doc.name = "testdoc.md";
		doc.game = "General";
		doc.path = "Tests/Stuff/Whatever";
		doc.title = "Testing Things";
		doc.author = "Bob";
		doc.description = "There is no description";

		// create a simple on-disk structure containing a test document and metadata
		Path tmpRoot = Files.createTempDirectory("test-docs");
		Path wwwRoot = Files.createTempDirectory("test-docs-www");
		try {
			Path docPath = Files.createDirectories(tmpRoot.resolve("test-doc"));
			Files.write(docPath.resolve("document.yml"), YAML.toString(doc).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);

			try (InputStream is = getClass().getResourceAsStream("test.md")) {
				Files.copy(is, docPath.resolve(doc.name));
			}

			DocumentManager dm = new DocumentManager(tmpRoot);
			assertTrue(dm.all().contains(doc));

			Documents documents = new Documents(dm, wwwRoot, wwwRoot);
			assertEquals(6, documents.generate().size());
		} finally {
			// cleanup temp files
			ArchiveUtil.cleanPath(tmpRoot);
			ArchiveUtil.cleanPath(wwwRoot);
		}
	}
}
