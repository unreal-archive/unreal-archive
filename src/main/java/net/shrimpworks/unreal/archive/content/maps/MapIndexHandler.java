package net.shrimpworks.unreal.archive.content.maps;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.Incoming;
import net.shrimpworks.unreal.archive.content.IndexHandler;
import net.shrimpworks.unreal.archive.content.IndexLog;
import net.shrimpworks.unreal.archive.content.IndexResult;
import net.shrimpworks.unreal.archive.content.IndexUtils;
import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.ExportedObject;
import net.shrimpworks.unreal.packages.entities.Import;
import net.shrimpworks.unreal.packages.entities.Named;
import net.shrimpworks.unreal.packages.entities.objects.Object;
import net.shrimpworks.unreal.packages.entities.objects.Polys;
import net.shrimpworks.unreal.packages.entities.properties.ArrayProperty;
import net.shrimpworks.unreal.packages.entities.properties.IntegerProperty;
import net.shrimpworks.unreal.packages.entities.properties.Property;
import net.shrimpworks.unreal.packages.entities.properties.StringProperty;

public class MapIndexHandler implements IndexHandler<Map> {

	private static final Pattern SP_MATCH = Pattern.compile("(.+)?(single ?player|cooperative)([\\s:]+)?yes(\\s+)?",
															Pattern.CASE_INSENSITIVE);

	private static final int BOT_PATH_MIN = 5; // minimum number fo connected PathNodes to assume whether this map has bot support

	public static class MapIndexHandlerFactory implements IndexHandlerFactory<Map> {

		@Override
		public IndexHandler<Map> get() {
			return new MapIndexHandler();
		}
	}

	@Override
	public void index(Incoming incoming, Content content, Consumer<IndexResult<Map>> completed) {
		IndexLog log = incoming.log;
		Map m = (Map)content;

		Incoming.IncomingFile baseMap = baseMap(incoming);

		// populate basic information; the rest of this will be filled in later if possible
		m.name = mapName(baseMap);
		m.gametype = gameType(incoming, m.name);
		m.title = m.name;

		boolean gameOverride = false;
		if (incoming.submission.override.get("game", null) != null) {
			gameOverride = true;
			m.game = incoming.submission.override.get("game", "Unreal Tournament");
		} else {
			m.game = game(incoming);
		}

		Set<IndexResult.NewAttachment> attachments = new HashSet<>();

		try (Package map = map(baseMap)) {
			if (!gameOverride) {
				// attempt to detect Unreal maps by possible release date
				if (map.version < 68 || (m.releaseDate != null && m.releaseDate.compareTo(IndexUtils.RELEASE_UT99) < 0)) m.game = "Unreal";
				// Unreal does not contain a LevelSummary
				if (map.version == 68 && map.objectsByClassName("LevelSummary").isEmpty()) m.game = "Unreal";
			}

			// read level info (also in LevelSummary, but missing Screenshot)
			Collection<ExportedObject> maybeLevelInfo = map.objectsByClassName("LevelInfo");
			if (maybeLevelInfo == null || maybeLevelInfo.isEmpty()) {
				throw new IllegalArgumentException("Could not find LevelInfo in map");
			}

			// if there are multiple LevelInfos in a map, try to find the right one...
			Object level = maybeLevelInfo.stream()
										 .map(ExportedObject::object)
										 .filter(l -> l.property("Title") != null || l.property("Author") != null)
										 .findFirst()
										 .orElse(maybeLevelInfo.iterator().next().object());

			// read some basic level info
			Property author = level.property("Author");
			Property title = level.property("Title");
			Property description = level.property("Description") != null
					? level.property("Description")
					: level.property("LevelEnterText"); // fallback for Unreal, some maps have fun text here

			if (author != null) m.author = ((StringProperty)author).value.trim();
			if (title != null) m.title = ((StringProperty)title).value.trim();
			if (description != null) m.description = ((StringProperty)description).value.trim();

			// just in case, some maps seem to have blank values occasionally
			if (m.author.isBlank()) m.author = "Unknown";
			if (m.title.isBlank()) m.title = m.name;

			if (map.version < 117) {
				Property idealPlayerCount = level.property("IdealPlayerCount");
				if (idealPlayerCount != null) m.playerCount = ((StringProperty)idealPlayerCount).value.trim();
			} else {
				Property idealPlayerCountMin = level.property("IdealPlayerCountMin");
				Property idealPlayerCountMax = level.property("IdealPlayerCountMax");
				int min = 0;
				int max = 0;
				if (idealPlayerCountMin != null) min = ((IntegerProperty)idealPlayerCountMin).value;
				if (idealPlayerCountMax != null) max = ((IntegerProperty)idealPlayerCountMax).value;

				if (min == max && max > 0) m.playerCount = Integer.toString(max);
				else if (min > 0 && max > 0) m.playerCount = min + "-" + max;
				else if (min > 0 || max > 0) m.playerCount = Integer.toString(Math.max(min, max));
			}

			Property screenshot = level.property("Screenshot");

			if (!gameOverride) {
				// use this opportunity to resolve some version overlap between game versions
				if (screenshot != null && map.version < 117 && !map.objectsByClassName("LevelSummary").isEmpty()) {
					m.game = "Unreal Tournament";
				}
				if (m.gametype.equals("XMP") && map.version >= 126
					&& !map.exportsByClassName("DeploymentPoint").isEmpty() && m.game.equals("Unreal Tournament")) {
					m.game = "Unreal 2";
				}
			}

			List<BufferedImage> screenshots = IndexUtils.screenshots(incoming, map, screenshot);
			screenshots.addAll(IndexUtils.findImageFiles(incoming));
			IndexUtils.saveImages(IndexUtils.SHOT_NAME, m, screenshots, attachments);

			// Find map themes
			m.themes.clear();
			m.themes.putAll(themes(map));

			m.bots = botSupport(map);
		} catch (IOException e) {
			log.log(IndexLog.EntryType.CONTINUE, "Failed to read map package", e);
		} catch (Exception e) {
			log.log(IndexLog.EntryType.CONTINUE, "Caught while parsing map: " + e.getMessage(), e);
		}

		completed.accept(new IndexResult<>(m, attachments));
	}

	private Incoming.IncomingFile baseMap(Incoming incoming) {
		Set<Incoming.IncomingFile> maps = incoming.files(Incoming.FileType.MAP);

		Incoming.IncomingFile shortestMap = null;
		for (Incoming.IncomingFile map : maps) {
			if (shortestMap == null || map.fileName().length() < shortestMap.fileName().length()) {
				shortestMap = map;
			}
		}
		return shortestMap;
	}

	private Package map(Incoming.IncomingFile mapFile) {
		return new Package(new PackageReader(mapFile.asChannel()));
	}

	private String mapName(Incoming.IncomingFile mapFile) {
		return Util.plainName(mapFile.fileName());
	}

	private String gameType(Incoming incoming, String name) {
		if (incoming.submission.override.get("gameType", null) != null) return incoming.submission.override.get("gameType", "DeathMatch");

		GameTypes.GameType gameType = GameTypes.forMap(name);

		if (gameType == null && maybeSingleplayer(incoming)) return "Single Player";

		if (gameType != null) return gameType.name;

		return IndexUtils.UNKNOWN;
	}

	private boolean maybeSingleplayer(Incoming incoming) {

		if (incoming.submission.filePath.toString().toLowerCase().contains("singleplayer")
			|| incoming.submission.filePath.toString().toLowerCase().contains("coop")) {
			return true;
		}

		try {
			List<String> lines = IndexUtils.textContent(incoming, Incoming.FileType.TEXT, Incoming.FileType.HTML);

			for (String s : lines) {
				Matcher m = SP_MATCH.matcher(s);
				if (m.matches()) return true;
			}
		} catch (Exception e) {
			incoming.log.log(IndexLog.EntryType.CONTINUE, "Couldn't figure out if singleplayer by reading text files", e);
		}

		return false;
	}

	private String game(Incoming incoming) {
		if (incoming.submission.override.get("game", null) != null) return incoming.submission.override.get("game", "Unreal Tournament");

		for (Incoming.IncomingFile f : incoming.files(Incoming.FileType.MAP)) {
			if (f.fileName().toLowerCase().endsWith(".unr")) return "Unreal Tournament";
			if (f.fileName().toLowerCase().endsWith(".ut2")) return "Unreal Tournament 2004";
			if (f.fileName().toLowerCase().endsWith(".ut3")) return "Unreal Tournament 3";
			if (f.fileName().toLowerCase().endsWith(".un2")) return "Unreal 2";
		}

		return IndexUtils.UNKNOWN;
	}

	public static java.util.Map<String, Double> themes(Package pkg) {
		final java.util.Map<String, Integer> foundThemes = new HashMap<>();

		// this can also work using "Models", but there are issues parsing those for UE2 maps
		pkg.objectsByClassName("Polys").forEach(o -> {
			Polys polys = (Polys)o.object();
			polys.polys.stream()
					   .map(p -> p.texture.get())
					   .filter(n -> n instanceof Import)
					   .map(n -> (Import)n)
					   .forEach(i -> {
						   // find the package a texture came from, which can be allocated to a theme
						   Import current = i;
						   Named parent = i.packageIndex.get();
						   while (parent instanceof Import) {
							   current = (Import)parent;
							   parent = current.packageIndex.get();
						   }

						   // if the package seems to belong to a theme, count its usage
						   String theme = Themes.findTheme(current.name.name);
						   if (theme != null) {
							   foundThemes.compute(theme, (k, v) -> v == null ? 1 : ++v);
						   }
					   });
		});

		// sort and collect the top 5 themes
		java.util.Map<String, Integer> mapThemes = foundThemes.entrySet().stream()
															  .sorted((a, b) -> -a.getValue().compareTo(b.getValue()))
															  .limit(Themes.MAX_THEMES)
															  .collect(Collectors.toMap(java.util.Map.Entry::getKey,
																						java.util.Map.Entry::getValue));

		// for the top 5 themes, give them a percentage value of the total themeable content
		double totalScore = mapThemes.values().stream().mapToInt(e -> e).sum();
		return mapThemes.entrySet()
						.stream()
						.filter(e -> ((double)e.getValue() / totalScore) > Themes.MIN_THRESHOLD)
						.collect(Collectors.toMap(java.util.Map.Entry::getKey,
												  v -> BigDecimal.valueOf((double)v.getValue() / totalScore)
																 .setScale(1, RoundingMode.HALF_UP).doubleValue()
						));
	}

	public static boolean botSupport(Package pkg) {
		return pkg.objectsByClassName("PathNode").stream()
				  .filter(n -> pkg.object(n).property("Paths") instanceof ArrayProperty &&
							   !((ArrayProperty)pkg.object(n).property("Paths")).values.isEmpty())
				  .count() > BOT_PATH_MIN;
	}

}
