package org.unrealarchive.www;

import java.nio.file.Path;
import java.util.Set;

public class UmodRepack implements PageGenerator {

	private final Path root;
	private final Path siteRoot;
	private final Path staticRoot;
	private final SiteFeatures features;

	public UmodRepack(Path output, Path staticRoot, SiteFeatures features) {
		this.root = output.resolve("umod");
		this.siteRoot = output;
		this.staticRoot = staticRoot;
		this.features = features;
	}

	@Override
	public Set<SiteMap.Page> generate() {
		Templates.PageSet pages = new Templates.PageSet("umod", features, siteRoot, staticRoot, root);
		pages.add("index.ftl", SiteMap.Page.of(1f, SiteMap.ChangeFrequency.weekly), "UMOD Repack")
			 .write(root.resolve("index.html"));

		return pages.pages;
	}
}
