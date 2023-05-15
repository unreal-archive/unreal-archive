package org.unrealarchive.indexing.voices;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import net.shrimpworks.unreal.packages.IntFile;

import org.unrealarchive.content.FileType;
import org.unrealarchive.indexing.Classifier;
import org.unrealarchive.indexing.Incoming;
import org.unrealarchive.indexing.IndexUtils;

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
	private static final List<String> INVALID_CLASSES = List.of(
		"engine.mutator", "botpack.tournamentweapon", "botpack.tournamentgameinfo", "botpack.tournamentplayer"
	);

	static final Pattern UT_VOICE_MATCH = Pattern.compile("Botpack\\.Voice.+?", Pattern.CASE_INSENSITIVE);
	static final String UT2_VOICE_CLASS = "XGame.xVoicePack";

	@Override
	public boolean classify(Incoming incoming) {
		Set<Incoming.IncomingFile> intFiles = incoming.files(FileType.INT);
		Set<Incoming.IncomingFile> codeFiles = incoming.files(FileType.CODE);
		Set<Incoming.IncomingFile> soundFiles = incoming.files(FileType.SOUNDS);

		Set<Incoming.IncomingFile> miscFiles = incoming.files(FileType.MAP,
															  FileType.MUSIC,
															  FileType.STATICMESH,
															  FileType.ANIMATION,
															  FileType.PLAYER);

		// if there are other types of files, we can probably assume its something like a mod
		if (!miscFiles.isEmpty()) return false;

		// there should be an int file, along with a sound or code package
		if (intFiles.isEmpty() || (codeFiles.isEmpty() && soundFiles.isEmpty())) return false;

		// a UT model should have a "code" package which contains the mesh
		return checkVoice(incoming, intFiles);
	}

	private boolean checkVoice(Incoming incoming, Set<Incoming.IncomingFile> intFiles) {
		final AtomicBoolean seemsToBeAVoice = new AtomicBoolean(false);
		final AtomicBoolean probablyNotAVoice = new AtomicBoolean(false);

		// search int files for objects describing a skin
		IndexUtils.readIntFiles(incoming, intFiles)
				  .filter(Objects::nonNull)
				  .forEach(intFile -> {
					  if (probablyNotAVoice.get()) return;

					  IntFile.Section section = intFile.section("public");
					  if (section == null) return;

					  IntFile.ListValue objects = section.asList("Object");
					  for (IntFile.Value value : objects.values()) {
						  if (!(value instanceof IntFile.MapValue mapVal)) continue;

						  if (!mapVal.containsKey("MetaClass")) continue;

						  // exclude things which may indicate a mod or similar
						  if (INVALID_CLASSES.contains(mapVal.get("MetaClass").toLowerCase())) {
							  probablyNotAVoice.set(true);
							  return;
						  }

						  // UT2003/4 check
						  if (mapVal.get("MetaClass").equalsIgnoreCase(UT2_VOICE_CLASS)) {
							  seemsToBeAVoice.set(true);
							  return;
						  }

						  // UT check
						  if (UT_VOICE_MATCH.matcher(mapVal.get("MetaClass")).matches()) {
							  seemsToBeAVoice.set(true);
						  }
					  }
				  });

		return !probablyNotAVoice.get() && seemsToBeAVoice.get();
	}

}
