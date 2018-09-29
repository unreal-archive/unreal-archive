package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import org.apache.hc.core5.http.ContentType;

/**
 * A simple HTTP storage solution, useful for testing and validation.
 * <p>
 * Possibly useful as an actual storage option with appropriate
 * configuration.
 */
public class SimpleHttpStore implements DataStore {

	private final String baseUrl;

	public SimpleHttpStore(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	@Override
	public void store(Path path, String name, Consumer<String> stored) throws IOException {
		Response execute = Request.Put(baseUrl + name)
								  .bodyFile(path.toFile(), ContentType.DEFAULT_BINARY)
								  .execute();
		if (execute.returnResponse().getCode() < 400) {
			stored.accept(baseUrl + name);
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
}
