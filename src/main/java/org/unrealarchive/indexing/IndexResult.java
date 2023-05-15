package org.unrealarchive.indexing;

import java.nio.file.Path;
import java.util.Set;

import org.unrealarchive.content.addons.Addon;

public class IndexResult<T extends Addon> {

	public final T content;
	public final Set<NewAttachment> files;

	public IndexResult(T content, Set<NewAttachment> attachments) {
		this.content = content;
		this.files = attachments;
	}

	public static class NewAttachment {

		public final Addon.AttachmentType type;
		public final String name;
		public final Path path;

		public NewAttachment(Addon.AttachmentType type, String name, Path path) {
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
