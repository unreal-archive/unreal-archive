package net.shrimpworks.unreal.archive.indexer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.YAML;
import net.shrimpworks.unreal.archive.storage.DataStore;

public class ContentManager {

	private final Path path;
	private final Map<String, ContentHolder> content;

	private final DataStore contentStore;
	private final DataStore imageStore;
	private final DataStore attachmentStore;

	private final Set<String> changes;

	public ContentManager(Path path, DataStore contentStore, DataStore imageStore, DataStore attachmentStore) throws IOException {
		this.path = path;
		this.contentStore = contentStore;
		this.imageStore = imageStore;
		this.attachmentStore = attachmentStore;
		this.content = new HashMap<>();

		this.changes = new HashSet<>();

		// load contents from path into content
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (Util.extension(file).equalsIgnoreCase("yml")) {
					Content c = YAML.fromFile(file, Content.class);
					content.put(c.hash, new ContentHolder(file, c));
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}

	public int size() {
		return content.size();
	}

	public Map<Class<? extends Content>, Long> countByType() {
		return content.values().stream()
					  .collect(Collectors.groupingBy(v -> v.content.getClass(), Collectors.counting()));
	}

	public Map<String, Long> countByGame() {
		return content.values().stream()
					  .collect(Collectors.groupingBy(v -> v.content.game, Collectors.counting()));
	}

	public Collection<Content> search(String game, String type, String name, String author) {
		return content.values().parallelStream()
					  .map(c -> c.content)
					  .filter(c -> {
						  boolean match = (game == null || c.game.equalsIgnoreCase(game));
						  match = match && (type == null || c.contentType.equalsIgnoreCase(type));
						  match = match && (author == null || c.author.toLowerCase().contains(author.toLowerCase()));
						  match = match && (name == null || c.name.toLowerCase().contains(name.toLowerCase()));
						  return match;
					  })
					  .collect(Collectors.toSet());
	}

	public Collection<Content> forName(String name) {
		return content.values().parallelStream()
					  .filter(c -> c.content.name.equalsIgnoreCase(name))
					  .map(c -> c.content)
					  .collect(Collectors.toSet());
	}

	public Content forHash(String hash) {
		ContentHolder contentHolder = content.get(hash);
		if (contentHolder != null) return contentHolder.content;

		return null;
	}

	@SuppressWarnings("unchecked")
	public <T extends Content> Collection<T> get(Class<T> type) {
		return content.values().parallelStream()
					  .map(c -> c.content)
					  .filter(c -> type.isAssignableFrom(c.getClass()))
					  .map(c -> (T)c)
					  .collect(Collectors.toSet());
	}

	/**
	 * Return all content which contains the provided file hash.
	 *
	 * @param hash file hash
	 * @return content containing the hash
	 */
	public Collection<Content> containing(String hash) {
		return content.values().parallelStream()
					  .filter(c -> c.content.containsFile(hash))
					  .map(c -> c.content)
					  .collect(Collectors.toSet());
	}

	// intent: when some content is going to be worked on, a clone is checked out.
	// when its checked out, its hash (immutable) is stored in the out collection.
	// after its been modified or left alone, the clone is checked in.
	// during check-in, if the the clone is no longer equal to the original, something changed.
	// if something changed, the content will be written out, within a new directory structure if needed
	// and the old file will be removed

	@SuppressWarnings("unchecked")
	public Content checkout(String hash) {
		ContentHolder out = this.content.get(hash);
		if (out != null) {
			try {
				return YAML.fromString(YAML.toString(out.content), Content.class);
			} catch (IOException e) {
				throw new IllegalStateException("Cannot clone content " + out.content);
			}
		}
		return null;
	}

	public boolean checkin(IndexResult<? extends Content> indexed, Submission submission) throws IOException {
		ContentHolder current = this.content.get(indexed.content.hash);

		if (current == null || !indexed.content.equals(current.content) || indexed.content.lastIndex.isAfter(current.content.lastIndex)) {
			// lets store the content \o/
			Path next = indexed.content.contentPath(path);
			Files.createDirectories(next);

			for (IndexResult.NewAttachment file : indexed.files) {
				// use same path structure as per contentPath
				try {
					String uploadPath = path.relativize(next.resolve(file.name)).toString();
					switch (file.type) {
						case IMAGE:
							imageStore.store(file.path, uploadPath,
											 s -> indexed.content.attachments.add(new Content.Attachment(file.type, file.name, s)));
							break;
						default:
							attachmentStore.store(file.path, uploadPath,
												  s -> indexed.content.attachments.add(new Content.Attachment(file.type, file.name, s)));
					}
				} finally {
					// cleanup file once uploaded
					Files.deleteIfExists(file.path);
				}
			}

			// TODO KW 20181015 - don't do this - updates any updates not involving a re-index will wipe attachments out
			// delete removed attachments from remote
//			if (current != null) {
//				for (Content.Attachment had : current.content.attachments) {
//					if (!indexed.content.attachments.contains(had)) {
//						switch (had.type) {
//							case IMAGE:
//								imageStore.delete(had.url, d -> {
//								});
//								break;
//							default:
//								attachmentStore.delete(had.url, d -> {
//								});
//						}
//					}
//				}
//			}

			if (indexed.content.downloads.stream().noneMatch(d -> d.main)) {
				String uploadPath = path.relativize(next.resolve(submission.filePath.getFileName())).toString();
				contentStore.store(submission.filePath, uploadPath,
								   s -> indexed.content.downloads.add(new Content.Download(s, true, LocalDate.now(), LocalDate.now(),
																						   true, false, false))
				);
			}

			Path newYml = next.resolve(String.format("%s_[%s].yml", indexed.content.name, indexed.content.hash.substring(0, 8)));
			Files.write(newYml, YAML.toString(indexed.content).getBytes(StandardCharsets.UTF_8),
						StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

			if (current != null && !current.path.equals(newYml)) {
				// remove old yml file if new file changed
				Files.deleteIfExists(current.path);
			}

			this.content.put(indexed.content.hash, new ContentHolder(newYml, indexed.content));
			this.changes.add(indexed.content.hash);

			return true;
		}
		return false;
	}

	private static class ContentHolder {

		private final Path path;
		private final Content content;

		public ContentHolder(Path path, Content content) {
			this.path = path;
			this.content = content;
		}
	}
}
