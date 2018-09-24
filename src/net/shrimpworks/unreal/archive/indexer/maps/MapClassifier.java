package net.shrimpworks.unreal.archive.indexer.maps;

import net.shrimpworks.unreal.archive.indexer.Classifier;
import net.shrimpworks.unreal.archive.indexer.Incoming;

public class MapClassifier implements Classifier {

	@Override
	public boolean classify(Incoming incoming) {
		// a bit naive, if there's a one-map mod, it would be caught here
		return incoming.files(Incoming.FileType.MAP).size() == 1;
	}

}
