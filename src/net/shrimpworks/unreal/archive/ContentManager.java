package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ContentManager {

	private final Map<String, Content> content;

	private final Set<String> changes;

	public ContentManager(Path inputPath) {
		this.content = new HashMap<>();

		this.changes = new HashSet<>();

		// TODO load contents from inputPAth into content
	}

	// intent: when some content is going to be worked on, a clone is checked out.
	// when its checked out, its hash (immutable) is stored in the out collection.
	// after its been modified or left alone, the clone is checked in.
	// during check-in, if the the clone is no longer equal to the original, something changed.
	// if something changed, the content will be written out, within a new directory structure if needed
	// and the old file will be removed

	@SuppressWarnings("unchecked")
	public <T extends Content> T checkout(String hash, Class<T> type) {
		T content = (T)this.content.get(hash);
		if (content != null) {
			try {
				return YAML.fromString(YAML.toString(content), type);
			} catch (IOException e) {
				throw new IllegalStateException("Cannot clone content " + content);
			}
		}
		return null;
	}

	public boolean checkin(Content content) {
		this.content.get(content.hash);
		return false;
	}
}
