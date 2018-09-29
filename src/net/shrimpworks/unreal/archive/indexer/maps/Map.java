package net.shrimpworks.unreal.archive.indexer.maps;

import java.nio.file.Path;
import java.util.Objects;

import net.shrimpworks.unreal.archive.indexer.Content;

public class Map extends Content {

	// Type/Game/Gametype/NAME5
	private static final String PATH_STRING = "%s/%s/%s/%s/";

	public String gametype = "Unknown";             // Deathmatch
	public String title = "Unknown";                // My Map
	public String playerCount = "Unknown";          // 2 - 4 Players

	@Override
	public Path contentPath(Path root) {
		String namePrefix = name.toUpperCase().replaceAll("[^A-Z0-9]", "");
		namePrefix = namePrefix.substring(0, Math.min(4, namePrefix.length() - 1));
		return root.resolve(String.format(PATH_STRING,
										  "Maps",
										  game,
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
