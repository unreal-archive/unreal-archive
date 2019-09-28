package net.shrimpworks.unreal.archive.content.mutators;

import java.beans.ConstructorProperties;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.shrimpworks.unreal.archive.content.Content;

public class Mutator extends Content {

	// Game/Type/A/
	private static final String PATH_STRING = "%s/%s/%s/%s/";

	static final String UT_MUTATOR_CLASS = "Engine.Mutator";
	static final String UT_MENU_CLASS = "UMenu.UMenuModMenuItem";
	static final String UT_KEYBINDINGS_CLASS = "UTMenu.UTExtraKeyBindings";
	static final String UT_WEAPON_CLASS = "Botpack.TournamentWeapon";

	static final String UT2_KEYBINDINGS_CLASS = "Xinterface.GUIUserKeyBinding";

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
										  this.hash.substring(0, 2)
		));
	}

	public static class NameDescription {

		public final String name;
		public final String description;

		@ConstructorProperties({ "name", "description" })
		public NameDescription(String name, String description) {
			this.name = name;
			this.description = description;
		}

		public NameDescription(String source) {
			if (source.contains(",")) {
				name = source.substring(0, source.indexOf(","));
				description = source.substring(source.indexOf(",") + 1);
			} else {
				name = source;
				description = "";
			}
		}
	}
}
