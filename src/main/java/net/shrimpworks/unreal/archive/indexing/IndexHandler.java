package net.shrimpworks.unreal.archive.indexing;

import java.util.Collections;
import java.util.function.Consumer;

import net.shrimpworks.unreal.archive.content.addons.Addon;

public interface IndexHandler<T extends Addon> {

	public interface IndexHandlerFactory<T extends Addon> {

		public IndexHandler<T> get();
	}

	public void index(Incoming incoming, Addon current, Consumer<IndexResult<T>> completed);

	public static class NoOpIndexHandlerFactory implements IndexHandlerFactory<Addon> {

		@Override
		public IndexHandler<Addon> get() {
			return (incoming, current, completed) -> completed.accept(new IndexResult<>(current, Collections.emptySet()));
		}
	}
}
