package net.shrimpworks.unreal.archive;

import java.beans.ConstructorProperties;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Entry-point for new content.
 * <p>
 * Defines the bare bones of a piece of content, where it currently lives, and
 * a local path to the downloaded content.
 * <p>
 * Source URLs are optional, if content has been sourced from disk only, or a
 * non-public source (someone's FTP server, HTTP upload, etc).
 */
public class ContentSubmission {

	public final Path filePath;
	public final String[] sourceUrls;

	@ConstructorProperties({ "filePath", "sourceUrls" })
	public ContentSubmission(Path filePath, String... sourceUrls) {
		this.filePath = filePath;
		this.sourceUrls = sourceUrls;
	}

	@Override
	public String toString() {
		return String.format("ContentSubmission [filePath=%s, sourceUrls=%s]", filePath, Arrays.toString(sourceUrls));
	}
}
