package org.unrealarchive.content.addons;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.unrealarchive.common.Util;
import org.unrealarchive.content.AuthorNames;
import org.unrealarchive.content.Games;

public class MapPack extends Addon {

	// Game/Type/A/
	private static final String PATH_STRING = "%s/%s/%s/%s/";

	public List<PackMap> maps = new ArrayList<>();
	public String gametype = "Mixed";
	public java.util.Map<String, Double> themes = new HashMap<>();

	@Override
	public Path contentPath(Path root) {
		String namePrefix = subGrouping();
		return root.resolve(String.format(PATH_STRING,
										  game,
										  "MapPacks",
										  namePrefix,
										  hashPath()
		));
	}

	@Override
	public Path slugPath(Path root) {
		String game = Util.slug(this.game);
		String type = Util.slug(this.contentType.toLowerCase().replaceAll("_", "") + "s");
		String gameType = Util.slug(this.gametype);
		String name = Util.slug(this.name + "_" + this.hash.substring(0, 8));
		return root.resolve(game).resolve(type).resolve(gameType).resolve(subGrouping()).resolve(name);
	}

	@Override
	public String autoDescription() {
		return String.format("%s, a %s map pack for %s containing %d maps, created by %s",
							 name, gametype, Games.byName(game).bigName, maps.size(), authorName());
	}

	@Override
	public Set<String> autoTags() {
		Set<String> tags = new HashSet<>(super.autoTags());
		tags.add(gametype.toLowerCase());
		tags.addAll(maps.stream().filter(m -> m.name.contains("-"))
						.map(m -> m.name.split("-")[0].toLowerCase()).distinct().toList());
		tags.addAll(maps.stream().filter(m -> m.name.contains("-"))
						.map(m -> m.name.split("-")[1].toLowerCase()).distinct().toList());
		tags.addAll(themes.keySet().stream().map(String::toLowerCase).toList());
		return tags;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		MapPack mapPack = (MapPack)o;
		return Objects.equals(maps, mapPack.maps)
			   && Objects.equals(gametype, mapPack.gametype)
			   && Objects.equals(themes, mapPack.themes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), maps, gametype, themes);
	}

	public static class PackMap implements Comparable<PackMap> {

		public String name;
		public String title;
		public String author = "Unknown";

		public PackMap() {
		}

		public PackMap(String name, String title, String author) {
			this.name = name;
			this.title = title;
			this.author = author;
		}

		public String authorName() {
			return AuthorNames.nameFor(author);
		}

		@Override
		public int compareTo(PackMap o) {
			return name.compareToIgnoreCase(o.name);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			PackMap packMap = (PackMap)o;
			return Objects.equals(name, packMap.name) && Objects.equals(title, packMap.title) &&
				   Objects.equals(author, packMap.author);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, title, author);
		}
	}
}
