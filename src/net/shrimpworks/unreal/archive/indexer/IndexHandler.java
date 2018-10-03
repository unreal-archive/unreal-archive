package net.shrimpworks.unreal.archive.indexer;

import java.util.Collections;
import java.util.function.Consumer;

public interface IndexHandler<T extends Content> {

	static final String UNKNOWN = "Unknown";

	static final String RELEASE_UT99 = "1999-11";

	public interface IndexHandlerFactory<T extends Content> {

		public IndexHandler<T> get();
	}

	public void index(Incoming incoming, Content current, Consumer<IndexResult<T>> completed);

	public static class NoOpIndexHandlerFactory implements IndexHandlerFactory<Content> {

		@Override
		public IndexHandler<Content> get() {
			return (incoming, current, completed) -> completed.accept(new IndexResult<>(current, Collections.emptySet()));
		}
	}
}
