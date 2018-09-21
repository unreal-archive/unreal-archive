package net.shrimpworks.unreal.archive.indexer.skins;

import java.util.function.Consumer;

import net.shrimpworks.unreal.archive.indexer.Content;
import net.shrimpworks.unreal.archive.indexer.ContentIndexer;
import net.shrimpworks.unreal.archive.indexer.Incoming;
import net.shrimpworks.unreal.archive.indexer.IndexLog;
import net.shrimpworks.unreal.archive.indexer.IndexResult;

public class SkinIndexer implements ContentIndexer<Skin> {

	public static class SkinIndexerFactory implements IndexerFactory<Skin> {

		@Override
		public ContentIndexer<Skin> get() {
			return new SkinIndexer();
		}
	}

	@Override
	public void index(Incoming incoming, Content current, IndexLog log, Consumer<IndexResult<Skin>> completed) {
		Skin s = (Skin)current;

		// TODO implement the things
	}
}
