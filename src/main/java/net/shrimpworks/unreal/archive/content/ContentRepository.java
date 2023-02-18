package net.shrimpworks.unreal.archive.content;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.shrimpworks.unreal.archive.common.Util;
import net.shrimpworks.unreal.archive.common.YAML;

public interface ContentRepository {

	/**
	 * The root content path.
	 * <p>
	 * May not apply to all repository implementations. Figure that out.
	 */
	public Path path();

	/**
	 * Get the total number of content elements in the repository.
	 */
	public int size();

	/**
	 * Get the total file size of the repository contents.
	 * <p>
	 * This is the size of the content items, not the size of the repository metadata itself.
	 */
	public long fileSize();

	/**
	 * Get the total number of items by content type.
	 */
	public Map<Class<? extends Content>, Long> countByType();

	/**
	 * Get the total number of items by content type, for the game specified.
	 */
	public Map<Class<? extends Content>, Long> countByType(String game);

	/**
	 * Get the total number of content items by game.
	 */
	public Map<String, Long> countByGame();

	/**
	 * Search the repository for content items matching one or more of the provided attributes.
	 * <p>
	 * Attributes may be null to omit from filtering.
	 */
	public Collection<Content> search(String game, String type, String name, String author);

	/**
	 * Get all content items in the repository - excluding deleted items, but including variations.
	 */
	public Collection<Content> all();

	/**
	 * Get all content items in the repository, excluding deleted items, optionally including variations.
	 */
	public Collection<Content> all(boolean withVariations);

	/**
	 * Find all content items in the repository matching the name provided (exact, case-insensitive).
	 * <p>
	 * Results may include deleted items and variations.
	 */
	public Collection<Content> forName(String name);

	/**
	 * Get the content item matching the hash provided.
	 * <p>
	 * May return deleted or variant items.
	 */
	public Content forHash(String hash);

	/**
	 * Get all content items of type provided, including deleted items and variations.
	 */
	public <T extends Content> Collection<T> get(Class<T> type);

	/**
	 * Get all content items of type provided, optionally including deleted items and variations.
	 */
	public <T extends Content> Collection<T> get(Class<T> type, boolean withDeleted, boolean withVariations);

	/**
	 * Convenience which return the count of all content which contains
	 * the provided file hash.
	 *
	 * @param hash file hash
	 * @return count of content items containing the hash
	 */
	public int containingFileCount(String hash);

	/**
	 * Return all content which contains the provided file hash.
	 *
	 * @param hash file hash
	 * @return content containing the hash
	 */
	public Collection<Content> containingFile(String hash);

	/**
	 * Return all content which is flagged as a variation of the provided
	 * content hash.
	 *
	 * @param hash content hash
	 * @return content variations for the content specified by the hash
	 */
	public Collection<Content> variationsOf(String hash);

	/**
	 * Add a content item to the repository. Will replace existing items matching the item's hash.
	 */
	public void put(Content added) throws IOException;

	public static class FileRepository implements ContentRepository {

		private static final int CONTENT_INITIAL_SIZE = 60000;
		private static final int FILES_INITIAL_SIZE = CONTENT_INITIAL_SIZE * 3;
		private static final int VARIATION_INITIAL_SIZE = CONTENT_INITIAL_SIZE / 10;

		private final Path path;
		private final Map<String, ContentHolder> content;

		private final Map<String, Collection<ContentHolder>> contentFileMap;
		private final Map<String, Collection<ContentHolder>> variationsMap;

		public FileRepository(Path path) throws IOException {
			this.path = path;
			this.content = new ConcurrentHashMap<>(CONTENT_INITIAL_SIZE);
			this.contentFileMap = new ConcurrentHashMap<>(FILES_INITIAL_SIZE);
			this.variationsMap = new ConcurrentHashMap<>(VARIATION_INITIAL_SIZE);

			try (Stream<Path> files = Files.find(path, 20, (file, attr) -> file.toString().endsWith(".yml")).parallel()) {
				files.forEach(file -> {
					try {
						Content c = YAML.fromFile(file, Content.class);
						ContentHolder holder = new ContentHolder(file, c);
						content.put(c.hash, holder);

						// while reading this content, also index its individual files for later quick lookup
						for (Content.ContentFile contentFile : c.files) {
							Collection<ContentHolder> fileSet = contentFileMap.computeIfAbsent(contentFile.hash,
																							   h -> ConcurrentHashMap.newKeySet());
							fileSet.add(holder);
						}

						if (c.variationOf != null) {
							Collection<ContentHolder> variations = variationsMap.computeIfAbsent(c.variationOf,
																								 h -> ConcurrentHashMap.newKeySet());
							variations.add(holder);
						}
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
			}
		}

		@Override
		public Path path() {
			return path;
		}

		@Override
		public int size() {
			return content.size();
		}

		@Override
		public long fileSize() {
			return content.values().parallelStream().mapToLong(c -> c.fileSize).sum();
		}

		@Override
		public Map<Class<? extends Content>, Long> countByType() {
			return countByType(null);
		}

		@Override
		public Map<Class<? extends Content>, Long> countByType(String game) {
			return content.values().parallelStream()
						  .filter(c -> !c.isVariation && !c.deleted)
						  .filter(c -> game == null || c.content().game.equals(game))
						  .collect(Collectors.groupingBy(v -> v.content().getClass(), Collectors.counting()));
		}

		@Override
		public Map<String, Long> countByGame() {
			return content.values().parallelStream()
						  .filter(c -> !c.isVariation && !c.deleted)
						  .collect(Collectors.groupingBy(v -> v.content().game, Collectors.counting()));
		}

		@Override
		public Collection<Content> search(String game, String type, String name, String author) {
			return content.values().parallelStream()
						  .map(ContentHolder::content)
						  .filter(Objects::nonNull)
						  .filter(c -> {
							  boolean match = (game == null || c.game.equalsIgnoreCase(game));
							  match = match && (type == null || c.contentType.equalsIgnoreCase(type));
							  match = match && (author == null || c.author.toLowerCase().contains(author.toLowerCase()));
							  match = match && (name == null || c.name.toLowerCase().contains(name.toLowerCase()));
							  return match;
						  })
						  .collect(Collectors.toSet());
		}

		@Override
		public Collection<Content> all() {
			return all(true);
		}

		@Override
		public Collection<Content> all(boolean withVariations) {
			return content.values().parallelStream()
						  .filter(c -> !c.deleted)
						  .filter(c -> withVariations || !c.isVariation)
						  .map(ContentHolder::content)
						  .filter(Objects::nonNull)
						  .collect(Collectors.toSet());
		}

		@Override
		public Collection<Content> forName(String name) {
			return content.values().parallelStream()
						  .map(ContentHolder::content)
						  .filter(Objects::nonNull)
						  .filter(c -> c.name.equalsIgnoreCase(name))
						  .collect(Collectors.toSet());
		}

		@Override
		public Content forHash(String hash) {
			ContentHolder contentHolder = content.get(hash);
			if (contentHolder != null) return contentHolder.content();

			return null;
		}

		@Override
		public <T extends Content> Collection<T> get(Class<T> type) {
			return get(type, true, true);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T extends Content> Collection<T> get(Class<T> type, boolean withDeleted, boolean withVariations) {
			return content.values().parallelStream()
						  .filter(c -> type.isAssignableFrom(c.type))
						  .filter(c -> withDeleted || !c.deleted)
						  .filter(c -> withVariations || !c.isVariation)
						  .map(ContentHolder::content)
						  .filter(Objects::nonNull)
						  .map(c -> (T)c)
						  .collect(Collectors.toSet());
		}

		@Override
		public int containingFileCount(String hash) {
			return contentFileMap.getOrDefault(hash, Collections.emptySet()).size();
		}

		@Override
		public Collection<Content> containingFile(String hash) {
			return contentFileMap.getOrDefault(hash, Collections.emptySet())
								 .parallelStream().map(ContentHolder::content)
								 .filter(Objects::nonNull)
								 .collect(Collectors.toSet());
		}

		@Override
		public Collection<Content> variationsOf(String hash) {
			return variationsMap.getOrDefault(hash, Collections.emptySet())
								.parallelStream().map(ContentHolder::content)
								.filter(Objects::nonNull)
								.collect(Collectors.toSet());
		}

		@Override
		public void put(Content added) throws IOException {
			ContentHolder replaces = content.get(added.hash);

			Path outPath = added.contentPath(path);
			Files.createDirectories(outPath);

			Path newYml = outPath.resolve(String.format("%s_[%s].yml", Util.slug(added.name), added.hash.substring(0, 8)));
			Files.writeString(Util.safeFileName(newYml), YAML.toString(added),
							  StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

			if (replaces != null && !replaces.path.equals(newYml)) {
				// remove old yml file if new file changed
				Files.deleteIfExists(replaces.path);
			}

			this.content.put(added.hash, new ContentHolder(newYml, added));
		}

		private static class ContentHolder {

			private final Path path;
			private final boolean deleted;
			private final boolean isVariation;
			private final int fileSize;
			private final Class<?> type;
			private SoftReference<Content> content;

			public ContentHolder(Path path, Content content) {
				this.path = path;
				this.deleted = content.deleted();
				this.isVariation = content.isVariation();
				this.fileSize = content.fileSize;
				this.type = content.getClass();
				this.content = !deleted && !isVariation ? new SoftReference<>(content) : null;
			}

			public Content content() {
				Content has = content == null ? null : content.get();
				if (has != null) {
					return has;
				} else {
					try {
						// this in itself will cause a lot of object churn
						Content newContent = YAML.fromFile(path, Content.class);
						this.content = new SoftReference<>(newContent);
						return newContent;
					} catch (IOException e) {
						e.printStackTrace(System.err);
					}
				}
				return null;
			}
		}
	}
}