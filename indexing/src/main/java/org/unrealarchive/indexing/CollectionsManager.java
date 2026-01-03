package org.unrealarchive.indexing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import org.unrealarchive.common.ArchiveUtil;
import org.unrealarchive.common.Platform;
import org.unrealarchive.common.Util;
import org.unrealarchive.common.YAML;
import org.unrealarchive.content.CollectionsRepository;
import org.unrealarchive.content.ContentCollection;
import org.unrealarchive.content.ContentEntity;
import org.unrealarchive.content.Download;
import org.unrealarchive.content.RepositoryManager;
import org.unrealarchive.content.addons.Addon;
import org.unrealarchive.content.addons.GameType;
import org.unrealarchive.content.managed.Managed;
import org.unrealarchive.storage.DataStore;

public class CollectionsManager {

	private static final String REMOTE_ROOT = "collections";

	private final RepositoryManager repositoryManager;
	private final CollectionsRepository repo;
	private final DataStore contentStore;

	public CollectionsManager(RepositoryManager repositoryManager, CollectionsRepository repo, DataStore contentStore) {
		this.repositoryManager = repositoryManager;
		this.repo = repo;
		this.contentStore = contentStore;
	}

	public CollectionsRepository repo() {
		return repo;
	}

	public ContentCollection checkout(ContentCollection collection) {
		try {
			return YAML.fromString(YAML.toString(collection), ContentCollection.class);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot clone content " + collection.name());
		}
	}

	public void checkin(ContentCollection collection) {
		try {
			repo.put(collection);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot store content " + collection.name());
		}
	}

	public void putFile(ContentCollection collection, Path sourceFile) throws IOException {
		repo.putFile(collection, sourceFile);
	}

	public Path createArchive(ContentCollection collection, Platform platform) throws IOException {
		Path tmpDir = Files.createTempDirectory("ua-collection-" + Util.slug(collection.title));
		try {
			for (ContentCollection.CollectionItem item : collection.items) {
				ContentEntity<?> content = repositoryManager.forId(item.id);
				if (content == null) continue;

				Download download = null;
				String filename = null;

				switch (content) {
					case Managed managed -> {
						// find a platform-specific file, or use "ANY" if none available
						Managed.ManagedFile file = managed.downloads.stream()
																	.filter(f -> f.platform == (platform != null ? platform : Platform.ANY))
																	.findFirst()
																	.orElse(managed.downloads.stream()
																							 .filter(f -> f.platform == Platform.ANY)
																							 .findFirst()
																							 .orElse(managed.downloads.isEmpty()
																										 ? null
																										 : managed.downloads.getFirst())
																	);
						if (file != null) {
							download = file.directDownload();
							filename = file.originalFilename;
						}
					}
					case GameType gt -> {
						// find a platform-specific version of the latest release, or use "ANY" if none available
						GameType.Release release = gt.releases.stream().max(GameType.Release::compareTo).orElse(null);
						if (release != null) {
							GameType.ReleaseFile file = release.files.stream()
																	 .filter(
																		 f -> f.platform == (platform != null ? platform : Platform.ANY))
																	 .findFirst()
																	 .orElse(release.files.stream()
																						  .filter(f -> f.platform == Platform.ANY)
																						  .findFirst()
																						  .orElse(release.files.isEmpty()
																									  ? null
																									  : release.files.getFirst())
																	 );
							if (file != null) {
								download = file.directDownload();
								filename = file.originalFilename;
							}
						}
					}
					case Addon addon -> {
						download = addon.directDownload();
						if (download != null) {
							filename = addon.originalFilename;
							if (filename == null || filename.isBlank()) filename = Util.fileName(download.url);
						}
					}
					default -> {
						System.err.println("Unsupported content type in collection: " + content.getClass().getSimpleName());
					}
				}

				if (download != null && filename != null) {
					System.err.printf("Downloading %s from %s%n", filename, download.url);
					Util.downloadTo(download.url.replaceAll(" ", "%20"), tmpDir.resolve(filename));
				}
			}

			String archiveName = Util.slug(collection.title) + ".zip";
			Path archivePath = Files.createTempDirectory("ua-collection").resolve(archiveName);
			ArchiveUtil.createZip(tmpDir, archivePath, Duration.ofMinutes(10));

			ContentCollection.CollectionArchive archive = new ContentCollection.CollectionArchive();
			archive.title = collection.title;
			archive.platform = platform != null ? platform : Platform.ANY;
			archive.localFile = archivePath.toString();
			archive.originalFilename = archiveName;
			archive.hash = Util.hash(archivePath);
			archive.fileSize = Files.size(archivePath);
			archive.synced = false;

			collection.archives.add(archive);

			return archivePath;
		} catch (InterruptedException e) {
			throw new IOException("Archive creation interrupted", e);
		} finally {
			ArchiveUtil.cleanPath(tmpDir);
		}
	}

	public void sync(ContentCollection collection) throws IOException {
		ContentCollection clone = checkout(collection);

		boolean[] success = { false };

		clone.archives.stream().filter(a -> !a.synced && !a.deleted).forEach(a -> {
			Path f = Paths.get(a.localFile);
			if (!Files.exists(f)) throw new IllegalArgumentException(String.format("Local file %s not found!", a.localFile));

			try {
				storeArchiveFile(clone, a, f, success);
			} catch (IOException e) {
				throw new RuntimeException(String.format("Failed to sync file %s: %s%n", a.localFile, e));
			}
		});

		if (success[0]) checkin(clone);
	}

	public void storeArchiveFile(ContentCollection collection, ContentCollection.CollectionArchive archive, Path localFile,
								  boolean[] success)
		throws IOException {
		contentStore.store(localFile, String.join("/", remotePath(collection), localFile.getFileName().toString()), (url, ex) -> {
			try {
				// record download
				if (archive.downloads.stream().noneMatch(dl -> dl.url.equals(url))) {
					archive.downloads.add(new Download(url, true, Download.DownloadState.OK));
				}

				// other file stats
				if (!archive.synced || archive.hash == null || archive.originalFilename == null) {
					archive.fileSize = Files.size(localFile);
					archive.hash = Util.hash(localFile);
					archive.originalFilename = Util.fileName(localFile);
					archive.synced = true;
				}

				success[0] = true;
			} catch (IOException e) {
				throw new RuntimeException(String.format("Failed to update collection definition %s: %s%n", collection.name(), e));
			}
		});
	}

	private String remotePath(ContentCollection collection) {
		return String.join("/", REMOTE_ROOT, Util.slug(collection.title));
	}
}
