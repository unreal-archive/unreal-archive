package net.shrimpworks.unreal.archive.mirror;

import net.shrimpworks.unreal.archive.ContentEntity;

@FunctionalInterface
public interface Progress {

	public void progress(long total, long remaining, ContentEntity<?> last);
}
