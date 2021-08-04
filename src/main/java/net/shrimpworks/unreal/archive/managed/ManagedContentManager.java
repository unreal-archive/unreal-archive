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
import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.storage.DataStore;

public class ManagedContentManager {

	private final Map<Managed, ManagedContentHolder> content;

	public ManagedContentManager(Path path) throws IOException {
		this.content = new HashMap<>();

		// load contents from path into content
		Files.walkFileTree(path, new SimpleFileVisitor<>() {
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
	 * @param managed document to get root path for
	 * @return document root path
	 */
	public Path contentRoot(Managed managed) {
		ManagedContentHolder holder = content.get(managed);
		if (holder == null) return null;

		return holder.path.getParent();
	}

	private Path path(Managed managed) {
		return content.get(managed).path;
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
					storeDownloadFile(contentStore, clone, d, f, success);
				} catch (IOException e) {
					throw new RuntimeException(String.format("Failed to sync file %s: %s%n", d.localFile, e));
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

	public void storeDownloadFile(DataStore contentStore, Managed managed, Managed.ManagedFile file, Path localFile, boolean[] success)
			throws IOException {
		contentStore.store(localFile, String.join("/", remotePath(managed), localFile.getFileName().toString()), (url, ex) -> {
			try {
				// record download
				if (file.downloads.stream().noneMatch(dl -> dl.url.equals(url))) {
					file.downloads.add(new Content.Download(url, !file.synced, false, Content.DownloadState.OK));
				}

				// other file stats (the null checks are added to populate fields added post initial implementation)
				if (!file.synced || file.hash == null || file.originalFilename == null) {
					file.fileSize = Files.size(localFile);
					file.hash = Util.hash(localFile);
					file.originalFilename = Util.fileName(localFile);
					file.synced = true;
				}

				// replace existing with updated
				Files.write(path(managed), YAML.toString(managed).getBytes(StandardCharsets.UTF_8),
							StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

				success[0] = true;
			} catch (IOException e) {
				throw new RuntimeException(String.format("Failed to update managed content definition %s: %s%n", path(managed), e));
			}
		});
	}

	private String remotePath(Managed managed) {
		return String.join("/", managed.contentType(), managed.game, managed.path);
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
