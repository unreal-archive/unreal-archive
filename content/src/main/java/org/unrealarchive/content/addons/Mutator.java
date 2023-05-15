package org.unrealarchive.content.addons;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.unrealarchive.content.Games;
import org.unrealarchive.content.NameDescription;

public class Mutator extends Addon {

	// Game/Type/A/
	private static final String PATH_STRING = "%s/%s/%s/%s/";

	public List<NameDescription> mutators = new ArrayList<>(); // mutators contained within the package

	public List<NameDescription> weapons = new ArrayList<>();  // weapons contained within the package
	public List<NameDescription> vehicles = new ArrayList<>(); // vehicles contained within the package

	public boolean hasConfigMenu = false;                      // if the mutator has any custom config menus
	public boolean hasKeybinds = false;                        // if the mutator has custom key bindings

	@Override
	public Path contentPath(Path root) {
		String namePrefix = subGrouping();
		return root.resolve(String.format(PATH_STRING,
										  game,
										  "Mutators",
										  namePrefix,
										  hashPath()
		));
	}

	@Override
	public String autoDescription() {
		return String.format("%s, a %s mutator for %s%s",
							 name,
							 vehicles.isEmpty() && weapons.isEmpty() ? "" :
								 !weapons.isEmpty() && !vehicles.isEmpty()
									 ? "weapon and vehicle"
									 : !weapons.isEmpty()
										 ? "weapon"
										 : "vehicle",
							 Games.byName(game).bigName,
							 authorName().equalsIgnoreCase("unknown") ? "" : ", created by " + authorName());
	}

	@Override
	public List<String> autoTags() {
		List<String> tags = new ArrayList<>(super.autoTags());
		tags.add(name.toLowerCase());
		if (!weapons.isEmpty()) tags.add("weapons");
		if (!vehicles.isEmpty()) tags.add("vehicles");
		tags.addAll(mutators.stream().map(m -> m.name.toLowerCase()).toList());
		return tags;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Mutator other)) return false;
		if (!super.equals(o)) return false;
		return hasConfigMenu == other.hasConfigMenu &&
			   hasKeybinds == other.hasKeybinds &&
			   Objects.equals(mutators, other.mutators) &&
			   Objects.equals(weapons, other.weapons) &&
			   Objects.equals(vehicles, other.vehicles);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), mutators, weapons, vehicles, hasConfigMenu, hasKeybinds);
	}

}
