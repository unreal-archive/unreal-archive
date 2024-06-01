package org.unrealarchive.www.features;

import java.nio.file.Path;
import java.util.Set;

import org.unrealarchive.www.PageGenerator;
import org.unrealarchive.www.SiteFeatures;
import org.unrealarchive.www.SiteMap;
import org.unrealarchive.www.Templates;

public class Search implements PageGenerator {

	private final Path root;
	private final Path staticRoot;
	private final SiteFeatures features;

	public Search(Path root, Path staticRoot, SiteFeatures features) {
		this.root = root;
		this.staticRoot = staticRoot;
		this.features = features;
	}

	@Override
	public Set<SiteMap.Page> generate() {
		Templates.PageSet pages = new Templates.PageSet("search", features, root, staticRoot);
		pages.add("index.ftl", SiteMap.Page.of(0f, SiteMap.ChangeFrequency.never), "Search")
			 .write(root.resolve("search").resolve("index.html"));

		return pages.pages;
	}
}
