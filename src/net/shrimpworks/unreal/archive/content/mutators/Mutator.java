package net.shrimpworks.unreal.archive.content.mutators;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.shrimpworks.unreal.archive.content.Content;

public class Mutator extends Content {

	// Game/Type/A/
	private static final String PATH_STRING = "%s/%s/%s/";

	static final String UT_MUTATOR_CLASS = "Engine.Mutator";
	static final String UT_MENU_CLASS = "UMenu.UMenuModMenuItem";
	static final String UT_KEYBINDINGS_CLASS = "UTMenu.UTExtraKeyBindings";

	static final String UT2_KEYBINDINGS_CLASS = "Xinterface.GUIUserKeyBinding";

	public List<String> mutators = new ArrayList<>(); // mutators contained within the package

	public List<String> weapons = new ArrayList<>();  // weapons contained within the package
	public List<String> vehicles = new ArrayList<>(); // vehicles contained within the package

	public boolean config = false;                    // if the mutator has any custom config menus
	public boolean keybinds = false;                  // if the mutator has custom key bindings

	@Override
	public Path contentPath(Path root) {
		String namePrefix = subGrouping();
		return root.resolve(String.format(PATH_STRING,
										  game,
										  "Voices",
										  namePrefix
		));
	}
}
