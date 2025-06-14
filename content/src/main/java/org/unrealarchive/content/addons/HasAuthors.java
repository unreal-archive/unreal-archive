package org.unrealarchive.content.addons;

import org.unrealarchive.content.AuthorInfo;

public interface HasAuthors {

	public default String authorName() {
		return authorInfo().authorName();
	}

	public AuthorInfo authorInfo();
}
