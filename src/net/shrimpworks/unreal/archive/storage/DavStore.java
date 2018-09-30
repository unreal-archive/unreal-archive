package net.shrimpworks.unreal.archive.storage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import org.apache.hc.core5.http.ContentType;

import net.shrimpworks.unreal.archive.CLI;

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
			return new DavStore(url);
		}
	}

	private final String baseUrl;

	private DavStore(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	@Override
	public void store(Path path, String name, Consumer<String> stored) throws IOException {
		URI uri = toUri(baseUrl + name);
		Response execute = Request.Put(uri)
								  .bodyFile(path.toFile(), ContentType.DEFAULT_BINARY)
								  .execute();
		if (execute.returnResponse().getCode() < 400) {
			stored.accept(uri.toString());
		}
	}

	@Override
	public void delete(String url, Consumer<Boolean> deleted) throws IOException {
		Response execute = Request.Delete(url).execute();
		deleted.accept(execute.returnResponse().getCode() < 400);
	}

	@Override
	public void download(String url, Consumer<Path> downloaded) throws IOException {
		Response execute = Request.Get(url).execute();

		Path tempFile = Files.createTempFile("dl_", url.substring(0, url.lastIndexOf("/") + 1));

		execute.saveContent(tempFile.toFile());

		downloaded.accept(tempFile);
	}

	private URI toUri(String s) throws IOException {
		try {
			URL url = new URL(s);
			return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
		} catch (URISyntaxException | MalformedURLException e) {
			throw new IOException("Invalid URL: " + s, e);
		}
	}
}
