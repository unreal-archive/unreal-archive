package net.shrimpworks.unreal.archive.www.content;

import java.nio.file.Path;

import net.shrimpworks.unreal.archive.content.ContentRepository;
import net.shrimpworks.unreal.archive.www.PageGenerator;
import net.shrimpworks.unreal.archive.www.SiteFeatures;
import net.shrimpworks.unreal.archive.www.Templates;

public abstract class ContentPageGenerator implements PageGenerator {

	final ContentRepository content;
	final Path siteRoot;
	final Path root;
	final Path staticRoot;

	final SiteFeatures features;

	/**
	 * Create a new Page Generator instance.
	 *
	 * @param content    content repository
	 * @param siteRoot   root directory of the website output
	 * @param output     path to write this generator's output to
	 * @param staticRoot path to static content
	 * @param features   if true, download and reference local copies of remote images
	 */
	public ContentPageGenerator(ContentRepository content, Path siteRoot, Path output, Path staticRoot, SiteFeatures features) {
		this.content = content;
		this.siteRoot = siteRoot;
		this.root = output;
		this.staticRoot = staticRoot;
		this.features = features;
	}

	Templates.PageSet pageSet(String resourceRoot) {
		return new Templates.PageSet(resourceRoot, features, siteRoot, staticRoot, root);
	}

}
