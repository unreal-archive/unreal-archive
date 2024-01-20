package org.unrealarchive.content.addons;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.unrealarchive.content.Games;
import org.unrealarchive.content.NameDescription;

public class Announcer extends Addon {

	// Game/Type/A/
	private static final String PATH_STRING = "%s/%s/%s/%s/";

	public List<NameDescription> announcers = new ArrayList<>();

	@Override
	public Path contentPath(Path root) {
		String namePrefix = subGrouping();
		return root.resolve(String.format(PATH_STRING,
										  game,
										  "Announcers",
										  namePrefix,
										  hashPath()
		));
	}

	@Override
	public String autoDescription() {
		return String.format("%s, an announcer pack for %s with %s%s",
							 name, Games.byName(game).bigName,
							 announcers.isEmpty()
								 ? "no announcers"
								 : announcers.size() > 1 ? announcers.size() + " announcers" : announcers.size() + " announcer",
							 authorName().equalsIgnoreCase("unknown") ? "" : ", created by " + authorName());
	}

	@Override
	public Set<String> autoTags() {
		Set<String> tags = new HashSet<>(super.autoTags());
		tags.add(name.toLowerCase());
		tags.addAll(announcers.stream().map(m -> m.name.toLowerCase()).toList());
		return tags;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Announcer other)) return false;
		if (!super.equals(o)) return false;
		return Objects.equals(announcers, other.announcers);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), announcers);
	}
}
