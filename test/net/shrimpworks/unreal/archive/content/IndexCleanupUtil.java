package net.shrimpworks.unreal.archive.content;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.YAML;
import net.shrimpworks.unreal.archive.content.mappacks.MapPack;
import net.shrimpworks.unreal.archive.content.maps.GameTypes;
import net.shrimpworks.unreal.archive.content.maps.Map;
import net.shrimpworks.unreal.archive.storage.DataStore;

import org.junit.Ignore;
import org.junit.Test;

import static net.shrimpworks.unreal.archive.content.IndexUtils.UNKNOWN;

public class IndexCleanupUtil {

	/*
	 * Find duplicated content - items with the same name, but
	 * variances in content and hash.
	 */
	@Test
	@Ignore
	public void findDupes() throws IOException {
		ContentManager cm = new ContentManager(Paths.get("unreal-archive-data/content/"),
											   new DataStore.NopStore(), new DataStore.NopStore(), new DataStore.NopStore());

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
	@Ignore
	public void fixThings() throws IOException {
		ContentManager cm = new ContentManager(Paths.get("unreal-archive-data/content/"),
											   new DataStore.NopStore(), new DataStore.NopStore(), new DataStore.NopStore());
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
	@Ignore
	public void setMapPackGametypes() throws IOException {
		ContentManager cm = new ContentManager(Paths.get("unreal-archive-data/content/"),
											   new DataStore.NopStore(), new DataStore.NopStore(), new DataStore.NopStore());
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
	@Ignore
	public void fixMapGametypes() throws IOException {
		ContentManager cm = new ContentManager(Paths.get("unreal-archive-data/content/"),
											   new DataStore.NopStore(), new DataStore.NopStore(), new DataStore.NopStore());
		Collection<Content> search = cm.search(null, "MAP", "RO-", null);
		for (Content c : search) {
			if (c instanceof Map && ((Map)c).gametype.equalsIgnoreCase("unknown")) {
				Map map = (Map)cm.checkout(c.hash);
				map.gametype = "Red Orchestra";
				if (cm.checkin(new IndexResult<>(map, Collections.emptySet()), null)) {
					System.out.println("Stored changes for " + String.join(" / ", map.game, map.gametype, map.name));
				} else {
					System.out.println("Failed to apply");
				}
			}
		}
	}

	@Test
	@Ignore
	public void addMedorLinks() throws IOException {
		Path root = Paths.get("/home/shrimp/tmp/files/UnrealTournament/Voices/Scraped/Medor/");
		String path = "Voices/%s";
		String url = "http://medor.no-ip.org/index.php?dir=%s&file=%s";

		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

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
	@Ignore
	public void reindexContent() throws IOException {
		ContentManager cm = new ContentManager(Paths.get("unreal-archive-data/content/"),
											   new DataStore.NopStore(), new DataStore.NopStore(), new DataStore.NopStore());

		Indexer indexer = new Indexer(cm);

		Path root = Paths.get("/home/shrimp/tmp/files/Unreal/");

		Files.walkFileTree(root, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (Indexer.INCLUDE_TYPES.contains(Util.extension(file).toLowerCase())) {
					String hash = Util.hash(file);
					Content content = cm.forHash(hash);
					if (content instanceof Map
						&& content.game.equals("Unreal") // for safety, restrict to a single game
						&& (content.attachments.isEmpty() // look for missing screenshots
							|| content.author.contains("�") // fix broken ascii names
							|| content.author.toLowerCase().equals("unknown"))) { // look for new authors
						System.out.printf("Map without screenshots: %s%n", content.name);
						indexer.index(true, null, new Indexer.CLIEventPrinter(false), file);
					}
				}

				return FileVisitResult.CONTINUE;
			}
		});
	}
}
