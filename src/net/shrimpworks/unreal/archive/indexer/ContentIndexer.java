package net.shrimpworks.unreal.archive.indexer;

import java.util.Collections;
import java.util.function.Consumer;

public interface ContentIndexer<T extends Content> {

	public interface IndexerFactory<T extends Content> {

		public ContentIndexer<T> get();
	}

	public void index(Incoming incoming, Content current, IndexLog log, Consumer<IndexResult<T>> completed);

	public static class NoOpIndexerFactory implements IndexerFactory<Content> {

		@Override
		public ContentIndexer<Content> get() {
			return (incoming, current, log, completed) -> completed.accept(new IndexResult<>(current, Collections.emptySet()));
		}
	}
}
