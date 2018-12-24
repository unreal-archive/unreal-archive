package net.shrimpworks.unreal.archive.indexer.models;

import java.util.Objects;
import java.util.Set;

import net.shrimpworks.unreal.archive.indexer.Classifier;
import net.shrimpworks.unreal.archive.indexer.Incoming;
import net.shrimpworks.unreal.archive.indexer.IndexUtils;
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

	@Override
	public boolean classify(Incoming incoming) {
		Set<Incoming.IncomingFile> intFiles = incoming.files(Incoming.FileType.INT);
		Set<Incoming.IncomingFile> playerFiles = incoming.files(Incoming.FileType.PLAYER);
		Set<Incoming.IncomingFile> animationFiles = incoming.files(Incoming.FileType.ANIMATION);
		Set<Incoming.IncomingFile> codeFiles = incoming.files(Incoming.FileType.CODE);
		Set<Incoming.IncomingFile> miscFiles = incoming.files(Incoming.FileType.MAP, Incoming.FileType.MUSIC, Incoming.FileType.STATICMESH);

		// there should be no maps in a model... otherwise this may be a mod
		if (!incoming.files(Incoming.FileType.MAP).isEmpty()) return false;

		// if there are other types of files, we can probably assume its something like a mod
		if (!miscFiles.isEmpty()) return false;

		// there should be at least one texture file
		// hmm, looks like textures might sometimes be built into the mesh or animation files...
		//if (incoming.files(Incoming.FileType.TEXTURE).isEmpty()) return false;

		// a UT2003/4 model should contain a player and animation definition
		if (!playerFiles.isEmpty() && !animationFiles.isEmpty()) return ut2004Model(incoming, playerFiles);

		// a UT model should have a "code" package which contains the mesh
		if (!intFiles.isEmpty() && !codeFiles.isEmpty()) return utModel(incoming, intFiles);

		return false;
	}

	private boolean utModel(Incoming incoming, Set<Incoming.IncomingFile> intFiles) {
		boolean[] seemsToBeAModel = new boolean[] { false };

		// search int files for objects describing a skin
		IndexUtils.readIntFiles(incoming, intFiles)
				  .filter(Objects::nonNull)
				  .forEach(intFile -> {
					  IntFile.Section section = intFile.section("public");
					  if (section == null) return;

					  IntFile.ListValue objects = section.asList("Object");
					  for (IntFile.Value value : objects.values) {
						  if (value instanceof IntFile.MapValue
							  && ((IntFile.MapValue)value).value.containsKey("Name")
							  && ((IntFile.MapValue)value).value.containsKey("MetaClass")
							  && ((IntFile.MapValue)value).value.containsKey("Description")
							  && ((IntFile.MapValue)value).value.get("MetaClass").equalsIgnoreCase(Model.UT_PLAYER_CLASS)) {

							  seemsToBeAModel[0] = true;
							  return;
						  }
					  }
				  });

		return seemsToBeAModel[0];
	}

	private boolean ut2004Model(Incoming incoming, Set<Incoming.IncomingFile> playerFiles) {
		// indicates a model - presence of a player file indicates a plain skin
		return !incoming.files(Incoming.FileType.ANIMATION).isEmpty();
	}
}
