package net.shrimpworks.unreal.archive.maps;

import java.util.function.Consumer;

import net.shrimpworks.unreal.archive.ContentIndexer;
import net.shrimpworks.unreal.archive.Incoming;

public class MapIndexer implements ContentIndexer<Map> {

	public static class MapIndexerFactory implements IndexerFactory<Map> {

		@Override
		public ContentIndexer<Map> get() {
			return new MapIndexer();
		}
	}

	@Override
	public void index(Incoming incoming, Consumer<Map> completed) {
		// FIXME pass in existing
		Map m = new Map();

		m.packageSHA1 = incoming.originalSha1;

		completed.accept(m);
	}
}
