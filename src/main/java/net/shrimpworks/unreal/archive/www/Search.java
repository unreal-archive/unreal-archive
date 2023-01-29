package net.shrimpworks.unreal.archive.www;

import java.nio.file.Path;
import java.util.Set;

import net.shrimpworks.unreal.archive.www.PageGenerator;
import net.shrimpworks.unreal.archive.www.SiteFeatures;
import net.shrimpworks.unreal.archive.www.SiteMap;
import net.shrimpworks.unreal.archive.www.Templates;

public class Search implements PageGenerator {

	private final Path root;
	private final Path siteRoot;
	private final Path staticRoot;
	private final SiteFeatures features;

	public Search(Path output, Path staticRoot, SiteFeatures features) {
		this.root = output.resolve("search");
		this.siteRoot = output;
		this.staticRoot = staticRoot;
		this.features = features;
	}

	@Override
	public Set<SiteMap.Page> generate() {
		Templates.PageSet pages = new Templates.PageSet("search", features, siteRoot, staticRoot, root);
		pages.add("index.ftl", SiteMap.Page.of(0f, SiteMap.ChangeFrequency.never), "Search")
			 .write(root.resolve("index.html"));

		return pages.pages;
	}
}
