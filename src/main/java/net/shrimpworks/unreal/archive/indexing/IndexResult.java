package net.shrimpworks.unreal.archive.indexing;

import java.nio.file.Path;
import java.util.Set;

import net.shrimpworks.unreal.archive.content.Content;

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

		@Override
		public String toString() {
			return String.format("NewAttachment [type=%s, name=%s, path=%s]", type, name, path);
		}
	}
}
