package net.shrimpworks.unreal.archive.indexer;

import java.nio.file.Path;
import java.util.Set;

public class IndexResult<T extends Content> {

	public final T content;
	public final Set<NewAttachment> files;

	public IndexResult(T content, Set<NewAttachment> attachments) {
		this.content = content;
		this.files = attachments;
	}

	public static class NewAttachment {

		public final Content.AttachmentType type;
		public final String name;
		public final Path path;

		public NewAttachment(Content.AttachmentType type, String name, Path path) {
			this.type = type;
			this.name = name;
			this.path = path;
		}
	}
}
