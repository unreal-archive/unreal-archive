package net.shrimpworks.unreal.archive.content.mappacks;

import java.util.Set;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.content.Classifier;
import net.shrimpworks.unreal.archive.content.Incoming;

public class MapPackClassifier implements Classifier {

	@Override
	public boolean classify(Incoming incoming) {
		Set<Incoming.IncomingFile> maps = incoming.files(Incoming.FileType.MAP);
		Set<Incoming.IncomingFile> codes = incoming.files(Incoming.FileType.CODE);
		Set<Incoming.IncomingFile> ints = incoming.files(Incoming.FileType.INT);

		// single map
		if (maps.size() <= 1) return false;

		// for now, we assume a map pack should only contain maps and textures and other media, not code
		// presence of code package probably indicates a mod, rather than map pack
		// int files likely also indicate some other sort of more specialised content
		if (!codes.isEmpty() || !ints.isEmpty()) return false;

		// for now, we specifically disallow UT3, since we don't understand its package structure (> version 500?)
		for (Incoming.IncomingFile map : maps) {
			if (Util.extension(map.fileName()).equalsIgnoreCase("ut3")) return false;
		}

		// a bit naive, if there's a one-map mod, it would pass here
		return true;
	}
}
