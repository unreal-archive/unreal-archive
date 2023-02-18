package net.shrimpworks.unreal.archive.docs;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import net.shrimpworks.unreal.archive.common.ArchiveUtil;
import net.shrimpworks.unreal.archive.common.YAML;
import net.shrimpworks.unreal.archive.www.Documents;
import net.shrimpworks.unreal.archive.www.SiteFeatures;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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

			DocumentRepository dm = new DocumentRepository.FileRepository(tmpRoot);
			assertTrue(dm.all().contains(doc));

			try (Reader reader = Channels.newReader(dm.document(doc), StandardCharsets.UTF_8.name())) {
				assertNotNull(reader);

				Parser parser = Parser.builder().build();
				HtmlRenderer renderer = HtmlRenderer.builder().build();
				String markdown = renderer.render(parser.parseReader(reader));
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

			DocumentRepository dm = new DocumentRepository.FileRepository(tmpRoot);
			assertTrue(dm.all().contains(doc));

			Documents documents = new Documents(dm, wwwRoot, wwwRoot, SiteFeatures.ALL);
			assertEquals(6, documents.generate().size());
		} finally {
			// cleanup temp files
			ArchiveUtil.cleanPath(tmpRoot);
			ArchiveUtil.cleanPath(wwwRoot);
		}
	}
}
