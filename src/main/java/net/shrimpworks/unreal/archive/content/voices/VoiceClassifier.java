package net.shrimpworks.unreal.archive.content.voices;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.shrimpworks.unreal.archive.content.Classifier;
import net.shrimpworks.unreal.archive.content.Incoming;
import net.shrimpworks.unreal.archive.content.IndexUtils;
import net.shrimpworks.unreal.packages.IntFile;

/**
 * A voice should contain:
 * <p>
 * - At least one .int file
 * - At least one .u or .uax file
 * <p>
 * One of the .int files should contain a [public] section, and an entry which follows the format:
 * <p>
 * UT:
 * <pre>
 * [public]
 * Object=(Name=Package.VoicePackName,Class=Class,MetaClass=BotPack.Voice[Male|Female|?],Description="Voice Name")
 * </pre>
 * UT 2003/4 ($NAME section seems optional):
 * <pre>
 * [public]
 * Object=(Name=Package.$Name,Class=Class,MetaClass=XGame.xVoicePack)
 * [$Name]
 * VoicePackName="Voice Name"
 * </pre>
 */
public class VoiceClassifier implements Classifier {

	// if any of these types are present, its probably part of a mod, mutator, or weapon mod, so rather exclude it
	private static final List<String> INVALID_CLASSES = Arrays.asList(
			"engine.mutator", "botpack.tournamentweapon", "botpack.tournamentgameinfo", "botpack.tournamentplayer"
	);

	@Override
	public boolean classify(Incoming incoming) {
		Set<Incoming.IncomingFile> intFiles = incoming.files(Incoming.FileType.INT);
		Set<Incoming.IncomingFile> codeFiles = incoming.files(Incoming.FileType.CODE);
		Set<Incoming.IncomingFile> soundFiles = incoming.files(Incoming.FileType.SOUNDS);

		Set<Incoming.IncomingFile> miscFiles = incoming.files(Incoming.FileType.MAP,
															  Incoming.FileType.MUSIC,
															  Incoming.FileType.STATICMESH,
															  Incoming.FileType.ANIMATION,
															  Incoming.FileType.PLAYER);

		// if there are other types of files, we can probably assume its something like a mod
		if (!miscFiles.isEmpty()) return false;

		// there should be an int file, along with a sound or code package
		if (intFiles.isEmpty() || (codeFiles.isEmpty() && soundFiles.isEmpty())) return false;

		// a UT model should have a "code" package which contains the mesh
		return checkVoice(incoming, intFiles);
	}

	private boolean checkVoice(Incoming incoming, Set<Incoming.IncomingFile> intFiles) {
		boolean[] seemsToBeAVoice = new boolean[] { false };
		boolean[] probablyNotAVoice = new boolean[] { false };

		// search int files for objects describing a skin
		IndexUtils.readIntFiles(incoming, intFiles)
				  .filter(Objects::nonNull)
				  .forEach(intFile -> {
					  if (probablyNotAVoice[0]) return;

					  IntFile.Section section = intFile.section("public");
					  if (section == null) return;

					  IntFile.ListValue objects = section.asList("Object");
					  for (IntFile.Value value : objects.values) {
						  if (!(value instanceof IntFile.MapValue)) continue;
						  IntFile.MapValue mapVal = (IntFile.MapValue)value;

						  if (!mapVal.containsKey("MetaClass")) continue;

						  // exclude things which may indicate a mod or similar
						  if (INVALID_CLASSES.contains(mapVal.get("MetaClass").toLowerCase())) {
							  probablyNotAVoice[0] = true;
							  return;
						  }

						  // UT2003/4 check
						  if (mapVal.get("MetaClass").equalsIgnoreCase(Voice.UT2_VOICE_CLASS)) {
							  seemsToBeAVoice[0] = true;
							  return;
						  }

						  // UT check
						  if (Voice.UT_VOICE_MATCH.matcher(mapVal.get("MetaClass")).matches()) {
							  seemsToBeAVoice[0] = true;
						  }
					  }
				  });

		return !probablyNotAVoice[0] && seemsToBeAVoice[0];
	}

}
