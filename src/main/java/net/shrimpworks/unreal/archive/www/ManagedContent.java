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
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.managed.Managed;
import net.shrimpworks.unreal.archive.managed.ManagedContentManager;

import static net.shrimpworks.unreal.archive.Util.slug;

public class ManagedContent implements PageGenerator {

	private final ManagedContentManager content;
	private final Path siteRoot;
	private final Path staticRoot;
	private final SiteFeatures features;

	private final Map<String, ContentGroup> groups;

	public ManagedContent(ManagedContentManager content, Path root, Path staticRoot, SiteFeatures features) {
		this.content = content;
		this.siteRoot = root;
		this.staticRoot = staticRoot;
		this.features = features;

		this.groups = new HashMap<>();

		content.all().stream()
			   .filter(d -> d.published)
			   .sorted(Comparator.reverseOrder())
			   .collect(Collectors.toList())
			   .forEach(d -> {
				   ContentGroup group = groups.computeIfAbsent(d.group, g -> new ContentGroup(null, g, 0));
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
		Templates.PageSet pages = new Templates.PageSet("managed", features, siteRoot, staticRoot, siteRoot);
		try {
			// create the root landing page, for reasons
			ContentGroup rootGroup = new ContentGroup(null, "", 0);
			rootGroup.groups.putAll(groups);
			generateGroup(pages, rootGroup);
		} catch (IOException e) {
			throw new RuntimeException("Failed to render page", e);
		}

		return pages.pages;
	}

	private void generateGroup(Templates.PageSet pages, ContentGroup group) throws IOException {
		// we have to compute the path here, since a template can't do a while loop up its group tree itself
		List<ContentGroup> groupPath = new ArrayList<>();
		ContentGroup grp = group;
		while (grp != null) {
			groupPath.add(0, grp);
			grp = grp.parent;
		}

		pages.add("group.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", group.parentPath.split("/")))
			 .put("groupPath", groupPath)
			 .put("group", group)
			 .write(group.path.resolve("index.html"));

		for (ContentGroup g : group.groups.values()) {
			generateGroup(pages, g);
		}

		for (ContentInfo d : group.content) {
			generateDocument(pages, d);
		}
	}

	private void generateDocument(Templates.PageSet pages, ContentInfo content) throws IOException {
		try (ReadableByteChannel docChan = this.content.document(content.managed)) {

			// we have to compute the path here, since a template can't do a while loop up its group tree itself
			List<ContentGroup> groupPath = new ArrayList<>();
			ContentGroup grp = content.group;
			while (grp != null) {
				groupPath.add(0, grp);
				grp = grp.parent;
			}

			// copy content of directory to www output
			final Path path = Files.createDirectories(content.path);
			final Path docRoot = this.content.contentRoot(content.managed);
			Util.copyTree(docRoot, path);

			final String page = Templates.renderMarkdown(docChan);

			pages.add("content.ftl", SiteMap.Page.monthly(0.85f, content.managed.updatedDate),
					  String.join(" / ", String.join(" / ", content.managed.fullPath().split("/")), content.managed.title))
				 .put("groupPath", groupPath)
				 .put("managed", content)
				 .put("page", page)
				 .write(content.managed.pagePath(siteRoot));
		}
	}

	public class ContentGroup {

		private final String parentPath;

		public final String name;
		public final String slug;
		public final Path path;
		public final ContentGroup parent;

		public final TreeMap<String, ContentGroup> groups = new TreeMap<>();
		public final List<ContentInfo> content = new ArrayList<>();

		public int count;
		private final int depth;

		public ContentGroup(ContentGroup parent, String name, int depth) {
			this.depth = depth;
			this.parentPath = parent != null ? parent.parentPath.isEmpty() ? name : String.join("/", parent.parentPath, name) : "";

			this.parent = parent;

			this.name = name;
			this.slug = slug(name);
			this.path = parent != null ? parent.path.resolve(slug) : siteRoot.resolve(slug);
			this.count = 0;
		}

		public void add(Managed m) {
			Path docPath = m.slugPath(siteRoot);

			// TODO REVIEW fragile

			if (docPath.getParent().equals(this.path)) {
				// reached leaf of path tree for this content, place it here
				content.add(new ContentInfo(m, this));
			} else {
				// this content lives further down the tree, keep adding paths
				String[] next = m.fullPath().split("/");
				String nextName = next[depth + 1];

				ContentGroup group = groups.computeIfAbsent(nextName, g -> new ContentGroup(this, g, depth + 1));
				group.add(m);
			}
			this.count++;
		}
	}

	public class ContentInfo {

		public final Managed managed;
		public final ContentGroup group;

		public final String slug;
		public final Path path;
		public final Path indexPath;

		public ContentInfo(Managed managed, ContentGroup group) {
			this.managed = managed;
			this.group = group;

			this.slug = slug(managed.title);
			this.path = managed.slugPath(siteRoot);
			this.indexPath = managed.pagePath(siteRoot);
		}
	}

}
