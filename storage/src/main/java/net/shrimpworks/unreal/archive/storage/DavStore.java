package net.shrimpworks.unreal.archive.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.shrimpworks.unreal.archive.common.CLI;
import net.shrimpworks.unreal.archive.common.Util;

/**
 * A simple HTTP storage solution, useful for testing and validation.
 * <p>
 * Possibly useful as an actual storage option with appropriate
 * configuration.
 */
public class DavStore implements DataStore {

	public static class Factory implements DataStoreFactory {

		@Override
		public DataStore newStore(StoreContent type, CLI cli) {
			String url = cli.option("dav-" + type.name().toLowerCase(), System.getenv("DAV_" + type.name()));
			if (url == null || url.isEmpty()) url = cli.option("dav-url", System.getenv("DAV_URL"));

			if (url == null || url.isEmpty()) throw new IllegalArgumentException("Missing base URL for DAV store; --dav-url or DAV_URL");

			return new DavStore(url);
		}
	}

	private final String baseUrl;

	private DavStore(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	@Override
	public void close() {
		// no-op
	}

	@Override
	public void store(Path path, String name, BiConsumer<String, IOException> stored) throws IOException {
		String url = Util.toUriString(baseUrl + name);
		Util.uploadTo(path, url);

		stored.accept(url, null);
	}

	@Override
	public void store(InputStream stream, long dataSize, String name, BiConsumer<String, IOException> stored) {
		throw new UnsupportedOperationException("Uploading streams not supported yet");
	}

	@Override
	public void delete(String url, Consumer<Boolean> deleted) throws IOException {
		deleted.accept(Util.deleteRemote(Util.toUriString(url)));
	}

	@Override
	public void download(String url, Consumer<Path> downloaded) throws IOException {
		Path tempFile = Files.createTempFile("dl_", url.substring(0, url.lastIndexOf("/") + 1));
		Util.downloadTo(Util.toUriString(url), tempFile);

		downloaded.accept(tempFile);
	}

	@Override
	public void exists(String name, Consumer<Object> result) {
		result.accept(false);
	}

	@Override
	public String toString() {
		return String.format("DavStore [baseUrl=%s]", baseUrl);
	}
}
