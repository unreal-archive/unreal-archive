package net.shrimpworks.unreal.archive.indexing;

import java.beans.ConstructorProperties;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Entry-point for new content.
 * <p>
 * Defines the bare bones of a piece of content, where it currently lives, and
 * a local path to the downloaded content.
 * <p>
 * Source URLs are optional, if content has been sourced from disk only, or a
 * non-public source (someone's FTP server, HTTP upload, etc).
 */
public class Submission implements Comparable<Submission> {

	public Path filePath;
	public String[] sourceUrls;

	public SubmissionOverride override;

	@ConstructorProperties({ "filePath", "sourceUrls" })
	public Submission(Path filePath, String... sourceUrls) {
		this.filePath = filePath;
		this.sourceUrls = sourceUrls;
		this.override = new SubmissionOverride(new HashMap<>());
	}

	@Override
	public String toString() {
		return String.format("ContentSubmission [filePath=%s, sourceUrls=%s]", filePath, Arrays.toString(sourceUrls));
	}

	@Override
	public int compareTo(Submission o) {
		return filePath.compareTo(o.filePath);
	}
}
