package org.unrealarchive.indexing.mutators;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import net.shrimpworks.unreal.packages.IntFile;

import org.unrealarchive.content.FileType;
import org.unrealarchive.indexing.Classifier;
import org.unrealarchive.indexing.Incoming;
import org.unrealarchive.indexing.IndexUtils;

/**
 * A Mutator should contain:
 * <p>
 * - At least one .int file or .ucl file
 * - At least one .u file
 * <p>
 * One of the .int files should contain a [public] section, and an entry which follows the format:
 * <pre>
 * [public]
 * Object=(Name=Package.Class,Class=Class,MetaClass=Engine.Mutator,Description="Name,Longer Description")
 * </pre>
 * If a .ucl (UT2003/4) file is present, it should have an entry like (no section header):
 * <pre>
 * Mutator=(ClassName=MutPackage.MutatorClass,GroupName=MyMutator,IconMaterialName=MutatorArt.nosym,
 *          FriendlyName=MutPackage.MutatorClass.FriendlyName,Description=MutPackage.MutatorClass.Description,
 *          FallbackName="My Mutator",FallbackDesc="My Cool Mutator||It Does Stuff")
 * </pre>
 * The .int or .ucl files should contain no gametype, map or other non-mutator definitions
 */
public class MutatorClassifier implements Classifier {

	// if any of these types are present, its probably part of a mod, mutator, or weapon mod, so rather exclude it
	private static final List<String> INVALID_CLASSES = List.of(".voice", "tournamentgameinfo", "tournamentplayer", "gameinfo");

	private static final List<String> INVALID_UT3_SECTIONS = List.of("UTUIDataProvider_GameModeInfo".toLowerCase());

	public static final String UT_MUTATOR_CLASS = "Engine.Mutator";
	static final String UT_MENU_CLASS = "UMenu.UMenuModMenuItem";
	static final String UT_KEYBINDINGS_CLASS = "UTMenu.UTExtraKeyBindings";
	static final String UT_WEAPON_CLASS = "Botpack.TournamentWeapon";

	static final String UT2_KEYBINDINGS_CLASS = "Xinterface.GUIUserKeyBinding";

	public static final String UT3_MUTATOR_SECTION = "UTUIDataProvider_Mutator";
	static final String UT3_WEAPON_SECTION = "UTUIDataProvider_Weapon";
	static final String UT3_VEHICLE_SECTION = "UTUIDataProvider_Vehicle";

	@Override
	public boolean classify(Incoming incoming) {
		Set<Incoming.IncomingFile> intFiles = incoming.files(FileType.INT);
		Set<Incoming.IncomingFile> iniFiles = incoming.files(FileType.INI);
		Set<Incoming.IncomingFile> uclFiles = incoming.files(FileType.UCL);
		Set<Incoming.IncomingFile> codeFiles = incoming.files(FileType.CODE);

		Set<Incoming.IncomingFile> miscFiles = incoming.files(FileType.MAP, FileType.PLAYER);

		// if there are other types of files, we can probably assume its something like a mod
		if (!miscFiles.isEmpty()) return false;

		// there should be an int file, along with a sound or code package
		if ((intFiles.isEmpty() || uclFiles.isEmpty() || iniFiles.isEmpty()) && codeFiles.isEmpty()) return false;

		boolean utMutator = !intFiles.isEmpty() && checkUTMutator(incoming, intFiles);
		boolean ut2004Mutator = !uclFiles.isEmpty() && !utMutator && checkUT2004Mutator(incoming, uclFiles);
		boolean ut3Mutator = !iniFiles.isEmpty() && !utMutator && !ut2004Mutator && checkUT3Mutator(incoming, iniFiles);

		return utMutator || ut2004Mutator || ut3Mutator;
	}

	private boolean checkUTMutator(Incoming incoming, Set<Incoming.IncomingFile> intFiles) {
		final AtomicBoolean seemsToBeAMutator = new AtomicBoolean(false);
		final AtomicBoolean probablyNotAMutator = new AtomicBoolean(false);

		// search int files for objects describing a skin
		IndexUtils.readIntFiles(incoming, intFiles)
				  .filter(Objects::nonNull)
				  .forEach(intFile -> {
					  if (probablyNotAMutator.get()) return;

					  IntFile.Section section = intFile.section("public");
					  if (section == null) return;

					  IntFile.ListValue objects = section.asList("Object");
					  for (IntFile.Value value : objects.values()) {
						  if (!(value instanceof IntFile.MapValue mapVal)) continue;

						  if (!mapVal.containsKey("MetaClass")) continue;

						  // exclude things which may indicate a mod or similar
						  if (INVALID_CLASSES.stream().anyMatch(s -> mapVal.get("MetaClass").toLowerCase().contains(s))) {
							  probablyNotAMutator.set(true);
							  return;
						  }

						  if (UT_MUTATOR_CLASS.equalsIgnoreCase(mapVal.get("MetaClass"))) {
							  seemsToBeAMutator.set(true);
						  }
					  }
				  });

		return !probablyNotAMutator.get() && seemsToBeAMutator.get();
	}

	private boolean checkUT2004Mutator(Incoming incoming, Set<Incoming.IncomingFile> uclFiles) {
		final AtomicBoolean seemsToBeAMutator = new AtomicBoolean(false);
		final AtomicBoolean probablyNotAMutator = new AtomicBoolean(false);

		// search int files for objects describing a skin
		IndexUtils.readIntFiles(incoming, uclFiles, true)
				  .filter(Objects::nonNull)
				  .forEach(uclFile -> {
					  if (probablyNotAMutator.get()) return;

					  IntFile.Section section = uclFile.section("root");
					  if (section == null) return;

					  if (section.keys().contains("Map")
						  || section.keys().contains("Game")) {
						  probablyNotAMutator.set(true);
						  return;
					  }

					  if (section.keys().contains("Mutator")) {
						  seemsToBeAMutator.set(true);
					  }
				  });

		return !probablyNotAMutator.get() && seemsToBeAMutator.get();
	}

	private boolean checkUT3Mutator(Incoming incoming, Set<Incoming.IncomingFile> iniFiles) {
		// search ini files for things describing a character
		return IndexUtils.readIntFiles(incoming, iniFiles)
						 .filter(Objects::nonNull)
						 .anyMatch(iniFile ->
									   iniFile.sections()
											  .stream()
											  .noneMatch(s -> INVALID_UT3_SECTIONS
												  .stream()
												  .anyMatch(n -> s.toLowerCase().trim().endsWith(n.toLowerCase())))
									   && iniFile.sections()
												 .stream()
												 .anyMatch(s -> s.toLowerCase().trim().endsWith(UT3_MUTATOR_SECTION.toLowerCase())));
	}
}
