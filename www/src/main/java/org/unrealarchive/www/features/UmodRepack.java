package org.unrealarchive.www.features;

import java.nio.file.Path;
import java.util.Set;

import org.unrealarchive.www.PageGenerator;
import org.unrealarchive.www.SiteFeatures;
import org.unrealarchive.www.SiteMap;
import org.unrealarchive.www.Templates;

public class UmodRepack implements PageGenerator {

	private final Path root;
	private final Path staticRoot;
	private final SiteFeatures features;

	public UmodRepack(Path root, Path staticRoot, SiteFeatures features) {
		this.root = root;
		this.staticRoot = staticRoot;
		this.features = features;
	}

	@Override
	public Set<SiteMap.Page> generate() {
		Templates.PageSet pages = new Templates.PageSet("umod", features, root, staticRoot);
		pages.add("index.ftl", SiteMap.Page.of(1f, SiteMap.ChangeFrequency.weekly), "UMOD Repack")
			 .write(root.resolve("umod").resolve("index.html"));

		return pages.pages;
	}
}
