package org.unrealarchive.www;

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

import org.unrealarchive.content.managed.Managed;
import org.unrealarchive.content.managed.ManagedContentRepository;

import static org.unrealarchive.common.Util.slug;

public class ManagedContent implements PageGenerator {

	private final ManagedContentRepository managedRepo;
	private final Path siteRoot;
	private final Path root;
	private final Path staticRoot;
	private final SiteFeatures features;

	public ManagedContent(ManagedContentRepository managedRepo, Path root, Path staticRoot, SiteFeatures features) {
		this.managedRepo = managedRepo;
		this.siteRoot = root;
		this.root = root;
		this.staticRoot = staticRoot;
		this.features = features;
	}

	private Map<String, Game> loadGames(ManagedContentRepository managedRepo) {
		final Map<String, Game> games = new HashMap<>();

		managedRepo.all().stream()
				   .filter(d -> d.published)
				   .sorted(Comparator.reverseOrder())
				   .toList()
				   .forEach(d -> {
					   Game game = games.computeIfAbsent(d.game(), Game::new);
					   game.add(d);
				   });

		return games;
	}

	/**
	 * Generate one or more HTML pages of output.
	 */
	@Override
	public Set<SiteMap.Page> generate() {
		final Map<String, Game> games = loadGames(managedRepo);

		Templates.PageSet pages = new Templates.PageSet("managed", features, siteRoot, staticRoot, root);
		try {
			for (Game game : games.values()) {
				for (Group g : game.groups.values()) {
					generateGroup(pages, g);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to render managed content pages", e);
		}

		return pages.pages;
	}

	private void generateGroup(Templates.PageSet pages, Group group) throws IOException {
		pages.add("group.ftl", SiteMap.Page.monthly(0.7f), String.join(" / ", group.game.name, group.name))
			 .put("group", group)
			 .write(group.path.resolve("index.html"));

		for (SubGroup subgroup : group.subGroups.values()) {
			pages.add("subgroup.ftl", SiteMap.Page.monthly(0.75f), String.join(" / ", group.game.name, group.name, subgroup.name))
				 .put("subgroup", subgroup)
				 .write(subgroup.path.resolve("index.html"));

			for (ContentInfo content : subgroup.content) {
				generateDocument(pages, content);
			}
		}
	}

	private void generateDocument(Templates.PageSet pages, ContentInfo content) throws IOException {
		try (ReadableByteChannel docChan = this.managedRepo.document(content.managed)) {

			// copy content of directory to www output
			final Path path = Files.createDirectories(content.path);
			this.managedRepo.writeContent(content.managed, path);

			final String page = Markdown.renderMarkdown(docChan);

			pages.add("content.ftl", SiteMap.Page.monthly(0.85f, content.managed.updatedDate),
					  String.join(" / ", content.subGroup.parent.game.name, content.subGroup.parent.name, content.subGroup.name,
								  content.managed.title))
				 .put("managed", content)
				 .put("page", page)
				 .write(content.managed.pagePath(root));
		}
	}

	public class Game {

		public final String name;
		public final String slug;
		public final Path path;

		public final TreeMap<String, Group> groups = new TreeMap<>();

		public int count;

		public Game(String name) {
			this.name = name;
			this.slug = slug(name);
			this.path = root.resolve(slug);
			this.count = 0;
		}

		public void add(Managed m) {
			groups.computeIfAbsent(m.group, g -> new Group(this, g)).add(m);

			this.count++;
		}
	}

	public class Group {

		public final String name;
		public final String slug;
		public final Path path;

		public final Game game;
		public final TreeMap<String, SubGroup> subGroups = new TreeMap<>();

		public int count;

		public Group(Game game, String name) {
			this.game = game;
			this.name = name;
			this.slug = slug(name);
			this.path = game.path.resolve(slug);
			this.count = 0;
		}

		public void add(Managed m) {
			subGroups.computeIfAbsent(m.subGroup, g -> new SubGroup(this, g)).add(m);

			this.count++;
		}
	}

	public class SubGroup {

		public final String name;
		public final String slug;
		public final Path path;

		public final Group parent;
		public final List<ContentInfo> content = new ArrayList<>();
		public int count;

		public SubGroup(Group parent, String name) {
			this.parent = parent;

			this.name = name;
			this.slug = slug(name);
			this.path = parent.path.resolve(slug);
			this.count = 0;
		}

		public void add(Managed m) {
			content.add(new ContentInfo(m, this));

			count++;
		}
	}

	public class ContentInfo {

		public final Managed managed;
		public final SubGroup subGroup;

		public final String slug;
		public final Path path;
		public final Path indexPath;

		public ContentInfo(Managed managed, SubGroup subGroup) {
			this.managed = managed;
			this.subGroup = subGroup;

			this.slug = slug(managed.title);
			this.path = managed.slugPath(root);
			this.indexPath = managed.pagePath(root);
		}
	}

}
