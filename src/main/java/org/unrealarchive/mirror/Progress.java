package org.unrealarchive.mirror;

import org.unrealarchive.content.ContentEntity;

@FunctionalInterface
public interface Progress {

	public void progress(long total, long remaining, ContentEntity<?> last);
}
