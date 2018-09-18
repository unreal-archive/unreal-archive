package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ContentManager {

	private final Path path;
	private final Map<String, Content> content;

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
					content.put(c.hash, c);
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}

	public Map<Class<? extends Content>, Long> countByType() {
		return content.values().stream()
					  .collect(Collectors.groupingBy(Content::getClass, Collectors.counting()));
	}

	public Map<String, Long> countByGame() {
		return content.values().stream()
					  .collect(Collectors.groupingBy(v -> v.game, Collectors.counting()));
	}

	// intent: when some content is going to be worked on, a clone is checked out.
	// when its checked out, its hash (immutable) is stored in the out collection.
	// after its been modified or left alone, the clone is checked in.
	// during check-in, if the the clone is no longer equal to the original, something changed.
	// if something changed, the content will be written out, within a new directory structure if needed
	// and the old file will be removed

	@SuppressWarnings("unchecked")
	public Content checkout(String hash) {
		Content out = this.content.get(hash);
		if (out != null) {
			try {
				return YAML.fromString(YAML.toString(out), Content.class);
			} catch (IOException e) {
				throw new IllegalStateException("Cannot clone content " + out);
			}
		}
		return null;
	}

	public boolean checkin(IndexResult<? extends Content> indexed) throws IOException {
		Content current = this.content.get(indexed.content.hash);
		if (!indexed.content.equals(current)) {
			Path prior = null;
			if (current != null) {
				prior = indexed.content.contentPath(path);
			}

			// lets store the content \o/
			Path next = indexed.content.contentPath(path);
			Files.createDirectories(next);

			if (prior != null) {
				// TODO copy old content to new directory
			}

			Files.write(next.resolve(indexed.content.name + ".yml"), YAML.toString(indexed.content).getBytes(StandardCharsets.UTF_8),
						StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

			for (IndexResult.CreatedFile file : indexed.files) {
				Files.move(file.path, next.resolve(file.name), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			}

			if (prior != null) {
				// TODO recursively clean out the old directory and remove it
			}

			this.content.put(indexed.content.hash, indexed.content);
			this.changes.add(indexed.content.hash);

			return true;
		}
		return false;
	}
}
