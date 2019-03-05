package net.shrimpworks.unreal.archive.www;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.shrimpworks.unreal.archive.managed.Managed;
import net.shrimpworks.unreal.archive.managed.ManagedContentManager;

import static net.shrimpworks.unreal.archive.www.Templates.slug;

public class ManagedContent implements PageGenerator {

	private final ManagedContentManager content;
	private final Path root;
	private final Path staticRoot;
	private final String section;

	private final Map<String, ContentGroup> groups;

	public ManagedContent(ManagedContentManager content, Path root, Path staticRoot, String section) {
		this.content = content;
		this.root = root.resolve(slug(section));
		this.staticRoot = staticRoot;
		this.section = section;

		this.groups = new HashMap<>();

		content.all().stream()
			   .filter(d -> d.published)
			   .forEach(d -> {
				   ContentGroup group = groups.computeIfAbsent(d.game, g -> new ContentGroup(null, g));
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
		Set<SiteMap.Page> pages = new HashSet<>();
		try {
			// create the root landing page, for reasons
			ContentGroup rootGroup = new ContentGroup(null, "");
			rootGroup.groups.putAll(groups);
			pages.addAll(generateGroup(rootGroup));
		} catch (IOException e) {
			throw new RuntimeException("Failed to render page", e);
		}

		return pages;
	}

	private Set<SiteMap.Page> generateGroup(ContentGroup group) throws IOException {
		Set<SiteMap.Page> pages = new HashSet<>();

		// we have to compute the path here, since a template can't do a while loop up its group tree itself
		List<ContentGroup> groupPath = new ArrayList<>();
		ContentGroup grp = group;
		while (grp != null) {
			groupPath.add(0, grp);
			grp = grp.parent;
		}

		pages.add(Templates.template("managed/group.ftl", SiteMap.Page.weekly(0.65f))
						   .put("static", root.resolve(group.path).relativize(staticRoot))
						   .put("title", String.join(" / ", section, String.join(" / ", group.pPath.split("/"))))
						   .put("groupPath", groupPath)
						   .put("group", group)
						   .put("siteRoot", root.resolve(group.path).relativize(root))
						   .write(root.resolve(group.path).resolve("index.html")));

		for (ContentGroup g : group.groups.values()) {
			pages.addAll(generateGroup(g));
		}

		for (ContentInfo d : group.content) {
			pages.add(generateDocument(d));
		}

		return pages;
	}

	private SiteMap.Page generateDocument(ContentInfo content) throws IOException {
		try (ReadableByteChannel docChan = this.content.document(content.managed)) {

			// we have to compute the path here, since a template can't do a while loop up its group tree itself
			List<ContentGroup> groupPath = new ArrayList<>();
			ContentGroup grp = content.group;
			while (grp != null) {
				groupPath.add(0, grp);
				grp = grp.parent;
			}

			final Path path = Files.createDirectories(root.resolve(content.path));

			final Path docRoot = this.content.contentRoot(content.managed);
			Files.walk(docRoot, FileVisitOption.FOLLOW_LINKS)
				 .forEach(p -> {
					 if (Files.isRegularFile(p)) {
						 Path relPath = docRoot.relativize(p);
						 Path copyPath = path.resolve(relPath);

						 try {
							 if (!Files.isDirectory(copyPath.getParent())) Files.createDirectories(copyPath.getParent());
							 Files.copy(p, copyPath, StandardCopyOption.REPLACE_EXISTING);
						 } catch (IOException e) {
							 e.printStackTrace();
						 }
					 }
				 });

			final String page = Templates.renderMarkdown(docChan);

			return Templates.template("managed/content.ftl", SiteMap.Page.monthly(0.85f, content.managed.updatedDate))
							.put("static", path.relativize(staticRoot))
							.put("title",
								 String.join(" / ", section, content.managed.game, String.join(" / ", content.managed.path.split("/")),
											 content.managed.title))
							.put("groupPath", groupPath)
							.put("managed", content)
							.put("page", page)
							.put("siteRoot", path.relativize(root))
							.write(path.resolve("index.html"));
		}
	}

	public class ContentGroup {

		private final String pPath;

		public final String name;
		public final String slug;
		public final String path;
		public final ContentGroup parent;

		public final TreeMap<String, ContentGroup> groups = new TreeMap<>();
		public final List<ContentInfo> content = new ArrayList<>();

		public int count;

		public ContentGroup(ContentGroup parent, String name) {
			this.pPath = parent != null ? parent.pPath.isEmpty() ? name : String.join("/", parent.pPath, name) : "";

			this.parent = parent;

			this.name = name;
			this.slug = slug(name);
			this.path = parent != null ? String.join("/", parent.path, slug) : slug;
			this.count = 0;
		}

		public void add(Managed m) {
			if (m.path.equals(pPath)) {
				content.add(new ContentInfo(m, this));
			} else {
				String[] next = (pPath.isEmpty() ? m.path : m.path.replaceFirst(pPath + "/", "")).split("/");
				String nextName = (next.length > 0 && !next[0].isEmpty()) ? next[0] : "";

				ContentGroup group = groups.computeIfAbsent(nextName, g -> new ContentGroup(this, g));
				group.add(m);
			}
			this.count++;
		}
	}

	public class ContentInfo {

		public final Managed managed;
		public final ContentGroup group;

		public final String slug;
		public final String path;

		public ContentInfo(Managed managed, ContentGroup group) {
			this.managed = managed;
			this.group = group;

			this.slug = slug(managed.title);
			this.path = group != null ? String.join("/", group.path, slug) : slug;
		}
	}

}
