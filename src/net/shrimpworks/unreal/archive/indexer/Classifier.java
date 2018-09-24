package net.shrimpworks.unreal.archive.indexer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public interface Classifier {

	static final Set<String> KNOWN_FILES = new HashSet<>(Arrays.asList(
			"uxx", "unr", "umx", "usa", "uax", "u", "utx", "ut2", "ukx", "usx", "upx", "ogg", "umod"
	));

	public boolean classify(Incoming incoming);

	static class NoOpClassifier implements Classifier {

		@Override
		public boolean classify(Incoming incoming) {
			return false;
		}
	}

	public static ContentType classify(Incoming incoming, IndexLog log) {
		for (ContentType type : ContentType.values()) {
			if (type.classifier.classify(incoming)) {
				return type;
			}
		}

		log.log(IndexLog.EntryType.FATAL, "Unable to classify content in " + incoming.submission.filePath);

		return ContentType.UNKNOWN;
	}
}
