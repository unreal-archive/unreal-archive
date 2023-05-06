package net.shrimpworks.unreal.archive.indexing;

public interface Classifier {

	public boolean classify(Incoming incoming);

	static class NoOpClassifier implements Classifier {

		@Override
		public boolean classify(Incoming incoming) {
			return false;
		}
	}

}
