package net.shrimpworks.unreal.archive.content.maps;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.Incoming;
import net.shrimpworks.unreal.archive.content.IndexHandler;
import net.shrimpworks.unreal.archive.content.IndexLog;
import net.shrimpworks.unreal.archive.content.IndexResult;
import net.shrimpworks.unreal.archive.content.IndexUtils;
import net.shrimpworks.unreal.packages.IntFile;
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
			m.game = IndexUtils.game(incoming).name;
		}

		Set<IndexResult.NewAttachment> attachments = new HashSet<>();

		try (Package map = map(baseMap)) {
			List<BufferedImage> screenshots = new ArrayList<>();
			if (map.version < 200) {
				scrapeUE12(incoming, m, gameOverride, map, screenshots);
			} else {
				scrapeUE3(incoming, m, map, screenshots);
			}

			screenshots.addAll(IndexUtils.findImageFiles(incoming));
			IndexUtils.saveImages(IndexUtils.SHOT_NAME, m, screenshots, attachments);

			if (m.author.isBlank() || m.author.equals("Unknown")) m.author = IndexUtils.findAuthor(incoming);
			if (m.playerCount.isBlank() || m.playerCount.equals("Unknown")) {
				if (m.gametype.equals("1 on 1")) m.playerCount = "2";
				else if (m.gametype.equals("Single Player")) m.playerCount = "1";
				else m.playerCount = IndexUtils.findPlayerCount(incoming);
			}

			// special case for 1 on 1 maps, which sometimes don't contain the 1on1 at the start
			if (m.gametype.equalsIgnoreCase("DeathMatch") && m.name.startsWith("DM") && m.name.toLowerCase().contains("1on1")) {
				m.gametype = "1 on 1";
			}

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

	private void scrapeUE12(Incoming incoming, Map m, boolean gameOverride, Package map, List<BufferedImage> screenshots) {
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

		try {
			screenshots.addAll(IndexUtils.screenshots(incoming, map, screenshot));
		} catch (Throwable e) {
			incoming.log.log(IndexLog.EntryType.CONTINUE, "Failed to extract screenshot: " + e, e);
		}
	}

	private void scrapeUE3(Incoming incoming, Map m, Package map, List<BufferedImage> screenshots) {
		IndexUtils.readIntFiles(incoming, incoming.files(Incoming.FileType.INI)).findFirst().ifPresent(ini -> {
			ini.sections().forEach(s -> {
				IntFile.Value name = ini.section(s).value("MapName");
				if (name instanceof IntFile.SimpleValue) m.name = ((IntFile.SimpleValue)name).value.trim();

				IntFile.Value title = ini.section(s).value("FriendlyName");
				if (title instanceof IntFile.SimpleValue) m.title = ((IntFile.SimpleValue)title).value.trim();

				IntFile.Value desc = ini.section(s).value("Description");
				if (desc instanceof IntFile.SimpleValue) m.description = ((IntFile.SimpleValue)desc).value.trim();

				IntFile.Value players = ini.section(s).value("NumPlayers");
				if (players instanceof IntFile.SimpleValue) {
					String playerCount = ((IntFile.SimpleValue)players).value.replaceAll("([Pp]layers)", "");
					if (playerCount.toLowerCase().contains("author")) {
						m.playerCount = playerCount.substring(0, playerCount.toLowerCase().indexOf("author")).trim();
						m.author = playerCount.replaceAll(".*(?i)authors?\\s?:?\\s?(.*)", "$1");
					} else {
						m.playerCount = playerCount.toLowerCase(Locale.ROOT).trim();
					}
				}
			});
		});

		try {
			screenshots.addAll(IndexUtils.screenshots(incoming, map, null));
		} catch (Throwable e) {
			incoming.log.log(IndexLog.EntryType.CONTINUE, "Failed to extract screenshots: " + e, e);
		}
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
		return new Package(new PackageReader(mapFile.asChannel(), false));
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

	public static java.util.Map<String, Double> themes(Package pkg) {
		final java.util.Map<String, Integer> foundThemes = new HashMap<>();

		// polygon format of UE3 maps is unknown at the moment, so we cannot interrogate them for texture usage.
		// also UE3 maps no longer use much BSP, so need an alternative approach (mesh usage?)
		if (pkg.version > 199) return java.util.Map.of();

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
		return pkg.version < 117
			? Stream.concat(pkg.objectsByClassName("PathNode").stream(), pkg.objectsByClassName("InventorySpot").stream())
					.filter(n -> pkg.object(n).property("Paths") instanceof ArrayProperty
								 && !((ArrayProperty)pkg.object(n).property("Paths")).values.isEmpty())
					.count() > BOT_PATH_MIN
			: pkg.objectsByClassName("ReachSpec").size() > BOT_PATH_MIN;
	}

}
