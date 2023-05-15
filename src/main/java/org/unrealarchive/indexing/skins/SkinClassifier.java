package org.unrealarchive.indexing.skins;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unrealarchive.indexing.Classifier;
import org.unrealarchive.content.FileType;
import org.unrealarchive.indexing.Incoming;
import org.unrealarchive.indexing.IndexUtils;
import net.shrimpworks.unreal.packages.IntFile;

/**
 * A skin should contain:
 * <p>
 * - At least one .utx file
 * - One .int file
 * <p>
 * The .int file should contain a [public] section, and an entry which follows the format:
 * <pre>
 * [public]
 * Object=(Name=ModelReference_Something.tex1,Class=Texture,Description="Character")
 * </pre>
 * <p>
 * If there's a .u file, or more .int files (with other contents), it's probably a model.
 */
public class SkinClassifier implements Classifier {

	public static final Pattern NAME_MATCH = Pattern.compile(".+?\\..{4}\\d");
	static final Pattern NAME_MATCH_UNREAL = Pattern.compile(".+?\\.(.+?)");
	static final Pattern FACE_MATCH = Pattern.compile(".+?\\..{4}\\d[a-zA-Z0-9]+");
	static final Pattern FACE_PORTRAIT_MATCH = Pattern.compile("(.+?)\\.(.+?5[a-zA-Z0-9]+)"); // (something_lol).(word5name)
	static final Pattern TEAM_MATCH = Pattern.compile(".+?\\..+?\\dT_\\d", Pattern.CASE_INSENSITIVE);

	@Override
	public boolean classify(Incoming incoming) {
		Set<Incoming.IncomingFile> intFiles = incoming.files(FileType.INT);
		Set<Incoming.IncomingFile> playerFiles = incoming.files(FileType.PLAYER);
		Set<Incoming.IncomingFile> codeFiles = incoming.files(FileType.CODE);

		// presence of a .u package probably indicates a model
		if (!codeFiles.isEmpty()) return false;

		// there should be no maps in a skin... otherwise this may be a mod
		if (!incoming.files(FileType.MAP).isEmpty()) return false;

		// more often than not multiple ints probably indicates a model
//		if (intFiles.size() != 1 && playerFiles.size() != 1) return false;

		if (incoming.files(FileType.TEXTURE).isEmpty()) return false;

		if (!playerFiles.isEmpty()) return ut2004Skin(incoming, playerFiles);
		else if (!intFiles.isEmpty()) {
			return utSkin(incoming, intFiles) || unrealSkin(incoming, intFiles);
		}

		return false;
	}

	private boolean utSkin(Incoming incoming, Set<Incoming.IncomingFile> intFiles) {
		final AtomicBoolean seemsToBeASkin = new AtomicBoolean(false);

		// search int files for objects describing a skin
		IndexUtils.readIntFiles(incoming, intFiles)
				  .filter(Objects::nonNull)
				  .forEach(intFile -> {
					  IntFile.Section section = intFile.section("public");
					  if (section == null) return;

					  IntFile.ListValue objects = section.asList("Object");
					  for (IntFile.Value value : objects.values()) {
						  if (value instanceof IntFile.MapValue
							  && ((IntFile.MapValue)value).containsKey("Name")
							  && ((IntFile.MapValue)value).containsKey("Class")
							  && ((IntFile.MapValue)value).containsKey("Description")
							  && ((IntFile.MapValue)value).get("Class").equalsIgnoreCase("Texture")) {

							  Matcher m = NAME_MATCH.matcher(((IntFile.MapValue)value).get("Name"));
							  if (m.matches()) {
								  seemsToBeASkin.set(true);
								  return;
							  }
						  }
					  }

				  });

		return seemsToBeASkin.get();
	}

	private boolean unrealSkin(Incoming incoming, Set<Incoming.IncomingFile> intFiles) {
		final AtomicBoolean seemsToBeASkin = new AtomicBoolean(false);

		// search int files for objects describing a skin
		IndexUtils.readIntFiles(incoming, intFiles)
				  .filter(Objects::nonNull)
				  .forEach(intFile -> {
					  IntFile.Section section = intFile.section("public");
					  if (section == null) return;

					  IntFile.ListValue objects = section.asList("Object");
					  for (IntFile.Value value : objects.values()) {
						  if (value instanceof IntFile.MapValue
							  && (!((IntFile.MapValue)value).containsKey("Description"))
							  && ((IntFile.MapValue)value).containsKey("Name")
							  && ((IntFile.MapValue)value).containsKey("Class")
							  && ((IntFile.MapValue)value).get("Class").equalsIgnoreCase("Texture")) {

							  Matcher m = NAME_MATCH_UNREAL.matcher(((IntFile.MapValue)value).get("Name"));
							  if (m.matches()) {
								  seemsToBeASkin.set(true);
								  return;
							  }
						  }
					  }

				  });

		return seemsToBeASkin.get();
	}

	private boolean ut2004Skin(Incoming incoming, Set<Incoming.IncomingFile> playerFiles) {
		// indicates a model - presence of a player file indicates a plain skin
		return incoming.files(FileType.ANIMATION).isEmpty();
	}
}
