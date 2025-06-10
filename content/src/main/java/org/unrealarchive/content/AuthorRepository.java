package org.unrealarchive.content;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.unrealarchive.common.Util;
import org.unrealarchive.common.YAML;

public interface AuthorRepository {

	Author UNKNOWN = new Author("Unknown");
	Author VARIOUS = new Author("Various");

	int size();

	Collection<Author> all();

	Author byName(String name);

	/**
	 * Add an author to the repository.
	 * <p>
	 * If ephemeral is true, the author will not be persisted.
	 */
	void put(Author author, boolean ephemeral) throws IOException;

	/**
	 * Copy assets (title images, etc) to the specified `outPath`
	 */
	void writeContent(Author author, Path outPath) throws IOException;

	default public String summary() {
		StringBuilder result = new StringBuilder();

		result.append("Authors Repository Totals: ").append(System.lineSeparator());
		result.append(" > Total: ").append(size()).append(System.lineSeparator());
		result.append(" > Current: ").append(all().size()).append(System.lineSeparator());
		result.append(" > Aliases: ").append(all().stream().mapToInt(a -> a.aliases.size()).sum()).append(System.lineSeparator());

		return result.toString();
	}

	static String authorKey(String name) {
		return Util.normalised(name).toLowerCase().replaceAll("[\"`()\\[\\]<>{}=*-]", "'").strip();
	}

	/**
	 * Implementation of `AuthorRepository` using local files for storage.
	 */
	public static class FileRepository implements AuthorRepository {

		private static final Set<String> DEFAULT_ICONS = Set.of("icon.png", "icon.jpg", "icon.jpeg", "icon.gif");
		private static final Set<String> DEFAULT_PROFILES = Set.of("profile.png", "profile.jpg", "profile.jpeg", "profile.gif");
		private static final Set<String> DEFAULT_COVERS = Set.of("cover.png", "cover.jpg", "cover.jpeg", "cover.gif");
		private static final Set<String> DEFAULT_BACKGROUNDS = Set.of(
			"background.png", "background.jpg", "background.jpeg", "background.gif",
			"bg.png", "bg.jpg", "bg.jpeg", "bg.gif"
		);

		private final Path path;

		private final Map<Author, AuthorHolder> authors = new ConcurrentHashMap<>();
		private final Map<String, AuthorHolder> authorsByKey = new ConcurrentHashMap<>();

		/**
		 * Initialize the repository and load authors from the given path.
		 *
		 * @param path Path to the root directory containing author data
		 * @throws IOException if loading authors or traversing the path fails
		 */
		public FileRepository(Path path) throws IOException {
			this.path = path;

			// Load authors from path into repository
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (Util.extension(file).equalsIgnoreCase("yml")) {
						Author author = YAML.fromFile(file, Author.class);
						AuthorHolder holder = new AuthorHolder(file, author, false);
						authors.put(author, holder);

						authorsByKey.put(authorKey(author.name), holder);
						author.aliases.forEach(alias -> authorsByKey.put(authorKey(alias), holder));

						/*
						 additional resource discovery if needed
						 */
						if (author.iconImage == null || author.iconImage.isBlank()) {
							author.iconImage = DEFAULT_ICONS.stream()
															.filter(f -> Files.exists(file.getParent().resolve(f)))
															.findFirst().orElse(null);
						}
						if (author.profileImage == null || author.profileImage.isBlank()) {
							author.profileImage = DEFAULT_PROFILES.stream()
																  .filter(f -> Files.exists(file.getParent().resolve(f)))
																  .findFirst().orElse(null);
						}
						if (author.coverImage == null || author.coverImage.isBlank()) {
							author.coverImage = DEFAULT_COVERS.stream()
															  .filter(f -> Files.exists(file.getParent().resolve(f)))
															  .findFirst().orElse(null);
						}
						if (author.bgImage == null || author.bgImage.isBlank()) {
							author.bgImage = DEFAULT_BACKGROUNDS.stream()
																.filter(f -> Files.exists(file.getParent().resolve(f)))
																.findFirst().orElse(null);
						}
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}

		@Override
		public int size() {
			return authors.size();
		}

		@Override
		public Collection<Author> all() {
			return authors.keySet().stream()
						  .filter(author -> !author.deleted)
						  .toList();
		}

		@Override
		public void put(Author author, boolean ephemeral) throws IOException {
			AuthorHolder current = authors.get(author);

			Path yml = Util.safeFileName(path.resolve(author.slug())).resolve("author.yml");

			if (!ephemeral) {
				if (!Files.isDirectory(yml.getParent())) Files.createDirectories(yml.getParent());
				Files.writeString(yml, YAML.toString(author), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			}

			// add to repo
			AuthorHolder holder = new AuthorHolder(yml, author, ephemeral);
			authors.put(author, holder);

			authorsByKey.put(authorKey(author.name), holder);
			author.aliases.forEach(alias -> authorsByKey.put(authorKey(alias), holder));

			// if it's a different file, remove the old one
			if (!ephemeral && current != null && !yml.equals(current.path)) {
				Files.delete(current.path);
			}
		}

		@Override
		public Author byName(String name) {
			if (name == null || name.isBlank() || name.equalsIgnoreCase(UNKNOWN.slug())) return UNKNOWN;
			if (name.equals(VARIOUS.slug())) return VARIOUS;

			AuthorHolder holder = authorsByKey.get(authorKey(name));
			if (holder != null) return holder.author;
			return null;
		}

		@Override
		public void writeContent(Author author, Path outPath) throws IOException {
			AuthorHolder holder = authors.get(author);

			if (holder == null || holder.path == null || !Files.exists(holder.path)) return;

			Util.copyTree(holder.path.getParent(), outPath);
		}

		@Override
		public String summary() {
			StringBuilder result = new StringBuilder();

			result.append(AuthorRepository.super.summary());

			result.append(" > Ephemeral Entries: ")
				  .append(authors.values().stream().filter(a -> a.ephemeral).count())
				  .append(System.lineSeparator());
			result.append(" > Ephemeral aliases: ")
				  .append(authors.values().stream().filter(a -> a.ephemeral).mapToLong(a -> a.author.aliases.size()).sum())
				  .append(System.lineSeparator());

			return result.toString();
		}

		private record AuthorHolder(Path path, Author author, boolean ephemeral) {
		}
	}
}