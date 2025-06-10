package org.unrealarchive.content;

import java.util.Objects;

public class AuthorInfo implements Comparable<AuthorInfo> {

	private final String sourceName;

	private Author author;
	private Contributors contributors;

	private volatile boolean loaded = false;

	public AuthorInfo(String sourceName) {
		this.sourceName = sourceName;
	}

	public String authorName() {
		return author().name;
	}

	public synchronized Author author() {
		if (author != null) return author;

		Contributors contribs = contributors();
		if (contribs != null && contribs.originalAuthor != null) author = contribs.originalAuthor;

		if (author == null) author = Authors.byName(sourceName);
		if (author == null) author = new Author(sourceName);

		return author;
	}

	public synchronized Contributors contributors() {
		if (loaded) return contributors;

		contributors = Authors.contributors(sourceName);
		loaded = true;

		return contributors;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof AuthorInfo that)) return false;
		return Objects.equals(author(), that.author());
	}

	@Override
	public int hashCode() {
		return author().hashCode();
	}

	@Override
	public int compareTo(AuthorInfo o) {
		return authorName().compareToIgnoreCase(o.authorName());
	}
}
