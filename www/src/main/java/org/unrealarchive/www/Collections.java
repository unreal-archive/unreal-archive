package org.unrealarchive.www;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.unrealarchive.common.Util;
import org.unrealarchive.content.ContentCollection;
import org.unrealarchive.content.ContentEntity;
import org.unrealarchive.content.RepositoryManager;

/**
 * Generates the Collections landing page and detail pages for stored collections.
 */
public class Collections implements PageGenerator {

	private final RepositoryManager repos;
	private final Path root;
	private final Path staticRoot;
	private final SiteFeatures features;

	public Collections(RepositoryManager repos, Path root, Path staticRoot, SiteFeatures features) {
		this.repos = repos;
		this.root = root;
		this.staticRoot = staticRoot;
		this.features = features;
	}

	@Override
	public Set<SiteMap.Page> generate() {
		Templates.PageSet pages = new Templates.PageSet("collections", features, root, staticRoot);

		final List<CollectionPage> published = repos.collections().all().stream()
													.filter(c -> c.published)
													.sorted(Comparator.comparing((ContentCollection c) -> c.createdDate != null
														? c.createdDate
														: LocalDate.MIN).reversed())
													.map(c -> {
														List<ResolvedContent> items = c.items.stream()
																							 .map(
																								 i -> new ResolvedContent(i, repos.forId(
																								 i.id)))
																							 .toList();
														return new CollectionPage(c, pathFor(c), items);
													})
													.toList();

		try {
			// index page
			pages.add("index.ftl", SiteMap.Page.monthly(0.8f), "Collections")
				 .put("collections", published)
				 .write(Files.createDirectories(root.resolve("collections")).resolve("index.html"));

			// editor page
			pages.add("editor.ftl", SiteMap.Page.monthly(0f), "Collections Editor")
				 .write(Files.createDirectories(root.resolve("collections")).resolve("editor.html"));

			// detail pages
			for (CollectionPage c : published) {
				outputPage(pages, c);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to render collections pages", e);
		}

		return pages.pages;
	}

	private void outputPage(Templates.PageSet pages, CollectionPage c) throws IOException {
		// copy content of directory to www output
		final Path path = Files.createDirectories(c.path.getParent());
		repos.collections().writeContent(c.collection, path);

		pages.add("collection.ftl", SiteMap.Page.monthly(0.85f), String.join(" / ", "Collections", c.collection.title))
			 .put("collection", c)
			 .write(c.path);
	}

	private Path pathFor(ContentCollection c) {
		String slug = Util.slug(c.title != null ? c.title : "collection");
		return root.resolve("collections").resolve(slug).resolve("index.html");
	}

	public static class CollectionPage {

		public final ContentCollection collection;
		public final Path path;
		public final List<ResolvedContent> items;

		public CollectionPage(ContentCollection collection, Path path, List<ResolvedContent> items) {
			this.collection = collection;
			this.path = path;
			this.items = items;
		}
	}

	public record ResolvedContent(
		ContentCollection.CollectionItem item,
		ContentEntity<?> content
	) {}
}
