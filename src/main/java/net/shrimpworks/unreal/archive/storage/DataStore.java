package net.shrimpworks.unreal.archive.storage;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

import net.shrimpworks.unreal.archive.CLI;

public interface DataStore extends Closeable {

	enum StoreType {
		DAV(new DavStore.Factory()),
		B2(new B2Store.Factory()),
		NOP(new NopStore.NopStoreFactory()),
		;

		private final DataStoreFactory factory;

		StoreType(DataStoreFactory factory) {
			this.factory = factory;
		}

		public DataStore newStore(StoreContent contentType, CLI cli) {
			return factory.newStore(contentType, cli);
		}
	}

	enum StoreContent {
		IMAGES,
		ATTACHMENTS,
		CONTENT,
		;
	}

	public static final DataStore NOP = new NopStore();

	public interface DataStoreFactory {

		public DataStore newStore(StoreContent type, CLI cli);
	}

	public void store(Path path, String name, Consumer<String> stored) throws IOException;

	public void delete(String url, Consumer<Boolean> deleted) throws IOException;

	public void download(String url, Consumer<Path> downloaded) throws IOException;

	static class NopStore implements DataStore {

		static class NopStoreFactory implements DataStoreFactory {

			@Override
			public DataStore newStore(StoreContent type, CLI cli) {
				return new NopStore();
			}
		}

		@Override
		public void store(Path path, String name, Consumer<String> stored) {
			stored.accept("nop://" + name);
		}

		@Override
		public void delete(String url, Consumer<Boolean> deleted) {
			deleted.accept(true);
		}

		@Override
		public void download(String url, Consumer<Path> downloaded) {
			downloaded.accept(null);
		}

		@Override
		public void close() {

		}
	}
}
