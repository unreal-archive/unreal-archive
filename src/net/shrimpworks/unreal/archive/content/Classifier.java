package net.shrimpworks.unreal.archive.content;

public interface Classifier {

	public boolean classify(Incoming incoming);

	static class NoOpClassifier implements Classifier {

		@Override
		public boolean classify(Incoming incoming) {
			return false;
		}
	}

}
