package net.shrimpworks.unreal.archive.indexer;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.indexer.mappacks.MapPack;
import net.shrimpworks.unreal.archive.indexer.maps.GameTypes;
import net.shrimpworks.unreal.archive.storage.DataStore;

import org.junit.Ignore;
import org.junit.Test;

import static net.shrimpworks.unreal.archive.indexer.IndexUtils.UNKNOWN;

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
								   System.out.println("Stored changes for " + String.join(" / ", dupe.game, dupe.name));
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

}
