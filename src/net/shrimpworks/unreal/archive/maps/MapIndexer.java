package net.shrimpworks.unreal.archive.maps;

import java.util.function.Consumer;

import net.shrimpworks.unreal.archive.ContentIndexer;
import net.shrimpworks.unreal.archive.ContentSubmission;

public class MapIndexer implements ContentIndexer<Map> {

	public static class MapIndexerFactory implements IndexerFactory<Map> {

		@Override
		public ContentIndexer<Map> get() {
			return new MapIndexer();
		}
	}

	@Override
	public void index(ContentSubmission submission, Consumer<Map> completed) {

	}
}
