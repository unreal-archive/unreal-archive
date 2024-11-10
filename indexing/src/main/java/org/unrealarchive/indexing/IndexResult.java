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

	public record NewAttachment(Addon.AttachmentType type, String name, Path path) {
		@Override
		public String toString() {
			return String.format("NewAttachment [type=%s, name=%s, path=%s]", type, name, path);
		}
	}
}
