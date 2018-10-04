package net.shrimpworks.unreal.archive.indexer.mappacks;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.indexer.Content;
import net.shrimpworks.unreal.archive.indexer.Incoming;
import net.shrimpworks.unreal.archive.indexer.IndexHandler;
import net.shrimpworks.unreal.archive.indexer.IndexLog;
import net.shrimpworks.unreal.archive.indexer.IndexResult;
import net.shrimpworks.unreal.archive.indexer.IndexUtils;
import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.ExportedObject;
import net.shrimpworks.unreal.packages.entities.objects.Object;
import net.shrimpworks.unreal.packages.entities.properties.Property;
import net.shrimpworks.unreal.packages.entities.properties.StringProperty;

public class MapPackIndesHandler implements IndexHandler<MapPack> {

	private static final String SHOT_NAME = "%s_shot_%d.png";

	public static class MapPackIndesHandlerFactory implements IndexHandler.IndexHandlerFactory<MapPack> {

		@Override
		public IndexHandler<MapPack> get() {
			return new MapPackIndesHandler();
		}
	}

	@Override
	public void index(Incoming incoming, Content content, Consumer<IndexResult<MapPack>> completed) {
		IndexLog log = incoming.log;
		MapPack m = (MapPack)content;

		m.name = packName(m.name);

		Set<Incoming.IncomingFile> maps = incoming.files(Incoming.FileType.MAP);

		boolean gameOverride = false;
		if (incoming.submission.override.get("game", null) != null) {
			gameOverride = true;
			m.game = incoming.submission.override.get("game", "Unreal Tournament");
		} else {
			m.game = game(maps.iterator().next());
		}

		try (Package map = map(maps.iterator().next())) {
			if (!gameOverride) {
				// attempt to detect Unreal maps by possible release date
				if (map.version < 68 || (m.releaseDate != null && m.releaseDate.compareTo(RELEASE_UT99) < 0)) m.game = "Unreal";
				// Unreal does not contain a LevelSummary
				if (map.version == 68 && map.objectsByClassName("LevelSummary").isEmpty()) m.game = "Unreal";
			}
		} catch (IOException e) {
			log.log(IndexLog.EntryType.CONTINUE, "Failed to read map package", e);
		} catch (Exception e) {
			log.log(IndexLog.EntryType.CONTINUE, "Caught while parsing map pack: " + e.getMessage(), e);
		}

		m.maps.clear();
		Set<IndexResult.NewAttachment> attachments = new HashSet<>();
		for (Incoming.IncomingFile map : maps) {
			try {
				m.maps.add(addMap(incoming, map, images -> {
					try {
						IndexUtils.saveImages(SHOT_NAME, m, images, attachments);
					} catch (IOException e) {
						log.log(IndexLog.EntryType.CONTINUE, "Failed saving images for map pack map", e);
					}
				}));
			} catch (Exception e) {
				log.log(IndexLog.EntryType.CONTINUE, "Reading map failed", e);
			}
		}

		m.author = UNKNOWN;
		for (MapPack.PackMap map : m.maps) {
			if (m.author.equals(UNKNOWN)) {
				m.author = map.author;
			} else if (!m.author.equalsIgnoreCase(map.author)) {
				m.author = "Various";
				break;
			}
		}

		completed.accept(new IndexResult<>(m, Collections.emptySet()));
	}

	private MapPack.PackMap addMap(Incoming incoming, Incoming.IncomingFile map, Consumer<List<BufferedImage>> listConsumer) {
		MapPack.PackMap p = new MapPack.PackMap();
		p.author = UNKNOWN;
		p.name = Util.fileName(map.fileName());
		p.name = p.name.substring(0, p.name.lastIndexOf(".")).replaceAll("/", "").trim().replaceAll("[^\\x20-\\x7E]", "").trim();
		p.title = p.name;

		List<BufferedImage> images = new ArrayList<>();

		try (Package pkg = map(map)) {

			Collection<ExportedObject> maybeLevelInfo = pkg.objectsByClassName("LevelInfo");
			if (maybeLevelInfo != null && !maybeLevelInfo.isEmpty()) {
				ExportedObject levelInfo = maybeLevelInfo.iterator().next();
				if (levelInfo == null) return p;

				Object level = levelInfo.object();

				Property author = level.property("Author");
				Property title = level.property("Title");

				if (author != null) p.author = ((StringProperty)author).value.trim();
				if (title != null) p.title = ((StringProperty)title).value.trim();

				Property screenshot = level.property("Screenshot");
				images.addAll(IndexUtils.screenshots(incoming, pkg, screenshot));
			}

		} catch (Exception e) {
			incoming.log.log(IndexLog.EntryType.CONTINUE, "Failed to read map properties", e);
		}

		listConsumer.accept(images);
		return p;
	}

	private String game(Incoming.IncomingFile incoming) {
		if (incoming.fileName().toLowerCase().endsWith(".unr")) return "Unreal Tournament";
		if (incoming.fileName().toLowerCase().endsWith(".ut2")) return "Unreal Tournament 2004";
		if (incoming.fileName().toLowerCase().endsWith(".ut3")) return "Unreal Tournament 3";
		if (incoming.fileName().toLowerCase().endsWith(".un2")) return "Unreal 2";

		return UNKNOWN;
	}

	private String packName(String name) {
		// Cool_name_bro -> Cool Name Bro
		// cool-name-bro -> Cool Name Bro

		String[] words = name.replaceAll("([-_.])", " ").trim().split("\\s");
		String[] res = new String[words.length];

		for (int i = 0; i < words.length; i++) {
			if (words[i].length() == 1) res[i] = words[i];
			else res[i] = Character.toUpperCase(words[i].charAt(0)) + words[i].substring(1);
		}

		return String.join(" ", res);
	}

	private Package map(Incoming.IncomingFile file) {
		return new Package(new PackageReader(file.asChannel()));
	}

}