package net.shrimpworks.unreal.archive.www;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.shrimpworks.unreal.archive.indexer.ContentManager;
import net.shrimpworks.unreal.archive.indexer.ContentType;

public class Index {

	private final ContentManager content;
	private final Path root;
	private final Path staticRoot;

	public Index(ContentManager content, Path output, Path staticRoot) {
		this.content = content;
		this.root = output;
		this.staticRoot = staticRoot;
	}

	public int generate() {
		Map<String, Long> contentCount = new HashMap<>();
		content.countByType().forEach((k, v) -> {
			contentCount.put(k.getSimpleName(), v);
		});

		try {
			Templates.template("index.ftl")
					 .put("static", staticRoot)
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
