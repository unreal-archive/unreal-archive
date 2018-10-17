package net.shrimpworks.unreal.archive.indexer.maps;

import java.util.Set;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.indexer.Classifier;
import net.shrimpworks.unreal.archive.indexer.Incoming;

public class MapClassifier implements Classifier {

	@Override
	public boolean classify(Incoming incoming) {
		Set<Incoming.IncomingFile> maps = incoming.files(Incoming.FileType.MAP);

		if (maps.isEmpty()) return false;

		// for now, we specifically disallow UT3, since we don't understand its package structure (> version 500?)
		for (Incoming.IncomingFile map : maps) {
			if (Util.extension(map.fileName()).equalsIgnoreCase("ut3")) return false;
		}

		// a bit naive, if there's a one-map mod, it would pass here
		if (maps.size() == 1) return true;

		// support for maps with variations; often "DM-MyLevel" with an associated "DM-MyLevel(Variation)" or something
		// these get incorrectly identified as map packs, so when we find a collection of maps all starting with the same
		// name, try to classify them as a single map.

		String baseName = "";
		for (Incoming.IncomingFile map : maps) {
			String tmp = Util.plainName(map.fileName()).toUpperCase().replaceAll("[^A-Z0-9]", "");
			if (baseName.isEmpty() || tmp.length() < baseName.length()) {
				baseName = tmp;
			}
		}

		int variations = 0;
		for (Incoming.IncomingFile map : maps) {
			String tmp = Util.plainName(map.fileName()).toUpperCase().replaceAll("[^A-Z0-9]", "");
			if (tmp.startsWith(baseName)) variations++;
		}

		return variations == maps.size();
	}

}
