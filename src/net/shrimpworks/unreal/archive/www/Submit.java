package net.shrimpworks.unreal.archive.www;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class Submit implements PageGenerator {

	private final Path root;
	private final Path staticRoot;

	public Submit(Path output, Path staticRoot) {
		this.root = output.resolve("submit");
		this.staticRoot = staticRoot;
	}

	@Override
	public Set<SiteMap.Page> generate() {
		Set<SiteMap.Page> pages = new HashSet<>();
		try {
			pages.add(Templates.template("submit/index.ftl", SiteMap.Page.of(0.1f, SiteMap.ChangeFrequency.monthly))
							   .put("static", root.relativize(staticRoot))
							   .put("title", "Submit Content")
							   .put("siteRoot", root)
							   .write(root.resolve("index.html")));
		} catch (IOException e) {
			throw new RuntimeException("Failed to render page", e);
		}

		return pages;
	}
}
