package net.shrimpworks.unreal.archive.content.models;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.shrimpworks.unreal.archive.content.Classifier;
import net.shrimpworks.unreal.archive.content.Incoming;
import net.shrimpworks.unreal.archive.content.IndexUtils;
import net.shrimpworks.unreal.packages.IntFile;

/**
 * A model should contain:
 * <p>
 * - At least one .utx file
 * - At least one .int file
 * - One .u file
 * - Zero map files
 * <p>
 * One of the .int files should contain a [public] section, and an entry which follows the format:
 * <pre>
 * [public]
 * Object=(Name=PackageName.ModelName,Class=Class,MetaClass=Botpack.TournamentPlayer,Description="Model Name")
 * </pre>
 */
public class ModelClassifier implements Classifier {

	// if any of these types are present, its probably part of a mod, mutator, or weapon mod, so rather exclude it
	private static final List<String> INVALID_CLASSES = Arrays.asList(
		"engine.mutator", "botpack.tournamentweapon", "botpack.tournamentgameinfo"
	);

	@Override
	public boolean classify(Incoming incoming) {
		Set<Incoming.IncomingFile> intFiles = incoming.files(Incoming.FileType.INT);
		Set<Incoming.IncomingFile> animationFiles = incoming.files(Incoming.FileType.ANIMATION);
		Set<Incoming.IncomingFile> codeFiles = incoming.files(Incoming.FileType.CODE);
		Set<Incoming.IncomingFile> miscFiles = incoming.files(Incoming.FileType.MAP, Incoming.FileType.MUSIC, Incoming.FileType.STATICMESH);
		Set<Incoming.IncomingFile> playerFiles = incoming.files(Incoming.FileType.PLAYER); // ut2004
		Set<Incoming.IncomingFile> upkFiles = incoming.files(Incoming.FileType.PACKAGE); // ut3
		Set<Incoming.IncomingFile> iniFiles = incoming.files(Incoming.FileType.INI);

		// if there are other types of files, we can probably assume its something like a mod
		if (!miscFiles.isEmpty()) return false;

		// there should be at least one texture file
		// hmm, looks like textures might sometimes be built into the mesh or animation files...
		//if (incoming.files(Incoming.FileType.TEXTURE).isEmpty()) return false;

		// a UT2003/4 model should contain a player and animation definition
		if (!playerFiles.isEmpty() && !animationFiles.isEmpty()) return ut2004Model(incoming, playerFiles);

		// a UT3 model should contain a package files and an ini definition
		if (!upkFiles.isEmpty() && !iniFiles.isEmpty()) return ut3Model(incoming, iniFiles);

		// a UT model should have a "code" package which contains the mesh
		if (!intFiles.isEmpty() && !codeFiles.isEmpty()) return utModel(incoming, intFiles);

		return false;
	}

	private boolean utModel(Incoming incoming, Set<Incoming.IncomingFile> intFiles) {
		boolean[] seemsToBeAModel = new boolean[] { false };
		boolean[] probablyNotAModel = new boolean[] { false };

		// search int files for objects describing a model
		IndexUtils.readIntFiles(incoming, intFiles)
				  .filter(Objects::nonNull)
				  .forEach(intFile -> {
					  if (probablyNotAModel[0]) return;

					  IntFile.Section section = intFile.section("public");
					  if (section == null) return;

					  IntFile.ListValue objects = section.asList("Object");
					  for (IntFile.Value value : objects.values) {
						  if (!(value instanceof IntFile.MapValue)) continue;
						  IntFile.MapValue mapVal = (IntFile.MapValue)value;

						  // exclude things which may indicate a mod or similar
						  if (INVALID_CLASSES.contains(mapVal.getOrDefault("MetaClass", "").toLowerCase())) {
							  probablyNotAModel[0] = true;
							  return;
						  }

						  if (mapVal.containsKey("Name")
							  && mapVal.containsKey("MetaClass")
							  && mapVal.containsKey("Description")
							  && mapVal.get("MetaClass").equalsIgnoreCase(Model.UT_PLAYER_CLASS)) {

							  seemsToBeAModel[0] = true;
						  }
					  }
				  });

		return !probablyNotAModel[0] && seemsToBeAModel[0];
	}

	private boolean ut2004Model(Incoming incoming, Set<Incoming.IncomingFile> playerFiles) {
		// indicates a model - presence of a player file indicates a plain skin
		return !incoming.files(Incoming.FileType.ANIMATION).isEmpty();
	}

	private boolean ut3Model(Incoming incoming, Set<Incoming.IncomingFile> iniFiles) {
		// search ini files for things describing a character
		return IndexUtils.readIntFiles(incoming, iniFiles)
						 .filter(Objects::nonNull)
						 .anyMatch(intFile -> {
							 IntFile.Section section = intFile.section(Model.UT3_CHARACTER_DEF);
							 if (section == null) return false;
							 return section.keys().contains("+Characters");
						 });
	}
}
