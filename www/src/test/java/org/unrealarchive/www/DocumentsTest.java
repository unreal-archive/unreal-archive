package org.unrealarchive.www;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;

import org.unrealarchive.common.ArchiveUtil;
import org.unrealarchive.common.YAML;
import org.unrealarchive.content.docs.Document;
import org.unrealarchive.content.docs.DocumentRepository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DocumentsTest {

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
