package net.shrimpworks.unreal.archive.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

import net.shrimpworks.unreal.archive.CLI;

public interface DataStore {

	enum StoreType {
		DAV(new DavStore.Factory()),
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

	public interface DataStoreFactory {

		public DataStore newStore(StoreContent type, CLI cli);
	}

	public void store(Path path, String name, Consumer<String> stored) throws IOException;

	public void delete(String url, Consumer<Boolean> deleted) throws IOException;

	public void download(String url, Consumer<Path> downloaded) throws IOException;
}
