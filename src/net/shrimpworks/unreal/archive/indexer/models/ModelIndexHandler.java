package net.shrimpworks.unreal.archive.indexer.models;

import java.util.function.Consumer;

import net.shrimpworks.unreal.archive.indexer.Content;
import net.shrimpworks.unreal.archive.indexer.Incoming;
import net.shrimpworks.unreal.archive.indexer.IndexHandler;
import net.shrimpworks.unreal.archive.indexer.IndexResult;

public class ModelIndexHandler implements IndexHandler<Model> {

	private static final String SHOT_NAME = "%s_shot_%d.png";

	public static class ModelIndexHandlerFactory implements IndexHandlerFactory<Model> {

		@Override
		public IndexHandler<Model> get() {
			return new ModelIndexHandler();
		}
	}

	@Override
	public void index(Incoming incoming, Content current, Consumer<IndexResult<Model>> completed) {
		throw new UnsupportedOperationException("Not implemented");
	}
}
