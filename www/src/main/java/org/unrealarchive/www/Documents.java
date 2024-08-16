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

import org.unrealarchive.content.docs.Document;
import org.unrealarchive.content.docs.DocumentRepository;

import static org.unrealarchive.common.Util.slug;

public class Documents implements PageGenerator {

	private static final String SECTION = "Guides & Reference";

	private final DocumentRepository documents;
	private final Path root;
	private final Path staticRoot;
	private final SiteFeatures features;

	public Documents(DocumentRepository documents, Path root, Path staticRoot, SiteFeatures features) {
		this.documents = documents;
		this.root = root;
		this.staticRoot = staticRoot;
		this.features = features;
	}

	private Map<String, Game> loadGames(DocumentRepository documents) {
		final Map<String, Game> groups = new HashMap<>();
		documents.all().stream()
				 .filter(d -> d.published)
				 .sorted(Comparator.reverseOrder())
				 .forEach(d -> {
					 Game game = groups.computeIfAbsent(d.game, Game::new);
					 game.add(d);
				 });
		return groups;
	}

	/**
	 * Generate one or more HTML pages of output.
	 */
	@Override
	public Set<SiteMap.Page> generate() {
		final Map<String, Game> games = loadGames(documents);

		Templates.PageSet pages = new Templates.PageSet("documents", features, root, staticRoot);
		try {
			for (Game game : games.values()) {

				pages.add("game.ftl", SiteMap.Page.monthly(0.7f), String.join(" / ", game.name, SECTION))
					 .put("game", game)
					 .write(game.path.resolve("index.html"));

				for (Group g : game.groups.values()) {
					generateGroup(pages, g);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to render pages", e);
		}

		return pages.pages;
	}

	private void generateGroup(Templates.PageSet pages, Group group) throws IOException {
		pages.add("group.ftl", SiteMap.Page.monthly(0.7f), String.join(" / ", group.game.name, SECTION, group.name))
			 .put("group", group)
			 .write(group.path.resolve("index.html"));

		for (SubGroup subgroup : group.subGroups.values()) {
			pages.add("subgroup.ftl", SiteMap.Page.monthly(0.75f), String.join(" / ", group.game.name, SECTION, group.name, subgroup.name))
				 .put("subgroup", subgroup)
				 .write(subgroup.path.resolve("index.html"));

			for (DocumentInfo content : subgroup.documents) {
				generateDocument(pages, content);
			}
		}
	}

	private void generateDocument(Templates.PageSet pages, DocumentInfo doc) throws IOException {
		try (ReadableByteChannel docChan = this.documents.document(doc.document)) {

			// copy content of directory to www output
			final Path path = Files.createDirectories(doc.path);
			this.documents.writeContent(doc.document, path);

			final String page = Markdown.renderMarkdown(docChan);

			pages.add("document.ftl", SiteMap.Page.monthly(0.85f, doc.document.updatedDate),
					  String.join(" / ", doc.subGroup.parent.game.name, SECTION, doc.subGroup.parent.name, doc.subGroup.name,
								  doc.document.title))
				 .put("document", doc)
				 .put("page", page)
				 .write(doc.document.pagePath(root));
		}
	}

	public class Game {

		public final String name;
		public final String slug;
		public final Path root;
		public final Path path;

		public final TreeMap<String, Group> groups = new TreeMap<>();

		public int count;

		public Game(String name) {
			this.name = name;
			this.slug = slug(name);
			this.root = Documents.this.root.resolve(slug);
			this.path = root.resolve("documents");
			this.count = 0;
		}

		public void add(Document d) {
			groups.computeIfAbsent(d.group, g -> new Group(this, g)).add(d);

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

		public void add(Document d) {
			subGroups.computeIfAbsent(d.subGroup, g -> new SubGroup(this, g)).add(d);

			this.count++;
		}
	}

	public class SubGroup {

		public final String name;
		public final String slug;
		public final Path path;

		public final Group parent;
		public final List<DocumentInfo> documents = new ArrayList<>();
		public int count;

		public SubGroup(Group parent, String name) {
			this.parent = parent;

			this.name = name;
			this.slug = slug(name);
			this.path = parent.path.resolve(slug);
			this.count = 0;
		}

		public void add(Document d) {
			documents.add(new DocumentInfo(d, this));

			count++;
		}
	}

	public class DocumentInfo {

		public final Document document;
		public final SubGroup subGroup;

		public final String slug;
		public final Path path;

		public DocumentInfo(Document document, SubGroup subGroup) {
			this.document = document;
			this.subGroup = subGroup;

			this.slug = slug(document.title);
			this.path = document.slugPath(root);
		}
	}

}
