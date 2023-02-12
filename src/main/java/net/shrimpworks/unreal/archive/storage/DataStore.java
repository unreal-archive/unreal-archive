package net.shrimpworks.unreal.archive.storage;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.shrimpworks.unreal.archive.common.CLI;

public interface DataStore extends Closeable {

	enum StoreType {
		DAV(new DavStore.Factory()),
		B2(new B2Store.Factory()),
		S3(new S3Store.Factory()),
		AZ(new AzStore.Factory()),
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

	/**
	 * Store the file at <code>path</code> under the provided name in the
	 * store.
	 *
	 * @param path   local file to store
	 * @param name   name and path of the stored file
	 * @param stored callback for completion, containing the full URL to the stored file
	 * @throws IOException storage failure
	 */
	public void store(Path path, String name, BiConsumer<String, IOException> stored) throws IOException;

	public void store(InputStream stream, long dataSize, String name, BiConsumer<String, IOException> stored) throws IOException;

	/**
	 * Remove the file at <code>url</code> from storage.
	 *
	 * @param url     url of file to delete
	 * @param deleted callback for completion, true if successful
	 * @throws IOException deletion failure
	 */
	public void delete(String url, Consumer<Boolean> deleted) throws IOException;

	/**
	 * Retrieve the file from the remote URL and write it to a local
	 * temporary file.
	 *
	 * @param url        file to download
	 * @param downloaded callback for completion, containing path to downloaded file
	 * @throws IOException download failure
	 */
	public void download(String url, Consumer<Path> downloaded) throws IOException;

	/**
	 * Check if the given file exists in this store.
	 *
	 * @param name   file to check
	 * @param result callback for completion, contains implementation-specific file information
	 * @throws IOException check failure
	 */
	public void exists(String name, Consumer<Object> result) throws IOException;

	static class NopStore implements DataStore {

		static class NopStoreFactory implements DataStoreFactory {

			@Override
			public DataStore newStore(StoreContent type, CLI cli) {
				return new NopStore();
			}
		}

		@Override
		public void store(Path path, String name, BiConsumer<String, IOException> stored) {
			stored.accept("nop://" + name, null);
		}

		@Override
		public void store(InputStream stream, long dataSize, String name, BiConsumer<String, IOException> stored) {
			stored.accept("nop://" + name, null);
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
		public void exists(String name, Consumer<Object> result) {
			result.accept(false);
		}

		@Override
		public void close() {

		}

		@Override
		public String toString() {
			return "NopStore";
		}
	}
}
