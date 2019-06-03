package net.shrimpworks.unreal.archive.www;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public class Submit implements PageGenerator {

	private final Path root;
	private final Path siteRoot;
	private final Path staticRoot;

	public Submit(Path output, Path staticRoot) {
		this.root = output.resolve("submit");
		this.siteRoot = output;
		this.staticRoot = staticRoot;
	}

	@Override
	public Set<SiteMap.Page> generate() {
		Templates.PageSet pages = new Templates.PageSet("submit", siteRoot, staticRoot, root);
		try {
			pages.add("index.ftl", SiteMap.Page.of(1f, SiteMap.ChangeFrequency.weekly), "Submit Content")
				 .write(root.resolve("index.html"));
		} catch (IOException e) {
			throw new RuntimeException("Failed to render page", e);
		}

		return pages.pages;
	}
}
