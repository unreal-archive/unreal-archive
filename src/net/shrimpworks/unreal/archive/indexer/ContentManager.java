package net.shrimpworks.unreal.archive.indexer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.ArchiveUtil;
import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.YAML;

public class ContentManager {

	private final Path path;
	private final Map<String, ContentHolder> content;

	private final Set<String> changes;

	public ContentManager(Path path) throws IOException {
		this.path = path;
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

	public boolean checkin(IndexResult<? extends Content> indexed) throws IOException {
		ContentHolder current = this.content.get(indexed.content.hash);

		if (current == null || !indexed.content.equals(current.content)) {
			Path prior = null;
			if (current != null) {
				prior = current.path.getParent();
			}

			// lets store the content \o/
			Path next = indexed.content.contentPath(path);
			Files.createDirectories(next);

			if (prior != null) {
				// copy old content to new directory
				Set<Path> oldFiles = Files.list(prior).collect(Collectors.toSet());
				for (Path oldFile : oldFiles) {
					if (Files.isRegularFile(oldFile)) Files.move(oldFile, next.resolve(oldFile.getFileName()));
				}
			}

			Path newYml = next.resolve(indexed.content.name + ".yml");
			Files.write(newYml, YAML.toString(indexed.content).getBytes(StandardCharsets.UTF_8),
						StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

			for (IndexResult.CreatedFile file : indexed.files) {
				Files.move(file.path, next.resolve(file.name), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			}

			if (prior != null) {
				// clean out the old directory and remove it
				ArchiveUtil.cleanPath(prior);
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
