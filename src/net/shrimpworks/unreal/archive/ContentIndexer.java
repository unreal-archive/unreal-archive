package net.shrimpworks.unreal.archive;

import java.util.function.Consumer;

public interface ContentIndexer<T extends Content> {

	public interface IndexerFactory<T extends Content> {

		public ContentIndexer<T> get();
	}

	public void index(Incoming incoming, Consumer<T> completed);

	public static class NoOpIndexerFactory implements IndexerFactory<Content> {

		@Override
		public ContentIndexer<Content> get() {
			return (incoming, completed) -> completed.accept(null);
		}
	}
}
