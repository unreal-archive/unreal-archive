package net.shrimpworks.unreal.archive.indexer;

public interface Classifier {

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
