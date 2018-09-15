package net.shrimpworks.unreal.archive;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public interface ContentIndexer<T extends Content> {

	static Set<String> KNOWN_FILES = new HashSet<>(Arrays.asList(
			"uxx", "unr", "umx", "usa", "uax", "u", "utx", "ut2", "ukx", "usx", "upx", "ogg"
	));

	public interface IndexerFactory<T extends Content> {

		public ContentIndexer<T> get();
	}

	public void index(Incoming incoming, IndexLog log, Consumer<T> completed);

	public static class NoOpIndexerFactory implements IndexerFactory<Content> {

		@Override
		public ContentIndexer<Content> get() {
			return (incoming, log, completed) -> completed.accept(null);
		}
	}
}
