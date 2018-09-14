package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import net.shrimpworks.unreal.archive.maps.Map;

import org.junit.Test;

import static org.junit.Assert.*;

public class YAMLTest {

	@Test
	public void serialiseStuff() throws IOException {
		Map m = new Map();
		m.firstIndex = LocalDateTime.now().minus(1, ChronoUnit.DAYS);
		m.lastIndex = LocalDateTime.now();

		m.game = "Unreal Tournament";
		m.name = "DM-MyMap";
		m.gametype = "Deathmatch";
		m.title = "My Map";
		m.author = "Joe Soap";
		m.playerCount = "2 - 4 Players";
		m.releaseDate = "2001-05";
		m.screenshots = Arrays.asList("Screenshot1.png", "shot2.jpg");
		m.packageSHA1 = "123456789";
		m.fileSize = 564231;
		m.files = Arrays.asList(file("DM-MyMap.unr"), file("MyTex.utx"));
		m.downloads = Arrays.asList(download("mysite.com/map.zip"), download("http://maps.com/map.rar"));

		m.deleted = false;

		String yaml = YAML.toString(m);
		assertNotNull(yaml);
		assertTrue(yaml.contains(m.name) && yaml.contains(m.author) && yaml.contains("shot2.jpg"));

		Map copy = YAML.fromString(yaml, Map.class);

		assertNotNull(copy);

		assertEquals(m.title, copy.title);
		assertEquals(m.screenshots.get(1), copy.screenshots.get(1));

		Path wrote = Files.write(Files.createTempFile("test-map", ".yaml"),
								 YAML.toString(copy).getBytes(StandardCharsets.UTF_8),
								 StandardOpenOption.CREATE);

		Map another = YAML.fromFile(wrote, Map.class);
		assertNotNull(another);
		assertEquals(m.title, another.title);
		assertEquals(m.screenshots.get(1), another.screenshots.get(1));

		System.out.println(YAML.toString(another));
	}

	private ContentFile file(String name) {
		ContentFile f = new ContentFile();
		f.name = name;
		f.fileSize = (int)(Math.random() * 10240);
		f.sha1 = "abc" + (Math.random() * 20480);
		return f;
	}

	private Download download(String url) {
		Download dl = new Download();
		dl.url = url;
		dl.lastChecked = LocalDate.now().minus((long)(Math.random() * 500), ChronoUnit.DAYS);
		dl.ok = true;
		dl.added = dl.lastChecked.minus((long)(Math.random() * 1000), ChronoUnit.DAYS);
		dl.repack = false;
		dl.deleted = false;
		return dl;
	}
}
