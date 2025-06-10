package org.unrealarchive.content;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.unrealarchive.common.Util;

public class Author implements Comparable<Author> {

	private transient String slug;

	public String name;
	public Set<String> aliases = new HashSet<>();
	public Map<String, String> links = new HashMap<>();
	public String iconImage;
	public String coverImage;
	public String profileImage;
	public String bgImage;
	public String about;
	public boolean showAliases = false;
	public boolean deleted = false;

	public Author() {
	}

	public Author(String name, String... aliases) {
		this.name = name;
		this.aliases.addAll(Arrays.asList(aliases));
	}

	public Path pagePath(Path root) {
		return root.resolve("authors").resolve(slug());
	}

	@Override
	public String toString() {
		return String.format("Author [slug=%s, name=%s, aliases=%s, links=%s, iconImage=%s, profileImage=%s, bgImage=%s, about=%s, deleted=%s]",
							 slug(), name, aliases, links, iconImage, profileImage, bgImage, about, deleted);
	}

	/**
	 * Slug for the author. This is used as a unique identifier for the author.
	 */
	public String slug() {
		if (slug == null) slug = Util.authorSlug(this.name);
		return slug;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Author author)) return false;
		return Objects.equals(slug(), author.slug());
	}

	@Override
	public int hashCode() {
		return Objects.hash(slug());
	}

	@Override
	public int compareTo(Author author) {
		return Util.normalised(name).compareTo(Util.normalised(author.name));
	}
}