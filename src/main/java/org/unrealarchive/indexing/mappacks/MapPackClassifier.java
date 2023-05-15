package org.unrealarchive.indexing.mappacks;

import java.util.Objects;
import java.util.Set;

import org.unrealarchive.common.Util;
import org.unrealarchive.content.FileType;
import org.unrealarchive.indexing.Classifier;
import org.unrealarchive.indexing.Incoming;
import org.unrealarchive.indexing.IndexUtils;

public class MapPackClassifier implements Classifier {

	@Override
	public boolean classify(Incoming incoming) {
		Set<Incoming.IncomingFile> maps = incoming.files(FileType.MAP);

		// single map
		if (maps.size() <= 1) return false;

		// If we have UT3 maps, we need to do a lot more digging to make sure it's not some other kind of content
		for (Incoming.IncomingFile map : maps) {
			if (Util.extension(map.fileName()).equalsIgnoreCase("ut3")) return isUt3MapPack(incoming, maps);
		}

//		Set<Incoming.IncomingFile> codes = incoming.files(Incoming.FileType.CODE);
		Set<Incoming.IncomingFile> disallowed = incoming.files(FileType.INT,
															   FileType.INI,
															   FileType.PLAYER,
															   FileType.UCL);

		// we're disallowing things which may indicate a mod, mutator or other things, such
		// as voices, models or skins. these things typically require an int or ucl file to
		// define them, so if these are not present, we assume it is a bunch of maps, with
		// some optional code packages (.u files, etc)
		return disallowed.isEmpty();
	}

	private boolean isUt3MapPack(Incoming incoming, Set<Incoming.IncomingFile> maps) {
		Set<Incoming.IncomingFile> inis = incoming.files(FileType.INI);
		long notMaps = IndexUtils.readIntFiles(incoming, inis, true)
								 .filter(Objects::nonNull)
								 .filter(iniFile -> {
									 final boolean maybeMutator = iniFile.sections().stream().anyMatch(
										 s -> s.toLowerCase().contains("mutator"));
									 final boolean maybeChar = iniFile.sections().stream().anyMatch(
										 s -> s.toLowerCase().contains("customchar"));
									 final boolean maybeWeapon = iniFile.sections().stream().anyMatch(
										 s -> s.toLowerCase().contains("weapon"));
									 final boolean maybeGame = iniFile.sections().stream().anyMatch(
										 s -> s.toLowerCase().contains("gamemode"));

									 return maybeChar || maybeMutator || maybeWeapon || maybeGame;
								 })
								 .count();
		return notMaps == 0;
	}
}
