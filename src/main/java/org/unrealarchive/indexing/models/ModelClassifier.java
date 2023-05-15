package org.unrealarchive.indexing.models;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.unrealarchive.indexing.Classifier;
import org.unrealarchive.content.FileType;
import org.unrealarchive.indexing.Incoming;
import org.unrealarchive.indexing.IndexUtils;
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
	private static final List<String> INVALID_CLASSES = List.of(
		"engine.mutator", "botpack.tournamentweapon", "botpack.tournamentgameinfo"
	);

	static final String UT_PLAYER_CLASS = "Botpack.TournamentPlayer";
	static final String RUNE_PLAYER_CLASS = "RuneI.RunePlayer";
	static final String UT3_CHARACTER_DEF = "UTGame.UTCustomChar_Data";
	static final Pattern NAME_MATCH = Pattern.compile(".+?\\..+?");

	@Override
	public boolean classify(Incoming incoming) {
		Set<Incoming.IncomingFile> intFiles = incoming.files(FileType.INT);
		Set<Incoming.IncomingFile> animationFiles = incoming.files(FileType.ANIMATION);
		Set<Incoming.IncomingFile> codeFiles = incoming.files(FileType.CODE);
		Set<Incoming.IncomingFile> miscFiles = incoming.files(FileType.MAP, FileType.MUSIC, FileType.STATICMESH);
		Set<Incoming.IncomingFile> playerFiles = incoming.files(FileType.PLAYER); // ut2004
		Set<Incoming.IncomingFile> upkFiles = incoming.files(FileType.PACKAGE); // ut3
		Set<Incoming.IncomingFile> iniFiles = incoming.files(FileType.INI);

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
		final AtomicBoolean probablyNotAModel = new AtomicBoolean(false);
		final AtomicBoolean seemsToBeAModel = new AtomicBoolean(false);

		// search int files for objects describing a model
		IndexUtils.readIntFiles(incoming, intFiles)
				  .filter(Objects::nonNull)
				  .forEach(intFile -> {
					  if (probablyNotAModel.get()) return;

					  IntFile.Section section = intFile.section("public");
					  if (section == null) return;

					  IntFile.ListValue objects = section.asList("Object");
					  for (IntFile.Value value : objects.values()) {
						  if (!(value instanceof IntFile.MapValue mapVal)) continue;

						  // exclude things which may indicate a mod or similar
						  if (INVALID_CLASSES.contains(mapVal.getOrDefault("MetaClass", "").toLowerCase())) {
							  probablyNotAModel.setOpaque(true);
							  return;
						  }

						  if (mapVal.containsKey("Name")
							  && mapVal.containsKey("MetaClass")
							  && mapVal.containsKey("Description")
							  && (mapVal.get("MetaClass").equalsIgnoreCase(UT_PLAYER_CLASS)
								  || mapVal.get("MetaClass").equalsIgnoreCase(RUNE_PLAYER_CLASS))
						  ) {
							  seemsToBeAModel.set(true);
						  }
					  }
				  });

		return !probablyNotAModel.get() && seemsToBeAModel.get();
	}

	private boolean ut2004Model(Incoming incoming, Set<Incoming.IncomingFile> playerFiles) {
		// indicates a model - presence of a player file indicates a plain skin
		return !incoming.files(FileType.ANIMATION).isEmpty();
	}

	private boolean ut3Model(Incoming incoming, Set<Incoming.IncomingFile> iniFiles) {
		// search ini files for things describing a character
		return IndexUtils.readIntFiles(incoming, iniFiles)
						 .filter(Objects::nonNull)
						 .anyMatch(intFile -> {
							 IntFile.Section section = intFile.section(UT3_CHARACTER_DEF);
							 if (section == null) return false;
							 return section.keys().contains("+Characters");
						 });
	}
}
