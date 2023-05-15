package org.unrealarchive.www.content;

import java.nio.file.Path;

import org.unrealarchive.content.addons.SimpleAddonRepository;
import org.unrealarchive.www.PageGenerator;
import org.unrealarchive.www.SiteFeatures;
import org.unrealarchive.www.Templates;

public abstract class ContentPageGenerator implements PageGenerator {

	final SimpleAddonRepository content;
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
	public ContentPageGenerator(SimpleAddonRepository content, Path siteRoot, Path output, Path staticRoot, SiteFeatures features) {
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
