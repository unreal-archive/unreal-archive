package org.unrealarchive.content;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import org.unrealarchive.content.addons.HasAuthors;

public interface ContentEntity<T extends ContentEntity<T>> extends Comparable<ContentEntity<?>>, HasAuthors {

	public String id();

	public Path contentPath(Path root);

	public Path slugPath(Path root);

	public Path pagePath(Path root);

	public String game();

	public String name();

	public String author();

	public AuthorInfo authorInfo();

	public String releaseDate();

	public String autoDescription();

	public Set<String> autoTags();

	public LocalDateTime addedDate();

	public String contentType();

	public String friendlyType();

	public String leadImage();

	/**
	 * Map of Title -> URL.
	 */
	public Map<String, String> links();

	/**
	 * Map of Title -> URL.
	 */
	public Map<String, String> problemLinks();

	public boolean deleted();

	public boolean isVariation();

	@Override
	default int compareTo(ContentEntity<?> o) {
		return name().compareToIgnoreCase(o.name());
	}

}
