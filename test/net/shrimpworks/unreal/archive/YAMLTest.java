package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import net.shrimpworks.unreal.archive.indexer.Content;
import net.shrimpworks.unreal.archive.indexer.ContentManager;
import net.shrimpworks.unreal.archive.indexer.ContentType;
import net.shrimpworks.unreal.archive.indexer.IndexResult;
import net.shrimpworks.unreal.archive.indexer.mappacks.MapPack;
import net.shrimpworks.unreal.archive.indexer.maps.GameTypes;
import net.shrimpworks.unreal.archive.indexer.maps.Map;
import net.shrimpworks.unreal.archive.storage.DataStore;

import org.junit.Ignore;
import org.junit.Test;

import static net.shrimpworks.unreal.archive.indexer.IndexHandler.UNKNOWN;
import static org.junit.Assert.*;

public class YAMLTest {

	/*
	 * A quick hack "test" (easily runnable in an IDE) to perform various
	 * transformations to indexed data.
	 */
	@Test
	@Ignore
	public void fixThings() throws IOException {
		ContentManager cm = new ContentManager(Paths.get("unreal-archive-data/archive-content/"),
											   new DataStore.NopStore(), new DataStore.NopStore(), new DataStore.NopStore());
		Collection<Content> search = cm.search("Unreal Tournament", "MAP", "AF-", null);
		for (Content c : search) {
			if (c instanceof Map && c.name.toLowerCase().startsWith("af-")) {
				Map map = (Map)cm.checkout(c.hash);

				map.gametype = "AirFight";
				if (cm.checkin(new IndexResult<>(map, Collections.emptySet()), null)) {
					System.out.println("Stored changes for " + String.join(" / ", map.game, map.gametype, map.name));
				} else {
					System.out.println("Failed to apply");
				}
			}
		}
	}

	/**
	 * Originally, indexed map packs did not contain gametype information,
	 * so this quick "test" will run through existing ones and apply a
	 * gametype.
	 */
	@Test
	@Ignore
	public void setMapPackGametypes() throws IOException {
		ContentManager cm = new ContentManager(Paths.get("unreal-archive-data/archive-content/"),
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

	@Test
	public void serialiseStuff() throws IOException {
		Map m = makeMap();

		String yaml = YAML.toString(m);

		assertNotNull(yaml);
		assertTrue(yaml.contains(m.name) && yaml.contains(m.author) && yaml.contains("shot2.jpg"));

		Map copy = YAML.fromString(yaml, Map.class);

		assertNotNull(copy);

		assertEquals(m.title, copy.title);
		assertEquals(m.attachments.get(1), copy.attachments.get(1));

		Path wrote = Files.write(Files.createTempFile("test-map", ".yaml"),
								 YAML.toString(copy).getBytes(StandardCharsets.UTF_8),
								 StandardOpenOption.CREATE);

		Map another = YAML.fromFile(wrote, Map.class);
		assertNotNull(another);
		assertEquals(m.title, another.title);
		assertEquals(m.attachments.get(1), another.attachments.get(1));

		System.out.println(YAML.toString(another));
	}

	private Map makeMap() {
		Map m = ContentType.MAP.newContent(null);

		m.firstIndex = LocalDateTime.now().minus(1, ChronoUnit.DAYS);
		m.lastIndex = LocalDateTime.now();

		m.game = "Unreal Tournament";
		m.name = "DM-MyMap";
		m.gametype = "Deathmatch";
		m.title = "My Map";
		m.author = "Joe Soap";
		m.playerCount = "2 - 4 Players";
		m.releaseDate = "2001-05";
		m.attachments = Arrays.asList(attachment("localhost/Screenshot1.png"), attachment("lolhosting.com/path/shot2.jpg"));
		m.hash = "123456789";
		m.fileSize = 564231;
		m.files = Arrays.asList(file("DM-MyMap.unr"), file("MyTex.utx"));
		m.downloads = Arrays.asList(download("mysite.com/map.zip"), download("http://maps.com/map.rar"));

		m.deleted = false;

		return m;
	}

	private Content.ContentFile file(String name) {
		return new Content.ContentFile(name, (int)(Math.random() * 10240), "abc" + (Math.random() * 20480));
	}

	private Content.Download download(String url) {
		return new Content.Download(url, false,
									LocalDate.now().minus((long)(Math.random() * 500), ChronoUnit.DAYS),
									LocalDate.now().minus((long)(Math.random() * 100), ChronoUnit.DAYS),
									true, false, false
		);
	}

	private Content.Attachment attachment(String url) {
		return new Content.Attachment(Content.AttachmentType.IMAGE, url.substring(url.lastIndexOf("/")), url);
	}
}
