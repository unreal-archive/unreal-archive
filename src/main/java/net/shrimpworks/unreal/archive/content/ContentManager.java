package net.shrimpworks.unreal.archive.content;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.YAML;
import net.shrimpworks.unreal.archive.storage.DataStore;

public class ContentManager {

	private final Path path;
	private final Map<String, ContentHolder> content;

	private final Map<String, Collection<Content>> contentFileMap;
	private final Map<String, Collection<Content>> variationsMap;

	private final DataStore contentStore;
	private final DataStore imageStore;
	private final DataStore attachmentStore;

	private final Set<String> changes;

	public ContentManager(Path path, DataStore contentStore, DataStore imageStore, DataStore attachmentStore) throws IOException {
		this.path = path;
		this.contentStore = contentStore;
		this.imageStore = imageStore;
		this.attachmentStore = attachmentStore;
		this.content = new ConcurrentHashMap<>();
		this.contentFileMap = new ConcurrentHashMap<>();
		this.variationsMap = new ConcurrentHashMap<>();

		this.changes = new HashSet<>();

		try (Stream<Path> files = Files.walk(path).parallel().filter(file -> Util.extension(file).equalsIgnoreCase("yml"))) {
			files.forEach(file -> {
				try {
					Content c = YAML.fromFile(file, Content.class);
					content.put(c.hash, new ContentHolder(file, c));

					// while reading this content, also index its individual files for later quick lookup
					for (Content.ContentFile contentFile : c.files) {
						Collection<Content> fileSet = contentFileMap.computeIfAbsent(contentFile.hash, h -> ConcurrentHashMap.newKeySet());
						fileSet.add(c);
					}

					if (c.variationOf != null) {
						Collection<Content> variations = variationsMap.computeIfAbsent(c.variationOf, h -> ConcurrentHashMap.newKeySet());
						variations.add(c);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	public int size() {
		return content.size();
	}

	public long fileSize() {
		return content.values().parallelStream().mapToLong(c -> c.content.fileSize).sum();
	}

	public Map<Class<? extends Content>, Long> countByType() {
		return content.values().stream()
					  .filter(c -> c.content.variationOf == null && !c.content.deleted)
					  .collect(Collectors.groupingBy(v -> v.content.getClass(), Collectors.counting()));
	}

	public Map<String, Long> countByGame() {
		return content.values().stream()
					  .filter(c -> c.content.variationOf == null && !c.content.deleted)
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
	public Collection<Content> containingFile(String hash) {
		return contentFileMap.getOrDefault(hash, Collections.emptySet());
	}

	/**
	 * Return all content which is flagged as a variation of the provided
	 * content hash.
	 *
	 * @param hash content hash
	 * @return content variations for the content specified by the hash
	 */
	public Collection<Content> variationsOf(String hash) {
		return variationsMap.getOrDefault(hash, Collections.emptySet());
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

		if (current == null || (!indexed.content.equals(current.content) || !indexed.files.isEmpty())) {
			// lets store the content \o/
			Path next = indexed.content.contentPath(path);
			Files.createDirectories(next);

			for (IndexResult.NewAttachment file : indexed.files) {
				// use same path structure as per contentPath
				try {
					String uploadPath = path.relativize(next.resolve(file.name)).toString();
					if (file.type == Content.AttachmentType.IMAGE) {
						imageStore.store(file.path, uploadPath, (fileUrl, ex) ->
								indexed.content.attachments.add(new Content.Attachment(file.type, file.name, fileUrl)));
					} else {
						attachmentStore.store(file.path, uploadPath, (fileUrl, ex) ->
								indexed.content.attachments.add(new Content.Attachment(file.type, file.name, fileUrl)));
					}
				} finally {
					// cleanup file once uploaded
					Files.deleteIfExists(file.path);
				}
			}

			// TODO KW 20181015 - don't do this - any updates not involving a re-index will wipe attachments out
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

			if (submission != null && indexed.content.downloads.stream().noneMatch(d -> d.main)) {
				String uploadPath = path.relativize(next.resolve(submission.filePath.getFileName())).toString();
				contentStore.store(submission.filePath, uploadPath, (fileUrl, ex) ->
						indexed.content.downloads.add(new Content.Download(fileUrl, true, false, Content.DownloadState.OK))
				);
			}

			Path newYml = next.resolve(String.format("%s_[%s].yml", indexed.content.name, indexed.content.hash.substring(0, 8)));
			Files.write(Util.safeFileName(newYml), YAML.toString(indexed.content).getBytes(StandardCharsets.UTF_8),
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
