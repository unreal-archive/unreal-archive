package net.shrimpworks.unreal.archive.www;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.shrimpworks.unreal.archive.content.ContentRepository;
import net.shrimpworks.unreal.archive.content.GameTypeRepository;
import net.shrimpworks.unreal.archive.docs.DocumentRepository;
import net.shrimpworks.unreal.archive.managed.ManagedContentRepository;

public class Index implements PageGenerator {

	private final DocumentRepository documents;
	private final ManagedContentRepository updates;
	private final ContentRepository content;
	private final GameTypeRepository gametypes;
	private final Path root;
	private final Path staticRoot;
	private final SiteFeatures features;

	public Index(ContentRepository content, GameTypeRepository gametypes, DocumentRepository documents, ManagedContentRepository updates,
				 Path output, Path staticRoot, SiteFeatures features) {
		this.content = content;
		this.gametypes = gametypes;
		this.documents = documents;
		this.updates = updates;

		this.root = output;
		this.staticRoot = staticRoot;
		this.features = features;
	}

	@Override
	public Set<SiteMap.Page> generate() {
		Map<String, Long> contentCount = new HashMap<>();
		content.countByType().forEach((k, v) -> contentCount.put(k.getSimpleName(), v));
		contentCount.put("Documents", documents.all().stream().filter(d -> d.published).count());
		contentCount.put("Updates", updates.all().stream().filter(d -> d.published).count());
		contentCount.put("GameTypes", gametypes.all().stream().filter(d -> !d.deleted).count());

		Templates.PageSet pages = new Templates.PageSet("", features, root, staticRoot, root);

		pages.add("index.ftl", SiteMap.Page.of(1f, SiteMap.ChangeFrequency.weekly), "Home")
			 .put("count", contentCount)
			 .write(root.resolve("index.html"));

		pages.add("404.ftl", SiteMap.Page.of(0f, SiteMap.ChangeFrequency.never), "Not Found")
			 .write(root.resolve("404.html"));

		return pages.pages;
	}
}
