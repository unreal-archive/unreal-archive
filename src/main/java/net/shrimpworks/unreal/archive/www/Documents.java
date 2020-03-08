package net.shrimpworks.unreal.archive.www;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.docs.Document;
import net.shrimpworks.unreal.archive.docs.DocumentManager;

import static net.shrimpworks.unreal.archive.Util.slug;

public class Documents implements PageGenerator {

	private static final String SECTION = "Articles";

	private final DocumentManager documents;
	private final Path siteRoot;
	private final Path root;
	private final Path staticRoot;
	private final SiteFeatures features;

	private final Map<String, DocumentGroup> groups;

	public Documents(DocumentManager documents, Path root, Path staticRoot, SiteFeatures features) {
		this.documents = documents;
		this.siteRoot = root;
		this.root = root.resolve("documents");
		this.staticRoot = staticRoot;
		this.features = features;

		this.groups = new HashMap<>();

		documents.all().stream()
				 .filter(d -> d.published)
				 .sorted(Comparator.reverseOrder())
				 .forEach(d -> {
					 DocumentGroup group = groups.computeIfAbsent(d.game, g -> new DocumentGroup(null, g));
					 group.add(d);
				 });
	}

	/**
	 * Generate one or more HTML pages of output.
	 *
	 * @return number of individual pages created
	 */
	@Override
	public Set<SiteMap.Page> generate() {
		Templates.PageSet pages = new Templates.PageSet("docs", features, siteRoot, staticRoot, root);
		try {
			// create the root landing page, for reasons
			DocumentGroup rootGroup = new DocumentGroup(null, "");
			rootGroup.groups.putAll(groups);
			generateGroup(pages, rootGroup);
		} catch (IOException e) {
			throw new RuntimeException("Failed to render page", e);
		}

		return pages.pages;
	}

	private void generateGroup(Templates.PageSet pages, DocumentGroup group) throws IOException {
		// we have to compute the path here, since a template can't do a while loop up its group tree itself
		List<DocumentGroup> groupPath = new ArrayList<>();
		DocumentGroup grp = group;
		while (grp != null) {
			groupPath.add(0, grp);
			grp = grp.parent;
		}

		pages.add("group.ftl", SiteMap.Page.weekly(0.6f), String.join(" / ", SECTION, String.join(" / ", group.parentPath.split("/"))))
			 .put("groupPath", groupPath)
			 .put("group", group)
			 .write(group.path.resolve("index.html"));

		for (DocumentGroup g : group.groups.values()) {
			generateGroup(pages, g);
		}

		for (DocumentInfo d : group.documents) {
			generateDocument(pages, d);
		}
	}

	private void generateDocument(Templates.PageSet pages, DocumentInfo doc) throws IOException {
		try (ReadableByteChannel docChan = documents.document(doc.document)) {

			// we have to compute the path here, since a template can't do a while loop up its group tree itself
			List<DocumentGroup> groupPath = new ArrayList<>();
			DocumentGroup grp = doc.group;
			while (grp != null) {
				groupPath.add(0, grp);
				grp = grp.parent;
			}

			final Path path = Files.createDirectories(doc.path);
			final Path docRoot = documents.documentRoot(doc.document);
			Util.copyTree(docRoot, path);

			final String page = Templates.renderMarkdown(docChan);

			pages.add("document.ftl", SiteMap.Page.monthly(0.8f, doc.document.updatedDate),
					  String.join(" / ", SECTION, doc.document.game, String.join(" / ", doc.document.path.split("/")), doc.document.title))
				 .put("groupPath", groupPath)
				 .put("document", doc)
				 .put("page", page)
				 .write(path.resolve("index.html"));
		}
	}

	public class DocumentGroup {

		private final String parentPath;

		public final DocumentGroup parent;

		public final String name;
		public final String slug;
		public final Path path;

		public final TreeMap<String, DocumentGroup> groups = new TreeMap<>();
		public final List<DocumentInfo> documents = new ArrayList<>();

		public int docs;

		public DocumentGroup(DocumentGroup parent, String name) {
			this.parentPath = parent != null ? parent.parentPath.isEmpty() ? name : String.join("/", parent.parentPath, name) : "";

			this.parent = parent;

			this.name = name;
			this.slug = slug(name);
			this.path = parent != null ? parent.path.resolve(slug) : root.resolve(slug);
			this.docs = 0;
		}

		public void add(Document d) {
			Path docPath = d.slugPath(root);

			if (docPath.getParent().equals(this.path)) {
				// reached leaf of path tree for this document, place it here
				documents.add(new DocumentInfo(d, this));
			} else {
				// this document lives further down the tree, keep adding paths
				String[] next = (parentPath.isEmpty() ? d.path : d.path.replaceFirst(parentPath + "/", "")).split("/");
				String nextName = (next.length > 0 && !next[0].isEmpty()) ? next[0] : "";

				DocumentGroup group = groups.computeIfAbsent(nextName, g -> new DocumentGroup(this, g));
				group.add(d);
			}
			this.docs++;
		}
	}

	public class DocumentInfo {

		public final Document document;
		public final DocumentGroup group;

		public final String slug;
		public final Path path;

		public DocumentInfo(Document document, DocumentGroup group) {
			this.document = document;
			this.group = group;

			this.slug = slug(document.title);
			this.path = document.slugPath(root);
		}
	}

}
