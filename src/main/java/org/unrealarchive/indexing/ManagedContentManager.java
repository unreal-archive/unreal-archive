package org.unrealarchive.indexing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.unrealarchive.common.Platform;
import org.unrealarchive.common.Util;
import org.unrealarchive.common.YAML;
import org.unrealarchive.content.Download;
import org.unrealarchive.content.Games;
import org.unrealarchive.content.managed.Managed;
import org.unrealarchive.content.managed.ManagedContentRepository;
import org.unrealarchive.storage.DataStore;

public class ManagedContentManager {

	private final ManagedContentRepository repo;
	private final DataStore contentStore;

	public ManagedContentManager(ManagedContentRepository repo, DataStore contentStore) {
		this.repo = repo;
		this.contentStore = contentStore;
	}

	public Managed checkout(Managed managed) {
		try {
			return YAML.fromString(YAML.toString(managed), Managed.class);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot clone content " + managed.name());
		}
	}

	public void checkin(Managed managed) {
		try {
			repo.put(managed);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot store content " + managed.name());
		}
	}

	public void addFile(DataStore contentStore, Games game, String group, String path, String title, Path localFile,
						Map<String, String> params)
		throws IOException {
		Managed managed = Optional.ofNullable(repo.findManaged(game, group, path, title))
								  .or(() -> {
									  try {
										  repo.create(game, group, path, title, newManaged -> {
											  newManaged.downloads.clear();
											  newManaged.links.clear();
											  newManaged.description = "";
											  newManaged.titleImage = "";
										  });
									  } catch (Exception e) {
										  throw new RuntimeException(e);
									  }
									  return Optional.ofNullable(repo.findManaged(game, group, path, title));
								  }).orElseThrow();

		Managed.ManagedFile dl = new Managed.ManagedFile();
		dl.title = params.getOrDefault("title", title);
		dl.localFile = localFile.toString();
		dl.originalFilename = Util.fileName(localFile);
		dl.version = params.getOrDefault("version", title);
		dl.description = params.getOrDefault("description", dl.description);
		dl.platform = Platform.valueOf(params.getOrDefault("platform", Platform.ANY.toString()));

		managed.downloads.add(dl);

		repo.put(managed);
	}

	public void sync(BiConsumer<Integer, Integer> progress) {
		// collect items to be synced
		Set<Managed> toSync = repo.all()
								  .stream()
								  .filter(m -> m.downloads.stream().anyMatch(d -> {
									  Path f = Paths.get(d.localFile);
									  return !d.synced && Files.exists(f);
								  }))
								  .collect(Collectors.toSet());

		long total = toSync.stream().mapToLong(m -> m.downloads.stream().filter(d -> !d.synced).count()).sum();
		AtomicInteger counter = new AtomicInteger();

		toSync.forEach(m -> {
			Managed clone = checkout(m);

			boolean[] success = { false };

			clone.downloads.stream().filter(d -> !d.synced).forEach(d -> {
				Path f = Paths.get(d.localFile);
				if (!Files.exists(f)) throw new IllegalArgumentException(String.format("Local file %s not found!", d.localFile));

				try {
					storeDownloadFile(clone, d, f, success);
				} catch (IOException e) {
					throw new RuntimeException(String.format("Failed to sync file %s: %s%n", d.localFile, e));
				}

				progress.accept((int)total, counter.incrementAndGet());
			});

			if (success[0]) checkin(clone);
		});
	}

	public void storeDownloadFile(Managed managed, Managed.ManagedFile file, Path localFile, boolean[] success)
		throws IOException {
		contentStore.store(localFile, String.join("/", remotePath(managed), localFile.getFileName().toString()), (url, ex) -> {
			try {
				// record download
				if (file.downloads.stream().noneMatch(dl -> dl.url.equals(url))) {
					file.downloads.add(new Download(url, true, Download.DownloadState.OK));
				}

				// other file stats (the null checks are added to populate fields added post initial implementation)
				if (!file.synced || file.hash == null || file.originalFilename == null) {
					file.fileSize = Files.size(localFile);
					file.hash = Util.hash(localFile);
					file.originalFilename = Util.fileName(localFile);
					file.synced = true;
				}

				success[0] = true;
			} catch (IOException e) {
				throw new RuntimeException(String.format("Failed to update managed content definition %s: %s%n", managed.name(), e));
			}
		});
	}

	private String remotePath(Managed managed) {
		return String.join("/", managed.contentType(), managed.game, managed.path);
	}

}
