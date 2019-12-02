package net.shrimpworks.unreal.archive.content.mappacks;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.Incoming;
import net.shrimpworks.unreal.archive.content.IndexHandler;
import net.shrimpworks.unreal.archive.content.IndexLog;
import net.shrimpworks.unreal.archive.content.IndexResult;
import net.shrimpworks.unreal.archive.content.IndexUtils;
import net.shrimpworks.unreal.archive.content.maps.GameTypes;
import net.shrimpworks.unreal.archive.content.maps.MapIndexHandler;
import net.shrimpworks.unreal.archive.content.maps.Themes;
import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.ExportedObject;
import net.shrimpworks.unreal.packages.entities.objects.Object;
import net.shrimpworks.unreal.packages.entities.properties.Property;
import net.shrimpworks.unreal.packages.entities.properties.StringProperty;

public class MapPackIndexHandler implements IndexHandler<MapPack> {

	public static class MapPackIndesHandlerFactory implements IndexHandler.IndexHandlerFactory<MapPack> {

		@Override
		public IndexHandler<MapPack> get() {
			return new MapPackIndexHandler();
		}
	}

	@Override
	public void index(Incoming incoming, Content content, Consumer<IndexResult<MapPack>> completed) {
		IndexLog log = incoming.log;
		MapPack m = (MapPack)content;

		Set<Incoming.IncomingFile> maps = incoming.files(Incoming.FileType.MAP);
		if (maps.isEmpty()) {
			log.log(IndexLog.EntryType.FATAL, "Cannot index a map pack with no maps!", new IllegalStateException());
			return;
		}

		m.name = IndexUtils.friendlyName(m.name);

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
				if (map.version < 69 || (m.releaseDate != null && m.releaseDate.compareTo(IndexUtils.RELEASE_UT99) < 0)) m.game = "Unreal";
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
		Map<String, Double> mapThemes = new HashMap<>();
		for (Incoming.IncomingFile map : maps) {
			try {
				m.maps.add(addMap(incoming, map, mapThemes, images -> {
					try {
						IndexUtils.saveImages(IndexUtils.SHOT_NAME, m, images, attachments);
					} catch (IOException e) {
						log.log(IndexLog.EntryType.CONTINUE, "Failed saving images for map pack map", e);
					}
				}));
			} catch (Exception e) {
				log.log(IndexLog.EntryType.CONTINUE, "Reading map failed", e);
			}
		}

		m.author = IndexUtils.UNKNOWN;
		for (MapPack.PackMap map : m.maps) {
			if (m.author.equals(IndexUtils.UNKNOWN)) {
				m.author = map.author;
			} else if (!m.author.equalsIgnoreCase(map.author)) {
				m.author = "Various";
				break;
			}
		}

		// find a common gametype if possible
		m.gametype = IndexUtils.UNKNOWN;
		for (MapPack.PackMap map : m.maps) {
			GameTypes.GameType gt = GameTypes.forMap(map.name);
			if (gt == null) continue;

			if (m.gametype.equals(IndexUtils.UNKNOWN)) {
				m.gametype = gt.name;
			} else if (!m.gametype.equalsIgnoreCase(gt.name)) {
				m.gametype = "Mixed";
				break;
			}
		}

		// for the top 5 themes, give them a percentage value of the total themeable content
		Map<String, Double> topThemes = mapThemes.entrySet().stream()
												 .sorted((a, b) -> -a.getValue().compareTo(b.getValue()))
												 .limit(Themes.MAX_THEMES)
												 .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		double totalScore = topThemes.values().stream().mapToDouble(e -> e).sum();
		m.themes.clear();
		m.themes.putAll(mapThemes.entrySet()
								 .stream()
								 .filter(e -> (e.getValue() / totalScore) > Themes.MIN_THRESHOLD)
								 .collect(Collectors.toMap(Map.Entry::getKey,
														   v -> BigDecimal.valueOf(v.getValue() / totalScore)
																		  .setScale(1, RoundingMode.HALF_UP).doubleValue()
								 )));

		completed.accept(new IndexResult<>(m, attachments));
	}

	private MapPack.PackMap addMap(Incoming incoming, Incoming.IncomingFile map, Map<String, Double> themes,
								   Consumer<List<BufferedImage>> listConsumer) {
		MapPack.PackMap p = new MapPack.PackMap();
		p.author = IndexUtils.UNKNOWN;
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
			MapIndexHandler.themes(pkg).forEach((theme, weight) -> themes.compute(theme, (k, v) -> v == null ? weight : v + weight));
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

		return IndexUtils.UNKNOWN;
	}

	private Package map(Incoming.IncomingFile file) {
		return new Package(new PackageReader(file.asChannel()));
	}

}