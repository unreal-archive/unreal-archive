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
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.CLI;
import net.shrimpworks.unreal.archive.Platform;
import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.YAML;
import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.Games;
import net.shrimpworks.unreal.archive.content.IndexUtils;
import net.shrimpworks.unreal.archive.storage.DataStore;

public class ManagedContentManager {

	private final Path root;

	private final Map<Managed, ManagedContentHolder> content;

	public ManagedContentManager(Path root) throws IOException {
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

	public void addFile(DataStore contentStore, Games game, String group, String path, String title, Path localFile, CLI cli)
		throws IOException {
		ManagedContentHolder managed = findManaged(game, group, path, title)
			.or(() -> {
				try {
					init(game, group, path, title, "template-minimal.md", newGt -> {
						newGt.downloads.clear();
						newGt.links.clear();
						newGt.description = "";
						newGt.titleImage = "";
					});
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				return findManaged(game, group, path, title);
			}).orElseThrow();

		Managed.ManagedFile dl = new Managed.ManagedFile();
		dl.title = cli.option("title", title);
		dl.localFile = localFile.toString();
		dl.originalFilename = Util.fileName(localFile);
		dl.version = cli.option("version", title);
		dl.description = cli.option("description", dl.description);
		dl.platform = Platform.valueOf(cli.option("platform", Platform.ANY.toString()));

		managed.managed.downloads.add(dl);

		// update with the release
		Files.write(managed.path, YAML.toString(managed.managed).getBytes(StandardCharsets.UTF_8),
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	public void sync(DataStore contentStore, CLI cli, BiConsumer<Integer, Integer> progress) {
		Set<Managed> synced = sync(contentStore, progress);

		if (synced.isEmpty()) {
			System.out.println("No files were synced.");
		} else {
			System.out.printf("Synced %d files:%n", synced.size());
			synced.forEach(m -> System.out.printf(" - %s%n", m.title));
		}
	}

	public Path init(Games game, String group, String path, String title) throws IOException {
		return init(game, group, path, title, "template.md", gt -> {
		});
	}

	private Path init(Games game, String group, String path, String title, String template, Consumer<Managed> initialised)
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
		man.author = IndexUtils.UNKNOWN;
		man.document = "readme.md";
		man.titleImage = "title.png";

		Managed.ManagedFile sampleFile = new Managed.ManagedFile();
		sampleFile.title = neatName + " Download";
		sampleFile.version = "1.0";
		sampleFile.localFile = "/path/to/file.zip";
		man.downloads.add(sampleFile);

		initialised.accept(man);

		final Path outPath = Files.createDirectories(man.contentPath(root));

		Path yml = Util.safeFileName(outPath.resolve("managed.yml"));
		Path md = Util.safeFileName(outPath.resolve("readme.md"));

		if (!Files.exists(yml)) {
			Files.write(yml, YAML.toString(man).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
		}

		if (!Files.exists(md)) {
			Files.copy(Managed.class.getResourceAsStream(template), md);
		}

		content.put(man, new ManagedContentHolder(yml, man));

		return outPath;
	}

	public Path managedPath(Games game, String group, String path, String title) {
		return root.resolve(Util.slug(group)).resolve(game.name).resolve(path).resolve(Util.slug(title));
	}

	private Set<Managed> sync(DataStore contentStore, BiConsumer<Integer, Integer> progress) {
		Set<Managed> synced = new HashSet<>();

		// collect items to be synced
		Set<ManagedContentHolder> toSync = content.values().stream()
												  .filter(m -> m.managed.downloads.stream().anyMatch(d -> {
													  Path f = m.path.resolve(d.localFile);
													  return !d.synced && Files.exists(f);
												  }))
												  .collect(Collectors.toSet());

		long total = toSync.stream().mapToLong(m -> m.managed.downloads.stream().filter(d -> !d.synced).count()).sum();
		AtomicInteger counter = new AtomicInteger();

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

				progress.accept((int)total, counter.incrementAndGet());
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

	private Path path(Managed managed) {
		return content.get(managed).path;
	}

	private String remotePath(Managed managed) {
		return String.join("/", managed.contentType(), managed.game, managed.path);
	}

	private Optional<ManagedContentHolder> findManaged(Games game, String group, String path, String title) {
		return content.values().stream()
					  .filter(m -> !m.managed.deleted())
					  .filter(m -> m.managed.game().equalsIgnoreCase(game.name))
					  .filter(m -> m.managed.group.equalsIgnoreCase(group))
					  .filter(m -> m.managed.path.equalsIgnoreCase(path))
					  .filter(m -> m.managed.title.equalsIgnoreCase(title))
					  .findFirst();
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
