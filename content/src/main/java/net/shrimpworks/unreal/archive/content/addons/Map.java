package net.shrimpworks.unreal.archive.content.addons;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import net.shrimpworks.unreal.archive.common.Util;
import net.shrimpworks.unreal.archive.content.Games;

public class Map extends Addon {

	// Game/Type/Gametype/A/[hash]
	private static final String PATH_STRING = "%s/%s/%s/%s/%s/";

	public String gametype = "Unknown";             // Deathmatch
	public String title = "Unknown";                // My Map
	public String playerCount = "Unknown";          // 2 - 4 Players
	public java.util.Map<String, Double> themes = new HashMap<>(); // "Industrial": 0.75, "Tech": 0.25
	public boolean bots = false;                    // true if the map seems to have bot pathing built

	@Override
	public String subGrouping() {
		MapGameTypes.MapGameType gt = MapGameTypes.byName(gametype);
		if (gt != null) {
			String s = name.toLowerCase().trim();
			for (String prefix : gt.mapPrefixes) {
				if (s.startsWith(prefix.toLowerCase())) {
					char first = s.replaceFirst(prefix.toLowerCase(), "").toUpperCase().replaceAll("[^A-Z0-9]", "").charAt(0);
					if (Character.isDigit(first)) first = '0';
					return Character.toString(first);
				}
			}
		}

		return super.subGrouping();
	}

	@Override
	public Path contentPath(Path root) {
		String namePrefix = subGrouping();
		return root.resolve(String.format(PATH_STRING,
										  game,
										  "Maps",
										  gametype,
										  namePrefix,
										  hashPath()
		));
	}

	@Override
	public Path slugPath(Path root) {
		String type = Util.slug(this.contentType.toLowerCase().replaceAll("_", "") + "s");
		String game = Util.slug(this.game);
		String gameType = Util.slug(this.gametype);
		String name = Util.slug(this.name + "_" + this.hash.substring(0, 8));
		return root.resolve(type).resolve(game).resolve(gameType).resolve(subGrouping()).resolve(name);
	}

	@Override
	public String autoDescription() {
		String playerString = playerCount.equalsIgnoreCase("unknown") ? "" : String.format(" %s player", playerCount);
		String authorString = authorName().equalsIgnoreCase("unknown") ? "" : String.format(", created by %s", authorName());
		return String.format("%s, a%s %s map for %s%s",
							 title, playerString, gametype, Games.byName(game).bigName, authorString);
	}

	@Override
	public List<String> autoTags() {
		List<String> tags = new ArrayList<>(super.autoTags());
		tags.add(gametype.toLowerCase());
		if (name.contains("-")) {
			tags.add(name.split("-")[0].toLowerCase());
			tags.add(name.split("-")[1].toLowerCase());
		}
		tags.addAll(themes.keySet().stream().map(String::toLowerCase).toList());
		return tags;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		Map map = (Map)o;
		return Objects.equals(gametype, map.gametype)
			   && Objects.equals(title, map.title)
			   && Objects.equals(playerCount, map.playerCount)
			   && Objects.equals(themes, map.themes)
			   && Objects.equals(bots, map.bots);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), gametype, title, playerCount, themes, bots);
	}
}
