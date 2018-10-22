package net.shrimpworks.unreal.archive.indexer.maps;

import java.nio.file.Path;
import java.util.Objects;

import net.shrimpworks.unreal.archive.indexer.Content;

public class Map extends Content {

	// Game/Type/Gametype/A/
	private static final String PATH_STRING = "%s/%s/%s/%s/";

	public String gametype = "Unknown";             // Deathmatch
	public String title = "Unknown";                // My Map
	public String playerCount = "Unknown";          // 2 - 4 Players

	@Override
	public String subGrouping() {
		GameTypes.GameType gt = GameTypes.byName(gametype);
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
										  namePrefix
		));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		Map map = (Map)o;
		return Objects.equals(gametype, map.gametype)
			   && Objects.equals(title, map.title)
			   && Objects.equals(playerCount, map.playerCount);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), gametype, title, playerCount);
	}
}
