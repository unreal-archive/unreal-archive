package net.shrimpworks.unreal.archive.www;

import java.nio.file.Path;
import java.util.Set;

public class Search implements PageGenerator {

	private final Path root;
	private final Path siteRoot;
	private final Path staticRoot;

	public Search(Path output, Path staticRoot) {
		this.root = output.resolve("search");
		this.siteRoot = output;
		this.staticRoot = staticRoot;
	}

	@Override
	public Set<SiteMap.Page> generate() {
		Templates.PageSet pages = new Templates.PageSet("search", siteRoot, staticRoot, root);
		pages.add("index.ftl", SiteMap.Page.of(0f, SiteMap.ChangeFrequency.never), "Search")
			 .write(root.resolve("index.html"));

		return pages.pages;
	}
}
