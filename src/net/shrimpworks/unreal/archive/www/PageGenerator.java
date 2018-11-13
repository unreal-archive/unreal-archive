package net.shrimpworks.unreal.archive.www;

import java.nio.file.Path;

import net.shrimpworks.unreal.archive.indexer.ContentManager;

public abstract class PageGenerator {

	final ContentManager content;
	final Path root;
	final Path staticRoot;

	/**
	 * Create a new Page Generator instance.
	 *
	 * @param content    content manager
	 * @param output     path to write this generator's output to
	 * @param staticRoot path to static content
	 */
	public PageGenerator(ContentManager content, Path output, Path staticRoot) {
		this.content = content;
		this.root = output;
		this.staticRoot = staticRoot;
	}

	/**
	 * Generate one or more HTML pages of output.
	 *
	 * @return number of individual pages created
	 */
	public abstract int generate();
}
