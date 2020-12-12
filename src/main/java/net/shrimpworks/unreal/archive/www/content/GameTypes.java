package net.shrimpworks.unreal.archive.www.content;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.content.GameTypeManager;
import net.shrimpworks.unreal.archive.content.gametypes.GameType;
import net.shrimpworks.unreal.archive.www.PageGenerator;
import net.shrimpworks.unreal.archive.www.SiteFeatures;
import net.shrimpworks.unreal.archive.www.SiteMap;
import net.shrimpworks.unreal.archive.www.Templates;

import static net.shrimpworks.unreal.archive.Util.slug;

public class GameTypes implements PageGenerator {

	private static final String SECTION = "Game Types & Mods";

	private final GameTypeManager gametypes;
	private final Path siteRoot;
	private final Path root;
	private final Path staticRoot;
	private final SiteFeatures features;

	private final Map<String, Game> games;

	public GameTypes(GameTypeManager gametypes, Path root, Path staticRoot, SiteFeatures features) {
		this.gametypes = gametypes;
		this.siteRoot = root;
		this.root = root.resolve(slug("gametypes"));
		this.staticRoot = staticRoot;
		this.features = features;

		this.games = new HashMap<>();

		gametypes.all().stream()
				 .sorted()
				 .collect(Collectors.toList())
				 .forEach(d -> {
					 Game game = games.computeIfAbsent(d.game, Game::new);
					 game.add(d);
				 });
	}

	/**
	 * Generate one or more HTML pages of output.
	 *
	 * @return number of individual pages created
	 */
	@Override
	public Set<SiteMap.Page> generate() {
		Templates.PageSet pages = new Templates.PageSet("content/gametypes", features, siteRoot, staticRoot, root);
		try {
			pages.add("games.ftl", SiteMap.Page.monthly(0.6f), SECTION)
				 .put("games", games)
				 .write(root.resolve("index.html"));

			for (Game game : games.values()) {
				generateGame(pages, game);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to render gametypes page", e);
		}

		return pages.pages;
	}

	private void generateGame(Templates.PageSet pages, Game game) throws IOException {
		pages.add("game.ftl", SiteMap.Page.weekly(0.95f), String.join(" / ", SECTION, game.name))
			 .put("game", game)
			 .write(game.path.resolve("index.html"));

		for (GameTypeInfo d : game.gametypes) {
			generateGameType(pages, d);
		}
	}

	private void generateGameType(Templates.PageSet pages, GameTypeInfo gametype) throws IOException {
		final Path sourcePath = gametypes.path(gametype.gametype).getParent();
		final Path outPath = Files.isDirectory(gametype.path) ? gametype.path : Files.createDirectories(gametype.path);

		try (ReadableByteChannel docChan = this.gametypes.document(gametype.gametype)) {
			// copy contents to output directory
			Util.copyTree(sourcePath, outPath);

			final String page = Templates.renderMarkdown(docChan);

			pages.add("gametype.ftl", SiteMap.Page.weekly(0.97f),
					  String.join(" / ", SECTION, gametype.gametype.game, gametype.gametype.name))
//				 .put("groupPath", groupPath)
				 .put("gametype", gametype)
				 .put("page", page)
				 .write(outPath.resolve("index.html"));
		}

//		try (ReadableByteChannel docChan = this.gametypes.document(content.managed)) {
//
//			// we have to compute the path here, since a template can't do a while loop up its group tree itself
//			List<ManagedContent.ContentGroup> groupPath = new ArrayList<>();
//			ManagedContent.ContentGroup grp = content.group;
//			while (grp != null) {
//				groupPath.add(0, grp);
//				grp = grp.parent;
//			}
//
//			// copy content of directory to www output
//			final Path path = Files.createDirectories(content.path);
//			final Path docRoot = this.gametypes.contentRoot(content.managed);
//			Util.copyTree(docRoot, path);
//
//			final String page = Templates.renderMarkdown(docChan);
//
//			pages.add("content.ftl", SiteMap.Page.monthly(0.85f, content.managed.updatedDate),
//					  String.join(" / ", section, content.managed.game, String.join(" / ", content.managed.path.split("/")),
//								  content.managed.title))
//				 .put("groupPath", groupPath)
//				 .put("managed", content)
//				 .put("page", page)
//				 .write(path.resolve("index.html"));
//		}
	}

	public class Game {

		public final String name;
		public final String slug;
		public final Path path;

		public final List<GameTypeInfo> gametypes = new ArrayList<>();

		public int count;

		public Game(String name) {
			this.name = name;
			this.slug = slug(name);
			this.path = root.resolve(slug);
			this.count = 0;
		}

		public void add(GameType g) {
			gametypes.add(new GameTypeInfo(g, this));
			this.count++;
		}
	}

	public class GameTypeInfo {

		public final GameType gametype;
		public final Game game;

		public final String slug;
		public final Path path;

		public GameTypeInfo(GameType gametype, Game game) {
			this.gametype = gametype;
			this.game = game;

			this.slug = slug(gametype.name);
			this.path = gametype.slugPath(root);
		}
	}

}
