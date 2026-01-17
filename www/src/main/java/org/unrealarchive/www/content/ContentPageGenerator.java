package org.unrealarchive.www.content;

import java.nio.file.Path;

import org.unrealarchive.content.RepositoryManager;
import org.unrealarchive.www.PageGenerator;
import org.unrealarchive.www.SiteFeatures;
import org.unrealarchive.www.Templates;

public abstract class ContentPageGenerator implements PageGenerator, AttachmentHelper {

	final RepositoryManager repos;
	final Path root;
	final Path staticRoot;

	final SiteFeatures features;

	/**
	 * Create a new Page Generator instance.
	 *
	 * @param repos      repository manager
	 * @param root       root directory of the website output
	 * @param staticRoot path to static content
	 * @param features   if true, download and reference local copies of remote images
	 */
	public ContentPageGenerator(RepositoryManager repos, Path root, Path staticRoot, SiteFeatures features) {
		this.repos = repos;
		this.root = root;
		this.staticRoot = staticRoot;
		this.features = features;
	}

	Templates.PageSet pageSet(String resourceRoot) {
		return new Templates.PageSet(resourceRoot, features, root, staticRoot);
	}

	@Override
	public SiteFeatures features() {
		return features;
	}

}
