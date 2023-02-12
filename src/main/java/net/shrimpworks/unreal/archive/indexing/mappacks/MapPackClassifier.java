package net.shrimpworks.unreal.archive.indexing.mappacks;

import java.util.Objects;
import java.util.Set;

import net.shrimpworks.unreal.archive.common.Util;
import net.shrimpworks.unreal.archive.indexing.Classifier;
import net.shrimpworks.unreal.archive.indexing.Incoming;
import net.shrimpworks.unreal.archive.indexing.IndexUtils;

public class MapPackClassifier implements Classifier {

	@Override
	public boolean classify(Incoming incoming) {
		Set<Incoming.IncomingFile> maps = incoming.files(Incoming.FileType.MAP);

		// single map
		if (maps.size() <= 1) return false;

		// If we have UT3 maps, we need to do a lot more digging to make sure it's not some other kind of content
		for (Incoming.IncomingFile map : maps) {
			if (Util.extension(map.fileName()).equalsIgnoreCase("ut3")) return isUt3MapPack(incoming, maps);
		}

//		Set<Incoming.IncomingFile> codes = incoming.files(Incoming.FileType.CODE);
		Set<Incoming.IncomingFile> disallowed = incoming.files(Incoming.FileType.INT,
															   Incoming.FileType.INI,
															   Incoming.FileType.PLAYER,
															   Incoming.FileType.UCL);

		// we're disallowing things which may indicate a mod, mutator or other things, such
		// as voices, models or skins. these things typically require an int or ucl file to
		// define them, so if these are not present, we assume it is a bunch of maps, with
		// some optional code packages (.u files, etc)
		return disallowed.isEmpty();
	}

	private boolean isUt3MapPack(Incoming incoming, Set<Incoming.IncomingFile> maps) {
		Set<Incoming.IncomingFile> inis = incoming.files(Incoming.FileType.INI);
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
