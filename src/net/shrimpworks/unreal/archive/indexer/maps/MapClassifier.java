package net.shrimpworks.unreal.archive.indexer.maps;

import net.shrimpworks.unreal.archive.indexer.ContentClassifier;
import net.shrimpworks.unreal.archive.indexer.Incoming;

import static java.util.Arrays.stream;

public class MapClassifier implements ContentClassifier.Classifier {

	private static final String[] MAP_EXTENSIONS = new String[] { ".unr", ".ut2" };

	@Override
	public boolean classify(Incoming incoming) {
		final String fileName = incoming.submission.filePath.toString();

		// no need to look further if it's just a map
		boolean isMap = stream(MAP_EXTENSIONS).anyMatch(e -> fileName.toLowerCase().endsWith(e));
		if (isMap) return true;

		// count all map files in the archive
		long count = incoming.files.keySet().stream()
								   .filter(f -> stream(MAP_EXTENSIONS).anyMatch(e -> f.toLowerCase().endsWith(e)))
								   .count();

		// a bit naive, if there's a one-map mod, it would be caught here
		return count == 1;
	}

}
