package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

public interface DataStore {

	public void store(Path path, String name, Consumer<String> stored) throws IOException;

	public void delete(String url, Consumer<Boolean> deleted) throws IOException;

	public void download(String url, Consumer<Path> downloaded) throws IOException;
}
