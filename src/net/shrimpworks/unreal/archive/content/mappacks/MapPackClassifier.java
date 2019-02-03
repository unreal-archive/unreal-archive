package net.shrimpworks.unreal.archive.content.mappacks;

import java.util.Set;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.content.Classifier;
import net.shrimpworks.unreal.archive.content.Incoming;

public class MapPackClassifier implements Classifier {

	@Override
	public boolean classify(Incoming incoming) {
		Set<Incoming.IncomingFile> maps = incoming.files(Incoming.FileType.MAP);
//		Set<Incoming.IncomingFile> codes = incoming.files(Incoming.FileType.CODE);
		Set<Incoming.IncomingFile> disallowed = incoming.files(Incoming.FileType.INT,
															   Incoming.FileType.INI,
															   Incoming.FileType.PLAYER,
															   Incoming.FileType.UCL);

		// single map
		if (maps.size() <= 1) return false;

		// we're disallowing things which may indicate a mod, mutator or other things, such
		// as voices, models or skins. these things typically require an int or ucl file to
		// define them, so if these are not present, we assume it is a bunch of maps, with
		// some optional code packages (.u files, etc)
		if (!disallowed.isEmpty()) return false;

		// for now, we specifically disallow UT3, since we don't understand its package structure (> version 500?)
		for (Incoming.IncomingFile map : maps) {
			if (Util.extension(map.fileName()).equalsIgnoreCase("ut3")) return false;
		}

		return true;
	}
}
