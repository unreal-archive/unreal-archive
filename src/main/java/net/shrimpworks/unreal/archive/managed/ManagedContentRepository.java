package net.shrimpworks.unreal.archive.managed;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.shrimpworks.unreal.archive.common.Util;
import net.shrimpworks.unreal.archive.common.YAML;
import net.shrimpworks.unreal.archive.content.Games;

import static net.shrimpworks.unreal.archive.content.Content.UNKNOWN;

public interface ManagedContentRepository {

	/**
	 * Get the total content count.
	 *
	 * @return number of contents
	 */
	public int size();

	/**
	 * Get all known content metadata.
	 *
	 * @return copy of the content collection
	 */
	public Collection<Managed> all();

	void put(Managed managed) throws IOException;

	void create(Games game, String group, String path, String title, Consumer<Managed> initialised)
		throws IOException;

	/**
	 * Get the raw text content of the content's associated document, as a channel.
	 *
	 * @param managed content document to retrieve
	 * @return document content
	 * @throws IOException failed to open the document
	 */
	public ReadableByteChannel document(Managed managed) throws IOException;

	/**
	 * Copy assets (title images, etc) to the specified `outPath`
	 */
	public void writeContent(Managed managed, Path outPath) throws IOException;

	public Managed findManaged(Games game, String group, String path, String title);

	public static class FileRepository implements ManagedContentRepository {

		private static final String DOCUMENT_FILE = "readme.md";
		private static final String DOCUMENT_TEMPLATE_FILE = "template.md";

		private final Path root;

		private final Map<Managed, ManagedContentHolder> content;

		public FileRepository(Path root) throws IOException {
			this.root = root;
			this.content = new HashMap<>();

			// load contents from path into content
			Files.walkFileTree(root, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (Util.extension(file).equalsIgnoreCase("yml")) {
						Managed c = YAML.fromFile(file, Managed.class);
						content.put(c, new ManagedContentHolder(file, c));
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}

		@Override
		public int size() {
			return content.size();
		}

		@Override
		public Collection<Managed> all() {
			return Collections.unmodifiableCollection(content.keySet());
		}

		@Override
		public ReadableByteChannel document(Managed managed) throws IOException {
			ManagedContentHolder holder = content.get(managed);
			if (holder == null) return null;

			Path docPath = holder.path.getParent().resolve(managed.document);

			if (!Files.exists(docPath)) return null;

			return Files.newByteChannel(docPath, StandardOpenOption.READ);
		}

		@Override
		public void writeContent(Managed managed, Path outPath) throws IOException {
			ManagedContentHolder holder = content.get(managed);
			if (holder == null) return;

			Util.copyTree(holder.path.getParent(), outPath);
		}

		@Override
		public void put(Managed managed) throws IOException {
			final Path outPath = Files.createDirectories(managed.contentPath(root));
			Path yml = Util.safeFileName(outPath.resolve("managed.yml"));
			Files.writeString(yml, YAML.toString(managed), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

			// replace existing entry
			content.put(managed, new ManagedContentHolder(yml, managed));
		}

		@Override
		public void create(Games game, String group, String path, String title, Consumer<Managed> completed)
			throws IOException {
			// create path
			final String neatName = Util.capitalWords(title);

			Managed man = new Managed();

			// create the basic definition
			man.createdDate = LocalDate.now();
			man.updatedDate = LocalDate.now();
			man.game = game.name;
			man.group = group;
			man.path = path;
			man.title = neatName;
			man.author = UNKNOWN;
			man.document = "readme.md";
			man.titleImage = "title.png";

			Managed.ManagedFile sampleFile = new Managed.ManagedFile();
			sampleFile.title = neatName + " Download";
			sampleFile.version = "1.0";
			sampleFile.localFile = "/path/to/file.zip";
			man.downloads.add(sampleFile);

			completed.accept(man);

			put(man);

			final Path docPath = Files.createDirectories(man.contentPath(root));
			Path md = Util.safeFileName(docPath.resolve(DOCUMENT_FILE));
			if (!Files.exists(md)) Files.copy(getClass().getResourceAsStream(DOCUMENT_TEMPLATE_FILE), md);
		}

		@Override
		public Managed findManaged(Games game, String group, String path, String title) {
			return content.values().stream()
						  .filter(m -> !m.managed.deleted())
						  .filter(m -> m.managed.game().equalsIgnoreCase(game.name))
						  .filter(m -> m.managed.group.equalsIgnoreCase(group))
						  .filter(m -> m.managed.path.equalsIgnoreCase(path))
						  .filter(m -> m.managed.title.equalsIgnoreCase(title))
						  .map(m -> m.managed)
						  .findFirst().orElse(null);
		}

		private record ManagedContentHolder(Path path, Managed managed) {
		}
	}
}
