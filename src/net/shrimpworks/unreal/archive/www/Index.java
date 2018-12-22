package net.shrimpworks.unreal.archive.www;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.shrimpworks.unreal.archive.docs.DocumentManager;
import net.shrimpworks.unreal.archive.indexer.ContentManager;
import net.shrimpworks.unreal.archive.managed.ManagedContentManager;

public class Index extends ContentPageGenerator {

	private final DocumentManager documents;
	private final ManagedContentManager updates;

	public Index(
			ContentManager content, DocumentManager documents, ManagedContentManager updates,
			Path output, Path staticRoot, boolean localImages) {
		super(content, output, staticRoot, localImages);
		this.documents = documents;
		this.updates = updates;
	}

	@Override
	public int generate() {
		Map<String, Long> contentCount = new HashMap<>();
		content.countByType().forEach((k, v) -> {
			contentCount.put(k.getSimpleName(), v);
		});
		contentCount.put("Documents", documents.all().stream().filter(d -> d.published).count());
		contentCount.put("Updates", updates.all().stream().filter(d -> d.published).count());

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
