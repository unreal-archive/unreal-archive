package net.shrimpworks.unreal.archive.content;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.YAML;
import net.shrimpworks.unreal.archive.content.mappacks.MapPack;
import net.shrimpworks.unreal.archive.content.maps.GameTypes;
import net.shrimpworks.unreal.archive.content.maps.Map;
import net.shrimpworks.unreal.archive.mirror.MirrorClient;
import net.shrimpworks.unreal.archive.storage.DataStore;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static net.shrimpworks.unreal.archive.content.IndexUtils.UNKNOWN;

public class IndexCleanupUtil {

	@Test
	@Disabled
	public void contentManagerBenchmark() throws IOException {
		// warmup
		new ContentManager(Paths.get("unreal-archive-data/content/"), DataStore.NOP, DataStore.NOP, DataStore.NOP);

		long[] times = new long[10];
		for (int i = 0; i < times.length; i++) {
			long start = System.currentTimeMillis();
			new ContentManager(Paths.get("unreal-archive-data/content/"), DataStore.NOP, DataStore.NOP, DataStore.NOP);
			times[i] = System.currentTimeMillis() - start;
			System.out.println(i + ": " + times[i]);
		}

		System.out.println("Average: " + Arrays.stream(times).average().getAsDouble());
		System.out.println("Total: " + Arrays.stream(times).sum());

		/*
		  reference time:
			0: 9204
			1: 9257
			2: 9063
			3: 9047
			4: 8884
			5: 8891
			6: 8884
			7: 8861
			8: 8903
			9: 8911
			Average: 8990.5
			Total: 89905
		 */
	}

	/*
	 * URL encode URLs.
	 */
	@Test
	@Disabled
	public void fixUrlEncoding() throws IOException {
		ContentManager cm = new ContentManager(Paths.get("unreal-archive-data/content/"), DataStore.NOP, DataStore.NOP, DataStore.NOP);

		Collection<Content> all = cm.search(null, null, null, null);

		// for all the dupes, sort by release date desc, then mark others as variations of the first and store
		for (Content e : all) {
			Content content = cm.checkout(e.hash);
			content.downloads.forEach(d -> {
				if (d.url.startsWith("https://f002.backblazeb2.com/file/unreal-archive") && d.url.contains(" ")) {
//					d.url = Util.toUriString(d.url);
				}
			});
			content.attachments.forEach(a -> {
				if (a.url.startsWith("https://f002.backblazeb2.com/file/unreal-archive") && a.url.contains(" ")) {
//					a.url = Util.toUriString(a.url);
				}
			});
			if (cm.checkin(new IndexResult<>(content, Collections.emptySet()), null)) {
				System.out.println("Stored changes for " + String.join(" / ", content.contentType, content.game, content.name));
			} else {
				System.out.println("Failed to apply " + String.join(" / ", content.contentType, content.game, content.name));
			}
		}
	}

	@Test
	@Disabled
	public void sanitiseFilenames() throws IOException {
		Path root = Paths.get("unreal-archive-data/content/");
		Files.walkFileTree(root, new SimpleFileVisitor<>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (Util.extension(file).toLowerCase().equals("yml")) {
					Path newName = Util.safeFileName(file);
					if (!newName.equals(file)) {
						Files.move(file, newName, StandardCopyOption.REPLACE_EXISTING);
						System.out.printf("Renamed %s to %s%n", file.getFileName(), newName.getFileName());
					}
				}

				return FileVisitResult.CONTINUE;
			}
		});
	}

	/*
	 * Find duplicated content - items with the same name, but
	 * variances in content and hash.
	 */
	@Test
	@Disabled
	public void findDupes() throws IOException {
		ContentManager cm = new ContentManager(Paths.get("unreal-archive-data/content/"), DataStore.NOP, DataStore.NOP, DataStore.NOP);

		for (ContentType contentType : ContentType.values()) {
			// first, find the dupes
			java.util.Map<String, List<Content>> collect = cm.search(null, contentType.name(), null, null).stream()
															 .filter(c -> c.variationOf == null)
															 .collect(Collectors.groupingBy(s -> String.format("%s_%s",
																											   s.name.toLowerCase(),
																											   s.author.toLowerCase())))
															 .entrySet().stream()
															 .filter(m -> m.getValue().size() > 1)
															 .collect(Collectors.toMap(java.util.Map.Entry::getKey,
																					   java.util.Map.Entry::getValue));

			// for all the dupes, sort by release date desc, then mark others as variations of the first and store
			collect.entrySet().stream()
				   .sorted(Comparator.comparingInt(a -> a.getValue().size()))
				   .peek(e -> e.getValue().sort((a, b) -> -a.releaseDate.compareTo(b.releaseDate)))
				   .forEach(e -> {
					   Content first = e.getValue().get(0);
					   for (int i = 1; i < e.getValue().size(); i++) {
						   Content dupe = cm.checkout(e.getValue().get(i).hash);
						   dupe.variationOf = first.hash;
						   try {
							   if (cm.checkin(new IndexResult<>(dupe, Collections.emptySet()), null)) {
								   System.out.println("Stored changes for " + String.join(" / ", dupe.contentType, dupe.game, dupe.name));
							   } else {
								   System.out.println("Failed to apply");
							   }
						   } catch (IOException e1) {
							   e1.printStackTrace();
						   }
					   }
				   });
		}
	}

	/*
	 * A quick hack "test" (easily runnable in an IDE) to perform various
	 * transformations to indexed data.
	 */
	@Test
	@Disabled
	public void fixThings() throws IOException {
		ContentManager cm = new ContentManager(Paths.get("unreal-archive-data/content/"), DataStore.NOP, DataStore.NOP, DataStore.NOP);
		Collection<Content> search = cm.search(null, null, null, null);
		for (Content c : search) {
			if (c.downloads.stream().anyMatch(d -> d.url == null)) {
				System.out.println(String.join(" / ", c.game, c.contentType, c.name));
				Content content = cm.checkout(c.hash);
				content.downloads.removeIf(d -> d.url == null);
				if (cm.checkin(new IndexResult<>(content, Collections.emptySet()), null)) {
					System.out.println("Stored changes for " + String.join(" / ", content.game, content.contentType, content.name));
				} else {
					System.out.println("Failed to apply");
				}
			}
//			if (c instanceof Map) {
//				Map map = (Map)cm.checkout(c.hash);
//				map.author = map.author.replaceFirst("M�nnich", "Münnich").replaceFirst("M�NNICH", "Münnich".toUpperCase());
//				if (cm.checkin(new IndexResult<>(map, Collections.emptySet()), null)) {
//					System.out.println("Stored changes for " + String.join(" / ", map.game, map.gametype, map.name));
//				} else {
//					System.out.println("Failed to apply");
//				}
//			}
		}

//		search = cm.search(null, "MAP_PACK", null, null);
//		for (Content c : search) {
//			if (c instanceof MapPack) {
//				MapPack pack = (MapPack)cm.checkout(c.hash);
//				pack.author = pack.author.replaceFirst("M�nnich", "Münnich").replaceFirst("M�NNICH", "Münnich".toUpperCase());
//
//				for (MapPack.PackMap map : pack.maps) {
//					map.author = map.author.replaceFirst("M�nnich", "Münnich").replaceFirst("M�NNICH", "Münnich".toUpperCase());
//				}
//
//				if (cm.checkin(new IndexResult<>(pack, Collections.emptySet()), null)) {
//					System.out.println("Stored changes for " + String.join(" / ", pack.game, pack.gametype, pack.name));
//				} else {
//					System.out.println("Failed to apply");
//				}
//			}
//		}
	}

	/**
	 * Originally, indexed map packs did not contain gametype information,
	 * so this quick "test" will run through existing ones and apply a
	 * gametype.
	 */
	@Test
	@Disabled
	public void setMapPackGametypes() throws IOException {
		ContentManager cm = new ContentManager(Paths.get("unreal-archive-data/content/"), DataStore.NOP, DataStore.NOP, DataStore.NOP);
		Collection<MapPack> search = cm.get(MapPack.class);
		for (MapPack mp : search) {
			if (!mp.gametype.equalsIgnoreCase("unknown")) continue;

			MapPack mapPack = (MapPack)cm.checkout(mp.hash);

			mapPack.gametype = UNKNOWN;
			for (MapPack.PackMap map : mapPack.maps) {
				GameTypes.GameType gt = GameTypes.forMap(map.name);
				if (gt == null) continue;

				if (mapPack.gametype.equals(UNKNOWN)) {
					mapPack.gametype = gt.name;
				} else if (!mapPack.gametype.equalsIgnoreCase(gt.name)) {
					mapPack.gametype = "Mixed";
					break;
				}
			}

			if (cm.checkin(new IndexResult<>(mapPack, Collections.emptySet()), null)) {
				System.out.printf("Set gametype for %s to %s%n", mapPack.name, mapPack.gametype);
			} else {
				System.out.println("Failed to apply");
			}
		}
	}

	@Test
	@Disabled
	public void fixMapGametypes() throws IOException {
		ContentManager cm = new ContentManager(Paths.get("unreal-archive-data/content/"), DataStore.NOP, DataStore.NOP, DataStore.NOP);
		Collection<Content> search = cm.search(null, "MAP", "FHI-", null);
		for (Content c : search) {
			if (c instanceof Map && ((Map)c).gametype.equalsIgnoreCase("unknown")) {
				Map map = (Map)cm.checkout(c.hash);
				map.gametype = "Fraghouse Invasion";
				if (cm.checkin(new IndexResult<>(map, Collections.emptySet()), null)) {
					System.out.println("Stored changes for " + String.join(" / ", map.game, map.gametype, map.name));
				} else {
					System.out.println("Failed to apply");
				}
			}
		}
	}

	@Test
	@Disabled
	public void addMedorLinks() throws IOException {
		Path root = Paths.get("/home/shrimp/tmp/files/UnrealTournament/Voices/Scraped/Medor/");
		String path = "Voices/%s";
		String url = "http://medor.no-ip.org/index.php?dir=%s&file=%s";

		Files.walkFileTree(root, new SimpleFileVisitor<>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (Indexer.INCLUDE_TYPES.contains(Util.extension(file).toLowerCase())) {
					Path rel = root.relativize(file.getParent());

					String fullPath = String.format(path, URLEncoder.encode(rel.toString(), StandardCharsets.UTF_8.name()));
					String fullUrl = String.format(url, fullPath, URLEncoder.encode(Util.fileName(file), StandardCharsets.UTF_8.name()));

					Submission sub = new Submission(file, fullUrl);
					Files.write(Paths.get(file.toString() + ".yml"), YAML.toString(sub).getBytes(StandardCharsets.UTF_8));

					System.out.println(fullUrl);
				}

				return FileVisitResult.CONTINUE;
			}
		});
	}

	@Test
	@Disabled
	public void reindexContent() throws IOException {
//		final CLI cli = net.shrimpworks.unreal.archive.CLI.parse(Collections.emptyMap());
//		final DataStore imageStore = store(DataStore.StoreContent.IMAGES, cli);
//		final DataStore attachmentStore = store(DataStore.StoreContent.ATTACHMENTS, cli);
//		final DataStore contentStore = store(DataStore.StoreContent.CONTENT, cli);
		final DataStore imageStore = DataStore.NOP;
		final DataStore attachmentStore = DataStore.NOP;
		final DataStore contentStore = DataStore.NOP;

		final ContentManager cm = new ContentManager(Paths.get("unreal-archive-data/content/"),
													 contentStore, imageStore, attachmentStore);

		final Indexer indexer = new Indexer(cm, new Indexer.IndexerEvents() {
			@Override
			public void starting(int foundFiles) {}

			@Override
			public void progress(int indexed, int total, Path currentFile) {}

			@Override
			public void indexed(Submission submission, Optional<IndexResult<? extends Content>> indexed, IndexLog log) {}

			@Override
			public void completed(int indexedFiles, int errorCount) {}
		}, new Indexer.IndexerPostProcessor() {
			@Override
			public void indexed(Submission sub, Content before, IndexResult<? extends Content> result) {
				// do not let game get reassigned during this process
				if (before != null) {
					result.content.game = before.game;
				}

				// do not let gametype get reassigned during this process
				if (before instanceof Map && result.content instanceof Map) {
					((Map)result.content).gametype = ((Map)before).gametype;
				}
			}
		});

		final Path root = Paths.get("/home/shrimp/tmp/files/UnrealTournament2004/");

		Files.walkFileTree(root, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (Indexer.INCLUDE_TYPES.contains(Util.extension(file).toLowerCase())) {
					final String hash = Util.hash(file);
					final Content content = cm.forHash(hash);
					if (content instanceof Map
						&& content.game.equals("Unreal Tournament 2004") // for safety, restrict to a single game
						&& (content.attachments.isEmpty() // look for missing screenshots
							|| content.author.contains("�") // fix broken ascii names
							|| content.author.toLowerCase().equals("unknown") // look for new authors
							|| (content.description == null // maps with no description
								|| content.description.isEmpty()
								|| content.description.length() < 50)
						)) {
						System.out.printf("Map missing things: %s (%s)%n", file.getFileName(), content.name);
						indexer.index(true, null, file);
					}
				}

				return FileVisitResult.CONTINUE;
			}
		});
	}

	@Test
	@Disabled
	public void fixDuplicateMapImageFiles() throws IOException {
//		final CLI cli = net.shrimpworks.unreal.archive.CLI.parse(Collections.emptyMap());
//		final DataStore imageStore = store(DataStore.StoreContent.IMAGES, cli);
//		final DataStore attachmentStore = store(DataStore.StoreContent.ATTACHMENTS, cli);
//		final DataStore contentStore = store(DataStore.StoreContent.CONTENT, cli);
		final DataStore imageStore = DataStore.NOP;
		final DataStore attachmentStore = DataStore.NOP;
		final DataStore contentStore = DataStore.NOP;

		final ContentManager cm = new ContentManager(Paths.get("unreal-archive-data/content/"),
													 contentStore, imageStore, attachmentStore);

		final Indexer indexer = new Indexer(cm, new Indexer.IndexerEvents() {
			@Override
			public void starting(int foundFiles) {}

			@Override
			public void progress(int indexed, int total, Path currentFile) {}

			@Override
			public void indexed(Submission submission, Optional<IndexResult<? extends Content>> indexed, IndexLog log) {
				indexed.ifPresent(i -> {
					try {
						System.out.println(YAML.toString(i.content));
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			}

			@Override
			public void completed(int indexedFiles, int errorCount) {}
		}, new Indexer.IndexerPostProcessor() {
			@Override
			public void indexed(Submission sub, Content before, IndexResult<? extends Content> result) {
				// do not let game get reassigned during this process
				if (before != null) {
					result.content.game = before.game;
				}

				// do not let gametype get reassigned during this process
				if (before instanceof Map && result.content instanceof Map) {
					((Map)result.content).gametype = ((Map)before).gametype;
				}

				// clear existing attachments
				if (before != null) before.attachments.clear();
				result.content.attachments.clear();
			}
		});

		// find all maps with the same name, which have attachments
		java.util.Map<String, List<Content>> grouped = cm.search(null, "MAP", null, null).stream()
														 .filter(c -> !c.attachments.isEmpty())
														 .collect(Collectors.groupingBy(s -> String.format("%s_%s",
																										   s.game.toLowerCase(),
																										   s.name.toLowerCase())))
														 .entrySet().stream()
														 .filter(m -> m.getValue().size() > 1)
														 .collect(Collectors.toMap(java.util.Map.Entry::getKey,
																				   java.util.Map.Entry::getValue));

		final Path tmpDir = Files.createTempDirectory("ua-image-cleanup");

		grouped.forEach((k, contents) -> {
			System.out.println(k);
			contents.forEach(c -> new MirrorClient.Downloader(c, tmpDir, d -> {
				// remove images from content
				System.out.println("downloaded " + d.destination);
				try {
					indexer.index(true, null, d.destination);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}).run());
		});
	}

	/*
	 * Find files which have the same main download URL, but different hashes.
	 *
	 * Try to find their original downloads then re-upload them.
	 */
	@Test
	@Disabled
	public void findDupeFiles() throws IOException {
//		final CLI cli = net.shrimpworks.unreal.archive.CLI.parse(Collections.emptyMap());
//		final DataStore imageStore = store(DataStore.StoreContent.IMAGES, cli);
//		final DataStore attachmentStore = store(DataStore.StoreContent.ATTACHMENTS, cli);
//		final DataStore contentStore = store(DataStore.StoreContent.CONTENT, cli);
		final DataStore imageStore = DataStore.NOP;
		final DataStore attachmentStore = DataStore.NOP;
		final DataStore contentStore = DataStore.NOP;

		final ContentManager cm = new ContentManager(Paths.get("unreal-archive-data/content/"),
													 contentStore, imageStore, attachmentStore);

		System.out.println("Find all files");
		Set<Path> allFiles = new HashSet<>();
		Files.walkFileTree(Paths.get("/home/shrimp/tmp/files/"), new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				allFiles.add(file);
				return FileVisitResult.CONTINUE;
			}
		});

		for (ContentType contentType : ContentType.values()) {
			System.out.println("finding duplicate urls for " + contentType.name());
			// first, find the duplicate download URLs
			java.util.Map<String, List<Content>> collect = cm.search(null, contentType.name(), null, null).stream()
															 .collect(Collectors.groupingBy(s -> {
																 Content.Download dl = s.downloads.stream().filter(d -> d.main)
																								  .findFirst().get();
																 return dl.url;
															 }))
															 .entrySet().stream()
															 .filter(m -> m.getValue().size() > 1)
															 .collect(Collectors.toMap(java.util.Map.Entry::getKey,
																					   java.util.Map.Entry::getValue));

			// for all the duplicates, look for their hashes in the local file system
			collect.values().stream().flatMap(Collection::stream).forEach(c -> {
				Path path = allFiles.parallelStream()
									.filter(p -> p.getFileName().toString().equalsIgnoreCase(c.originalFilename))
									.filter(p -> {
										try {
											return Util.hash(p).equals(c.hash);
										} catch (IOException e) {
											e.printStackTrace();
										}
										return false;
									}).findFirst().orElse(null);

				// if we found a local file for the content hash, re-upload it with the new path info
				if (path != null) {
					Content fixed = cm.checkout(c.hash);
					fixed.downloads.removeIf(d -> d.main);
					Submission sub = new Submission(path);
					try {
						if (cm.checkin(new IndexResult<>(fixed, Collections.emptySet()), sub)) {
							System.out.println("Stored changes for " + String.join(" / ", fixed.contentType, fixed.game, fixed.name));
						} else {
							System.out.println("Failed to apply");
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					System.out.println("No local files found for " + c.name + " [" + c.hash + "]");
				}
			});
		}
	}

	@Test
	@Disabled
	public void reindexMapsWithThemes() throws IOException {
		ContentManager cm = new ContentManager(Paths.get("unreal-archive-data/content/"), DataStore.NOP, DataStore.NOP, DataStore.NOP);

		final Indexer indexer = new Indexer(cm, new Indexer.IndexerEvents() {
			@Override
			public void starting(int foundFiles) {}

			@Override
			public void progress(int indexed, int total, Path currentFile) {}

			@Override
			public void indexed(Submission submission, Optional<IndexResult<? extends Content>> indexed, IndexLog log) {}

			@Override
			public void completed(int indexedFiles, int errorCount) {}
		}, new Indexer.IndexerPostProcessor() {
			@Override
			public void indexed(Submission sub, Content before, IndexResult<? extends Content> result) {
				// do not let some things get reassigned during this process
				if (before != null) {
					result.content.game = before.game;
					result.content.author = before.author;
					result.content.variationOf = before.variationOf;
					result.content.attachments = before.attachments;
				}

				// in this process, we don't want to change files
				for (IndexResult.NewAttachment file : result.files) {
					try {
						Files.deleteIfExists(file.path);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				result.files.clear();

				// do not let gametype get reassigned
				if (before instanceof Map && result.content instanceof Map) {
					((Map)result.content).gametype = ((Map)before).gametype;
				}
				if (before instanceof MapPack && result.content instanceof MapPack) {
					((MapPack)result.content).gametype = ((MapPack)before).gametype;
				}
			}
		});

		Collection<Content> search = cm.search("Unreal Tournament", "MAP", null, null);
		final Path tmpDir = Files.createTempDirectory("ua-themes");

		for (Content c : search) {
			if (!(c.subGrouping().toLowerCase().startsWith("b"))) continue;

			new MirrorClient.Downloader(c, tmpDir, d -> {
				System.out.printf("Downloaded %s%n", d.destination);
				try {
					indexer.index(true, null, d.destination);
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						Files.deleteIfExists(d.destination);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).run();
		}
	}
}
