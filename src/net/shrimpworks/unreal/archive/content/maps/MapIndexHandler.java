package net.shrimpworks.unreal.archive.content.maps;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import net.shrimpworks.unreal.packages.entities.objects.Object;
import net.shrimpworks.unreal.packages.entities.properties.IntegerProperty;
import net.shrimpworks.unreal.packages.entities.properties.Property;
import net.shrimpworks.unreal.packages.entities.properties.StringProperty;

public class MapIndexHandler implements IndexHandler<Map> {

	private static final Pattern SP_MATCH = Pattern.compile("(.+)?(single ?player|cooperative)([\\s:]+)?yes(\\s+)?",
															Pattern.CASE_INSENSITIVE);

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

		// TODO find .txt file in content root and scan for dates, authors, etc

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
			ExportedObject levelInfo = maybeLevelInfo.iterator().next();

			if (levelInfo == null) throw new IllegalStateException("No LevelInfo in the map?!");

			Object level = levelInfo.object();

			// read some basic level info
			Property author = level.property("Author");
			Property title = level.property("Title");
			Property description = level.property("Description");

			if (author != null) m.author = ((StringProperty)author).value.trim();
			if (title != null) m.title = ((StringProperty)title).value.trim();
			if (description != null) m.description = ((StringProperty)description).value.trim();

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

		if (incoming.submission.filePath.toString().toLowerCase().contains("SinglePlayer")
			|| incoming.submission.filePath.toString().toLowerCase().contains("COOP")) {
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

}
