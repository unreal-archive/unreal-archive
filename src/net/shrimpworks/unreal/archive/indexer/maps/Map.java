package net.shrimpworks.unreal.archive.indexer.maps;

import java.nio.file.Path;
import java.util.Objects;

import net.shrimpworks.unreal.archive.indexer.Content;

public class Map extends Content {

	// Game/Type/Gametype/NAME5/Name-[hash8]
	private static final String PATH_STRING = "%s/%s/%s/%s/%s_[%s]";

	public String gametype = "Unknown";             // Deathmatch
	public String title = "Unknown";                // My Map
	public String playerCount = "Unknown";          // 2 - 4 Players

	@Override
	public Path contentPath(Path root) {
		String basicName = name.toUpperCase().replaceAll("[^A-Z0-9]", "");
		basicName = basicName.substring(0, Math.min(4, basicName.length() - 1));
		return root.resolve(String.format(PATH_STRING,
										  game,
										  "Maps",
										  gametype,
										  basicName,
										  name,
										  hash.substring(0, 8)
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
