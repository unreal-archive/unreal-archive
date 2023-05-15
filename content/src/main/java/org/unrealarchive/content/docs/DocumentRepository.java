package org.unrealarchive.content.docs;

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

import org.unrealarchive.common.Util;
import org.unrealarchive.common.YAML;

public interface DocumentRepository {

	/**
	 * Get the total document count.
	 *
	 * @return number of documents
	 */
	public int size();

	/**
	 * Get all known documents' metadata.
	 *
	 * @return copy of the document collection
	 */
	public Collection<Document> all();

	/**
	 * Get the raw text content of the document as a channel.
	 *
	 * @param doc document to retrieve
	 * @return document content
	 * @throws IOException failed to open the document
	 */
	public ReadableByteChannel document(Document doc) throws IOException;

	/**
	 * Copy assets (title images, etc) to the specified `outPath`
	 */
	public void writeContent(Document doc, Path outPath) throws IOException;

	public static class FileRepository implements DocumentRepository {

		private final Map<Document, DocumentHolder> documents;

		public FileRepository(Path path) throws IOException {
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

		@Override
		public int size() {
			return documents.size();
		}

		@Override
		public Collection<Document> all() {
			return Collections.unmodifiableCollection(documents.keySet());
		}

		@Override
		public ReadableByteChannel document(Document doc) throws IOException {
			DocumentHolder holder = documents.get(doc);
			if (holder == null) return null;

			Path docPath = holder.path.getParent().resolve(doc.name);

			if (!Files.exists(docPath)) return null;

			return Files.newByteChannel(docPath, StandardOpenOption.READ);
		}

		@Override
		public void writeContent(Document doc, Path outPath) throws IOException {
			DocumentHolder holder = documents.get(doc);
			if (holder == null) return;

			Util.copyTree(holder.path.getParent(), outPath);
		}

		private record DocumentHolder(Path path, Document document) {
		}
	}
}
