package net.shrimpworks.unreal.archive.mirror;

import net.shrimpworks.unreal.archive.content.ContentEntity;

@FunctionalInterface
public interface Progress {

	public void progress(long total, long remaining, ContentEntity<?> last);
}
