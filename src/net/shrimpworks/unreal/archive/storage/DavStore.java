package net.shrimpworks.unreal.archive.storage;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;

import net.shrimpworks.unreal.archive.CLI;
import net.shrimpworks.unreal.archive.Util;

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
	public void store(Path path, String name, Consumer<String> stored) throws IOException {
		URI uri = Util.toUri(baseUrl + name);
		Response execute = Request.Put(uri)
								  .bodyFile(path.toFile(), ContentType.DEFAULT_BINARY)
								  .execute();
		if (execute.returnResponse().getStatusLine().getStatusCode() < 400) {
			stored.accept(uri.toString());
		}
	}

	@Override
	public void delete(String url, Consumer<Boolean> deleted) throws IOException {
		Response execute = Request.Delete(url).execute();
		deleted.accept(execute.returnResponse().getStatusLine().getStatusCode() < 400);
	}

	@Override
	public void download(String url, Consumer<Path> downloaded) throws IOException {
		Response execute = Request.Get(url).execute();

		Path tempFile = Files.createTempFile("dl_", url.substring(0, url.lastIndexOf("/") + 1));

		execute.saveContent(tempFile.toFile());

		downloaded.accept(tempFile);
	}

}
