package net.shrimpworks.unreal.archive.managed;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.YAML;
import net.shrimpworks.unreal.archive.storage.DataStore;

public class ManagedContentManager {

	private final Map<Managed, ManagedContentHolder> content;
	private final String name;

	public ManagedContentManager(Path path, String groupName) throws IOException {
		this.content = new HashMap<>();
		this.name = groupName;

		// load contents from path into content
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
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

	public String name() {
		return name;
	}

	/**
	 * Get the total document count.
	 *
	 * @return number of documents
	 */
	public int size() {
		return content.size();
	}

	/**
	 * Get all known documents' metadata.
	 *
	 * @return copy of the document collection
	 */
	public Collection<Managed> all() {
		return Collections.unmodifiableCollection(content.keySet());
	}

	/**
	 * Get the raw text content of the document as a channel.
	 *
	 * @param man document to retrieve
	 * @return document content
	 * @throws IOException failed to open the document
	 */
	public ReadableByteChannel document(Managed man) throws IOException {
		ManagedContentHolder holder = content.get(man);
		if (holder == null) return null;

		Path docPath = holder.path.getParent().resolve(man.document);

		if (!Files.exists(docPath)) return null;

		return Files.newByteChannel(docPath, StandardOpenOption.READ);
	}

	/**
	 * Get the on-disk location of the document metadata.
	 * <p>
	 * This can be used to access files intended to be bundled with
	 * the document.
	 *
	 * @param man document to get root path for
	 * @return document root path
	 */
	public Path contentRoot(Managed man) {
		ManagedContentHolder holder = content.get(man);
		if (holder == null) return null;

		return holder.path.getParent();
	}

	public Set<Managed> sync(DataStore contentStore) {
		Set<Managed> synced = new HashSet<>();

		// collect items to be synced
		Set<ManagedContentHolder> toSync = content.values().stream()
												  .filter(m -> m.managed.downloads.stream().anyMatch(d -> {
													  Path f = m.path.resolve(d.localFile);
													  return !d.synced && Files.exists(f);
												  }))
												  .collect(Collectors.toSet());

		toSync.forEach(m -> {
			Managed clone;
			try {
				clone = YAML.fromString(YAML.toString(m.managed), Managed.class);
			} catch (IOException e) {
				throw new IllegalStateException("Cannot clone managed content " + m.managed);
			}

			boolean[] success = { false };

			clone.downloads.stream().filter(d -> !d.synced).forEach(d -> {
				Path f = m.path.resolve(d.localFile);
				if (!Files.exists(f)) throw new IllegalArgumentException(String.format("Local file %s not found!", d.localFile));

				try {
					contentStore.store(f, String.join("/", remotePath(m.managed), f.getFileName().toString()), (url, ex) -> {
						try {
							if (!d.downloads.contains(url)) d.downloads.add(url);
							d.fileSize = Files.size(f);
							d.synced = true;

							// replace existing with updated
							Files.write(m.path, YAML.toString(clone).getBytes(StandardCharsets.UTF_8),
										StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

							success[0] = true;
						} catch (IOException e) {
							throw new RuntimeException(String.format("Failed to update managed content definition %s: %s%n",
																	 m.path, e.toString()));
						}

					});
				} catch (IOException e) {
					throw new RuntimeException(String.format("Failed to sync file %s: %s%n", d.localFile, e.toString()));
				}
			});

			if (success[0]) {
				content.remove(m.managed);
				content.put(clone, new ManagedContentHolder(m.path, clone));
				synced.add(clone);
			}
		});

		return synced;
	}

	private String remotePath(Managed managed) {
		return String.join("/", name, managed.game, managed.path);
	}

	private static class ManagedContentHolder {

		private final Path path;
		private final Managed managed;

		public ManagedContentHolder(Path path, Managed managed) {
			this.path = path;
			this.managed = managed;
		}
	}
}
