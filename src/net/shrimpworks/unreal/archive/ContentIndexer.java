package net.shrimpworks.unreal.archive;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public interface ContentIndexer<T extends Content> {

	static Set<String> KNOWN_FILES = new HashSet<>(Arrays.asList(
			"uxx", "unr", "umx", "usa", "uax", "u", "utx", "ut2", "ukx", "usx", "upx", "ogg", "umod"
	));

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
