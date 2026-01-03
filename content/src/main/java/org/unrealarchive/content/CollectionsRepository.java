package org.unrealarchive.content;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.unrealarchive.common.Util;
import org.unrealarchive.common.YAML;

/**
 * Repository for persisted content collections.
 */
public interface CollectionsRepository {

	int size();

	Collection<ContentCollection> all();

	ContentCollection find(String title);

	void writeContent(ContentCollection collection, Path outPath) throws IOException;

	void put(ContentCollection collection) throws IOException;

	void putFile(ContentCollection collection, Path sourceFile) throws IOException;

	/**
	 * Basic file-backed implementation that loads all collections from a directory tree.
	 */
	class FileRepository implements CollectionsRepository {

		private final Map<ContentCollection, Path> collections;
		private final Path root;

		public FileRepository(Path path) throws IOException {
			this.collections = new ConcurrentHashMap<>();
			this.root = path;

			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (Util.extension(file).equalsIgnoreCase("yml")) {
						ContentCollection c = YAML.fromFile(file, ContentCollection.class);
						collections.put(c, file);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}

		@Override
		public int size() {
			return collections.size();
		}

		@Override
		public Collection<ContentCollection> all() {
			return Collections.unmodifiableCollection(collections.keySet());
		}

		@Override
		public ContentCollection find(String title) {
			for (ContentCollection collection : collections.keySet()) {
				if (collection.title.equalsIgnoreCase(title)) {
					return collection;
				}
			}
			return null;
		}

		@Override
		public void writeContent(ContentCollection collection, Path outPath) throws IOException {
			Path path = collections.get(collection);
			if (path == null) return;

			Util.copyTree(path.getParent(), outPath);
		}

		@Override
		public void put(ContentCollection collection) throws IOException {
			Path yml = Files.createDirectories(root.resolve(Util.slug(collection.name()))).resolve("collection.yml");
			Files.writeString(yml, YAML.toString(collection), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			collections.put(collection, yml);
		}

		@Override
		public void putFile(ContentCollection collection, Path sourceFile) throws IOException {
			Path target = Files.createDirectories(root.resolve(Util.slug(collection.name()))).resolve(sourceFile.getFileName());
			Files.copy(sourceFile, target);
		}
	}
}
