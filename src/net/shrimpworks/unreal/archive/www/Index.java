package net.shrimpworks.unreal.archive.www;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.shrimpworks.unreal.archive.indexer.ContentManager;

public class Index extends PageGenerator {

	public Index(ContentManager content, Path output, Path staticRoot, boolean localImages) {
		super(content, output, staticRoot, localImages);
	}

	@Override
	public int generate() {
		Map<String, Long> contentCount = new HashMap<>();
		content.countByType().forEach((k, v) -> {
			contentCount.put(k.getSimpleName(), v);
		});

		try {
			Templates.template("index.ftl")
					 .put("static", root.relativize(staticRoot))
					 .put("title", "Unreal Archive")
					 .put("count", contentCount)
					 .put("siteRoot", root)
					 .write(root.resolve("index.html"));
		} catch (IOException e) {
			throw new RuntimeException("Failed to render page", e);
		}

		return 1;
	}
}
