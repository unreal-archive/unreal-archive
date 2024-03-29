package org.unrealarchive.content.addons;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.unrealarchive.content.Games;

public class Voice extends Addon {

	// Game/Type/A/
	private static final String PATH_STRING = "%s/%s/%s/%s/";

	public List<String> voices = new ArrayList<>();

	@Override
	public Path contentPath(Path root) {
		String namePrefix = subGrouping();
		return root.resolve(String.format(PATH_STRING,
										  game,
										  "Voices",
										  namePrefix,
										  hashPath()
		));
	}

	@Override
	public String autoDescription() {
		return String.format("%s, a voice pack for %s with %s%s",
							 name, Games.byName(game).bigName,
							 voices.isEmpty()
								 ? "no voices"
								 : voices.size() > 1 ? voices.size() + " voices" : voices.size() + " voice",
							 authorName().equalsIgnoreCase("unknown") ? "" : ", created by " + authorName());
	}

	@Override
	public Set<String> autoTags() {
		Set<String> tags = new HashSet<>(super.autoTags());
		tags.add(name.toLowerCase());
		tags.addAll(voices.stream().map(String::toLowerCase).toList());
		return tags;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Voice other)) return false;
		if (!super.equals(o)) return false;
		return Objects.equals(voices, other.voices);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), voices);
	}
}
