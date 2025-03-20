package org.unrealarchive.indexing.mappacks;

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

import net.shrimpworks.unreal.packages.IntFile;
import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.ExportedObject;
import net.shrimpworks.unreal.packages.entities.objects.Object;
import net.shrimpworks.unreal.packages.entities.properties.Property;
import net.shrimpworks.unreal.packages.entities.properties.StringProperty;

import org.unrealarchive.common.Util;
import org.unrealarchive.content.AuthorNames;
import org.unrealarchive.content.FileType;
import org.unrealarchive.content.Games;
import org.unrealarchive.content.addons.Addon;
import org.unrealarchive.content.addons.MapGameTypes;
import org.unrealarchive.content.addons.MapPack;
import org.unrealarchive.content.addons.MapThemes;
import org.unrealarchive.indexing.Incoming;
import org.unrealarchive.indexing.IndexHandler;
import org.unrealarchive.indexing.IndexLog;
import org.unrealarchive.indexing.IndexResult;
import org.unrealarchive.indexing.IndexUtils;
import org.unrealarchive.indexing.maps.MapIndexHandler;

import static org.unrealarchive.content.addons.Addon.UNKNOWN;

public class MapPackIndexHandler implements IndexHandler<MapPack> {

	public static class MapPackIndexHandlerFactory implements IndexHandler.IndexHandlerFactory<MapPack> {

		@Override
		public IndexHandler<MapPack> get() {
			return new MapPackIndexHandler();
		}
	}

	@Override
	public void index(Incoming incoming, Addon content, Consumer<IndexResult<MapPack>> completed) {
		IndexLog log = incoming.log;
		MapPack m = (MapPack)content;

		Set<Incoming.IncomingFile> maps = incoming.files(FileType.MAP);
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
			m.game = IndexUtils.game(incoming).name;
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

		try {
			IndexUtils.saveImages(IndexUtils.SHOT_NAME, m, IndexUtils.findImageFiles(incoming), attachments);
		} catch (Exception e) {
			log.log(IndexLog.EntryType.CONTINUE, "Failed finding additional attachment images", e);
		}
		m.maps = m.maps.stream().distinct().toList();

		m.author = UNKNOWN;
		for (MapPack.PackMap map : m.maps) {
			if (m.author.equals(UNKNOWN)) {
				m.author = AuthorNames.nameFor(map.author);
			} else if (!AuthorNames.nameFor(m.author).equalsIgnoreCase(AuthorNames.nameFor(map.author))) {
				m.author = "Various";
				break;
			}
		}

		// find a common gametype if possible
		m.gametype = UNKNOWN;
		for (MapPack.PackMap map : m.maps) {
			MapGameTypes.MapGameType gt = MapGameTypes.forMap(Games.byName(m.game), map.name);
			if (gt == null) continue;

			if (m.gametype.equals(UNKNOWN)) {
				m.gametype = gt.name();
			} else if (!m.gametype.equalsIgnoreCase(gt.name())) {
				m.gametype = "Mixed";
				break;
			}
		}

		// for the top 5 themes, give them a percentage value of the total themeable content
		Map<String, Double> topThemes = mapThemes.entrySet().stream()
												 .sorted((a, b) -> -a.getValue().compareTo(b.getValue()))
												 .limit(MapThemes.MAX_THEMES)
												 .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		double totalScore = topThemes.values().stream().mapToDouble(e -> e).sum();
		m.themes.clear();
		m.themes.putAll(mapThemes.entrySet()
								 .stream()
								 .filter(e -> (e.getValue() / totalScore) > MapThemes.MIN_THRESHOLD)
								 .collect(Collectors.toMap(Map.Entry::getKey,
														   v -> BigDecimal.valueOf(v.getValue() / totalScore)
																		  .setScale(1, RoundingMode.HALF_UP).doubleValue()
								 )));

		completed.accept(new IndexResult<>(m, attachments));
	}

	private MapPack.PackMap addMap(Incoming incoming, Incoming.IncomingFile map, Map<String, Double> themes,
								   Consumer<List<BufferedImage>> listConsumer) {
		MapPack.PackMap p = new MapPack.PackMap();
		p.author = UNKNOWN;
		p.name = Util.plainName(map.fileName());
		p.title = p.name;

		List<BufferedImage> images = new ArrayList<>();

		try (Package pkg = map(map)) {
			if (Util.extension(map.fileName()).equalsIgnoreCase("ut3")) return scrapeUE3(incoming, map, pkg, p, listConsumer);

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
		} catch (Throwable e) {
			incoming.log.log(IndexLog.EntryType.CONTINUE, "Failed to read map properties", e);
		}

		listConsumer.accept(images);
		return p;
	}

	private MapPack.PackMap scrapeUE3(Incoming incoming, Incoming.IncomingFile map, Package pkg, MapPack.PackMap p,
									  Consumer<List<BufferedImage>> listConsumer) {
		List<BufferedImage> images = new ArrayList<>();

		Set<Incoming.IncomingFile> iniFile = incoming.files(FileType.INI)
													 .stream()
													 .filter(f -> f.fileName().equalsIgnoreCase(Util.plainName(map.fileName()) + ".ini"))
													 .collect(Collectors.toSet());

		IndexUtils.readIntFiles(incoming, iniFile).findFirst().ifPresent(ini -> ini.sections().forEach(s -> {
			IntFile.Value name = ini.section(s).value("MapName");
			if (name instanceof IntFile.SimpleValue) p.name = ((IntFile.SimpleValue)name).value().trim();

			IntFile.Value title = ini.section(s).value("FriendlyName");
			if (title instanceof IntFile.SimpleValue) p.title = ((IntFile.SimpleValue)title).value().trim();

			IntFile.Value players = ini.section(s).value("NumPlayers");
			if (players instanceof IntFile.SimpleValue) {
				String playerCount = ((IntFile.SimpleValue)players).value().replaceAll("([Pp]layers)", "");
				if (playerCount.toLowerCase().contains("author")) {
					p.author = playerCount.replaceAll(".*(?i)authors?\\s?:?\\s?(.*)", "$1");
				}
			}
		}));

		try {
			images.addAll(IndexUtils.screenshots(incoming, pkg, null));
		} catch (Throwable e) {
			incoming.log.log(IndexLog.EntryType.CONTINUE, "Failed to extract screenshots: " + e, e);
		}

		listConsumer.accept(images);

		return p;
	}

	private Package map(Incoming.IncomingFile file) {
		return new Package(new PackageReader(file.asChannel()));
	}

}