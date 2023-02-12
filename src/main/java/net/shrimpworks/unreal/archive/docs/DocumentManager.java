package net.shrimpworks.unreal.archive.docs;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.shrimpworks.unreal.archive.common.Util;
import net.shrimpworks.unreal.archive.common.YAML;

public class DocumentManager {

	private final Map<Document, DocumentHolder> documents;

	public DocumentManager(Path path) throws IOException {
		this.documents = new HashMap<>();

		// load contents from path into content
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (Util.extension(file).equalsIgnoreCase("yml")) {
					Document c = YAML.fromFile(file, Document.class);
					documents.put(c, new DocumentHolder(file, c));
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * Get the total document count.
	 *
	 * @return number of documents
	 */
	public int size() {
		return documents.size();
	}

	/**
	 * Get all known documents' metadata.
	 *
	 * @return copy of the document collection
	 */
	public Collection<Document> all() {
		return Collections.unmodifiableCollection(documents.keySet());
	}

	/**
	 * Get the raw text content of the document as a channel.
	 *
	 * @param doc document to retrieve
	 * @return document content
	 * @throws IOException failed to open the document
	 */
	public ReadableByteChannel document(Document doc) throws IOException {
		DocumentHolder holder = documents.get(doc);
		if (holder == null) return null;

		Path docPath = holder.path.getParent().resolve(doc.name);

		if (!Files.exists(docPath)) return null;

		return Files.newByteChannel(docPath, StandardOpenOption.READ);
	}

	/**
	 * Get the on-disk location of the document metadata.
	 * <p>
	 * This can be used to access files intended to be bundled with
	 * the document.
	 *
	 * @param doc document to get root path for
	 * @return document root path
	 */
	public Path documentRoot(Document doc) {
		DocumentHolder holder = documents.get(doc);
		if (holder == null) return null;

		return holder.path.getParent();
	}

	private static class DocumentHolder {

		private final Path path;
		private final Document document;

		public DocumentHolder(Path path, Document document) {
			this.path = path;
			this.document = document;
		}
	}

}
