package net.shrimpworks.unreal.archive.content.mappacks;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.content.Content;

public class MapPack extends Content {

	// Game/Type/A/
	private static final String PATH_STRING = "%s/%s/%s/";

	public List<PackMap> maps = new ArrayList<>();
	public String gametype = "Mixed";

	@Override
	public Path contentPath(Path root) {
		String namePrefix = subGrouping();
		return root.resolve(String.format(PATH_STRING,
										  game,
										  "MapPacks",
										  namePrefix
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
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		MapPack mapPack = (MapPack)o;
		return Objects.equals(maps, mapPack.maps)
			   && Objects.equals(gametype, mapPack.gametype);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), maps, gametype);
	}

	public static class PackMap implements Comparable<PackMap> {

		public String name;
		public String title;
		public String author = "Unknown";

		@Override
		public int compareTo(PackMap o) {
			return name.compareToIgnoreCase(o.name);
		}
	}
}
