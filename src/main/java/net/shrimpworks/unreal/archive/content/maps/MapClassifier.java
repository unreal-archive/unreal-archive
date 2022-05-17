package net.shrimpworks.unreal.archive.content.maps;

import java.util.Set;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.content.Classifier;
import net.shrimpworks.unreal.archive.content.Incoming;

public class MapClassifier implements Classifier {

	private static final Set<String> IGNORED_FILES = Set.of("Screen.int", "CTFScreen.int", "XMaps.int");

	@Override
	public boolean classify(Incoming incoming) {
		Set<Incoming.IncomingFile> maps = incoming.files(Incoming.FileType.MAP);
		Set<Incoming.IncomingFile> ints = incoming.files(Incoming.FileType.INT);

		if (maps.isEmpty()) return false;

		// a map definitely probably won't have any associated .int files
		if (ints.stream().anyMatch(i -> !IGNORED_FILES.contains(i.fileName()))) return false;

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
