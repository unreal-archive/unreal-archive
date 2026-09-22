package org.unrealarchive.content;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import org.unrealarchive.content.addons.HasAuthors;

public interface ContentEntity<T extends ContentEntity<T>> extends Comparable<ContentEntity<?>>, HasAuthors {

	public ContentId id();

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

	/**
	 * Indicates that this content is a "repack" of an original content package.
	 * It may contain slightly different or corrected files, but is intended to remain
	 * true to the original package/release.
	 */
	public boolean repack();

	/**
	 * If true, will not show up in www output, and will be ignored in index passes.
	 */
	public boolean deleted();

	public boolean isVariation();

	public default String shortGame() {
		return Games.byName(game()).shortName;
	}

	@Override
	default int compareTo(ContentEntity<?> o) {
		return name().compareToIgnoreCase(o.name());
	}

	record ContentId(String type, String id) {

		public static ContentId of(String id) {
			return new ContentId(id.substring(0, id.indexOf(':')), id.substring(id.indexOf(':') + 1));
		}

		@Override
		public String toString() {
			return String.format("%s:%s", type, id);
		}
	}
}
