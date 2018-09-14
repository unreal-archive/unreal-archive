package net.shrimpworks.unreal.archive;

import java.util.function.Consumer;

public interface ContentIndexer<T extends Content> {

	public interface IndexerFactory<T extends Content> {

		public ContentIndexer<T> get();
	}

	public void index(ContentSubmission submission, Consumer<T> completed);

	public static class NoOpIndexerFactory implements IndexerFactory<Content> {

		@Override
		public ContentIndexer<Content> get() {
			return (submission, completed) -> completed.accept(null);
		}
	}
}
