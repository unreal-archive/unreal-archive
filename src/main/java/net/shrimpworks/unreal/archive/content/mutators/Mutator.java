package net.shrimpworks.unreal.archive.content.mutators;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.Games;
import net.shrimpworks.unreal.archive.content.NameDescription;

public class Mutator extends Content {

	// Game/Type/A/
	private static final String PATH_STRING = "%s/%s/%s/%s/";

	public static final String UT_MUTATOR_CLASS = "Engine.Mutator";
	static final String UT_MENU_CLASS = "UMenu.UMenuModMenuItem";
	static final String UT_KEYBINDINGS_CLASS = "UTMenu.UTExtraKeyBindings";
	static final String UT_WEAPON_CLASS = "Botpack.TournamentWeapon";

	static final String UT2_KEYBINDINGS_CLASS = "Xinterface.GUIUserKeyBinding";

	public static final String UT3_MUTATOR_SECTION = "UTUIDataProvider_Mutator";
	static final String UT3_WEAPON_SECTION = "UTUIDataProvider_Weapon";
	static final String UT3_VEHICLE_SECTION = "UTUIDataProvider_Vehicle";

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
		tags.addAll(mutators.stream().map(m -> m.name.toLowerCase()).collect(Collectors.toList()));
		return tags;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Mutator)) return false;
		if (!super.equals(o)) return false;
		Mutator mutator = (Mutator)o;
		return hasConfigMenu == mutator.hasConfigMenu &&
			   hasKeybinds == mutator.hasKeybinds &&
			   Objects.equals(mutators, mutator.mutators) &&
			   Objects.equals(weapons, mutator.weapons) &&
			   Objects.equals(vehicles, mutator.vehicles);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), mutators, weapons, vehicles, hasConfigMenu, hasKeybinds);
	}

}
