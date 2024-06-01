package org.unrealarchive.content.docs;

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
import org.unrealarchive.common.YAML;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentTest {

	@Test
	public void documentYaml() throws IOException {
		Document doc = new Document();
		doc.createdDate = LocalDate.now().minusDays(3);
		doc.updatedDate = LocalDate.now();
		doc.game = "General";
		doc.group = "Tests";
		doc.subGroup = "Stuff";
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
		doc.group = "Tests";
		doc.subGroup = "Stuff";
		doc.title = "Testing Things";
		doc.author = "Bob";
		doc.description = "There is no description";

		// create a simple on-disk structure containing a test document and metadata
		Path tmpRoot = Files.createTempDirectory("test-docs");
		try {
			Path docPath = Files.createDirectories(tmpRoot.resolve("test-doc"));
			Files.writeString(docPath.resolve("document.yml"), YAML.toString(doc), StandardOpenOption.CREATE);

			try (InputStream is = getClass().getResourceAsStream("doc.md")) {
				Files.copy(is, docPath.resolve(doc.name));
			}

			DocumentRepository dm = new DocumentRepository.FileRepository(tmpRoot);
			assertTrue(dm.all().contains(doc));

			try (BufferedReader reader = new BufferedReader(Channels.newReader(dm.document(doc), StandardCharsets.UTF_8))) {
				assertNotNull(reader);

				String docContent = reader.lines().collect(Collectors.joining("\n"));
				assertTrue(docContent.contains("Testing Document"));
			}
		} finally {
			// cleanup temp files
			ArchiveUtil.cleanPath(tmpRoot);
		}
	}

}
