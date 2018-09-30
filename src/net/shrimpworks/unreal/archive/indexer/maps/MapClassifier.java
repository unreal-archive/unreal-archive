package net.shrimpworks.unreal.archive.indexer.maps;

import java.util.Set;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.indexer.Classifier;
import net.shrimpworks.unreal.archive.indexer.Incoming;

public class MapClassifier implements Classifier {

	@Override
	public boolean classify(Incoming incoming) {
		Set<Incoming.IncomingFile> maps = incoming.files(Incoming.FileType.MAP);

		if (maps.size() > 1) return false;

		// for now, we specifically disallow UT3, since we don't understand its package structure (> version 500?)
		for (Incoming.IncomingFile map : maps) {
			if (Util.extension(map.fileName()).equalsIgnoreCase("ut3")) return false;
		}

		// a bit naive, if there's a one-map mod, it would pass here
		return maps.size() == 1;
	}

}
