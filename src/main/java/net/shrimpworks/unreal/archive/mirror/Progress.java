package net.shrimpworks.unreal.archive.mirror;

import net.shrimpworks.unreal.archive.content.Content;

@FunctionalInterface
public interface Progress {

	public void progress(long total, long remaining, Content last);
}
