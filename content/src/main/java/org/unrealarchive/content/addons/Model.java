package org.unrealarchive.content.addons;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.unrealarchive.content.Games;

public class Model extends Addon {

	// Game/Type/A/
	private static final String PATH_STRING = "%s/%s/%s/%s/";

	public List<String> models = new ArrayList<>();
	public List<String> skins = new ArrayList<>();

	@Override
	public Path contentPath(Path root) {
		String namePrefix = subGrouping();
		return root.resolve(String.format(PATH_STRING,
										  game,
										  "Models",
										  namePrefix,
										  hashPath()
		));
	}

	@Override
	public String autoDescription() {
		return String.format("%s, a player model for %s with %s%s%s",
							 name, Games.byName(game).bigName,
							 models.isEmpty()
								 ? "no characters"
								 : models.size() > 1 ? models.size() + " characters" : models.size() + " character",
							 skins.isEmpty()
								 ? ""
								 : String.format(" and %s", skins.size() > 1 ? skins.size() + " skins" : skins.size() + " skin"),
							 authorName().equalsIgnoreCase("unknown") ? "" : ", created by " + authorName());
	}

	@Override
	public Set<String> autoTags() {
		Set<String> tags = new HashSet<>(super.autoTags());
		tags.add(name.toLowerCase());
		tags.addAll(models.stream().map(String::toLowerCase).toList());
		tags.addAll(skins.stream().map(String::toLowerCase).toList());
		return tags;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Model other)) return false;
		if (!super.equals(o)) return false;
		return Objects.equals(models, other.models) &&
			   Objects.equals(skins, other.skins);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), models, skins);
	}
}
