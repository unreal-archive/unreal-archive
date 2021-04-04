package net.shrimpworks.unreal.archive;

import java.nio.file.Path;
import java.time.LocalDateTime;

public interface ContentEntity<T extends ContentEntity<T>> extends Comparable<ContentEntity<?>> {

	public Path contentPath(Path root);

	public Path slugPath(Path root);

	public Path pagePath(Path root);

	public String game();

	public String name();

	public String author();
	public String authorName();

	public String releaseDate();

	public String autoDescription();

	public LocalDateTime addedDate();

	public String contentType();

	public String friendlyType();

	public String leadImage();

	public boolean deleted();

	public boolean isVariation();

	@Override
	default int compareTo(ContentEntity<?> o) {
		return name().compareToIgnoreCase(o.name());
	}

}
