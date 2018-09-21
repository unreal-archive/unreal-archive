package net.shrimpworks.unreal.archive.indexer;

import java.nio.file.Path;
import java.util.Set;

public class IndexResult<T extends Content> {

	public final T content;
	public final Set<CreatedFile> files;

	public IndexResult(T content, Set<CreatedFile> files) {
		this.content = content;
		this.files = files;
	}

	public static class CreatedFile {

		public final String name;
		public final Path path;

		public CreatedFile(String name, Path path) {
			this.name = name;
			this.path = path;
		}
	}
}
