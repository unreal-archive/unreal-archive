package net.shrimpworks.unreal.archive.www.content;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.ContentManager;
import net.shrimpworks.unreal.archive.content.GameTypeManager;
import net.shrimpworks.unreal.archive.content.gametypes.GameType;
import net.shrimpworks.unreal.archive.www.PageGenerator;
import net.shrimpworks.unreal.archive.www.SiteFeatures;
import net.shrimpworks.unreal.archive.www.SiteMap;
import net.shrimpworks.unreal.archive.www.Templates;

import static net.shrimpworks.unreal.archive.Util.slug;

public class GameTypes implements PageGenerator {

	private static final String SECTION = "Game Types & Mods";

	private static final int THUMB_WIDTH = 350;

	private final GameTypeManager gametypes;
	private final ContentManager content;
	private final Path siteRoot;
	private final Path root;
	private final Path staticRoot;
	private final SiteFeatures features;

	public GameTypes(GameTypeManager gametypes, ContentManager content, Path root, Path staticRoot,
					 SiteFeatures features) {
		this.gametypes = gametypes;
		this.content = content;
		this.siteRoot = root;
		this.root = root.resolve(slug("gametypes"));
		this.staticRoot = staticRoot;
		this.features = features;
	}

	private Map<String, Game> loadGames(GameTypeManager gametypes) {
		final Map<String, Game> games = new TreeMap<>();

		gametypes.all().stream()
				 .sorted()
				 .forEach(d -> {
					 Game game = games.computeIfAbsent(d.game, Game::new);
					 game.add(d, gametypes.variations(d).stream().sorted().toList());
				 });

		return games;
	}

	/**
	 * Generate one or more HTML pages of output.
	 *
	 * @return number of individual pages created
	 */
	@Override
	public Set<SiteMap.Page> generate() {
		final Map<String, Game> games = loadGames(gametypes);

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

		for (GameTypeInfo g : game.gametypes) {
			generateGameType(pages, g);

			for (GameTypeInfo v : g.variations) {
				generateGameType(pages, v);
			}
		}
	}

	private void generateGameType(Templates.PageSet pages, GameTypeInfo gametype) throws IOException {
		final Path sourcePath = gametypes.path(gametype.gametype).getParent();
		final Path outPath = Files.isDirectory(gametype.path) ? gametype.path : Files.createDirectories(gametype.path);

		try (ReadableByteChannel docChan = this.gametypes.document(gametype.gametype)) {
			// copy contents to output directory
			Util.copyTree(sourcePath, outPath);

			// create gallery thumbnails
			gametype.buildGallery();

			final String page = Templates.renderMarkdown(docChan);

			Path indexPath = gametype.variationOf == null
				? gametype.indexPath
				: gametype.path.resolve("index.html");

			pages.add("gametype.ftl", SiteMap.Page.weekly(0.97f),
					  String.join(" / ", SECTION, gametype.gametype.game, gametype.gametype.name))
				 .put("gametype", gametype)
				 .put("page", page)
				 .write(indexPath);

			for (GameType.Release release : gametype.gametype.releases) {
				generateRelease(pages, gametype, release, outPath.resolve(slug(release.title)));
			}
		}
	}

	private void generateRelease(Templates.PageSet pages, GameTypeInfo gametype, GameType.Release release, Path path)
		throws IOException {

		final Path outPath = Files.isDirectory(path) ? path : Files.createDirectories(path);

		pages.add("release.ftl", SiteMap.Page.monthly(0.9f),
				  String.join(" / ", SECTION, gametype.gametype.game, gametype.gametype.name, release.title))
			 .put("gametype", gametype)
			 .put("release", release)
			 .write(outPath.resolve("index.html"));
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

		public void add(GameType g, List<GameType> variations) {
			gametypes.add(new GameTypeInfo(g, this, variations.stream()
															  .map(v -> new GameTypeInfo(v, this, g))
															  .toList()));
			this.count++;
		}
	}

	public class GameTypeInfo {

		public final GameType gametype;
		public final Game game;
		public final GameType variationOf;
		public final List<GameTypeInfo> variations;

		public final String slug;
		public final Path path;
		public final Path indexPath;

		public final String fallbackTitle;

		public final Map<String, String> gallery; // map of { image -> thumbnail }

		public final Map<String, Map<String, Integer>> filesAlsoIn;

		public GameTypeInfo(GameType gametype, Game game, List<GameTypeInfo> variations) {
			this(gametype, game, null, variations);
		}

		public GameTypeInfo(GameType gametype, Game game, GameType variationOf) {
			this(gametype, game, variationOf, List.of());
		}

		public GameTypeInfo(GameType gametype, Game game, GameType variationOf, List<GameTypeInfo> variations) {
			this.gametype = gametype;
			this.game = game;

			this.slug = slug(gametype.name);
			this.path = variationOf == null
				? gametype.slugPath(siteRoot)
				: variationOf.slugPath(siteRoot).resolve(Util.slug(gametype.name));
			this.indexPath = gametype.pagePath(siteRoot);
			this.variationOf = variationOf;
			this.variations = variations;

			this.filesAlsoIn = new HashMap<>();

			for (GameType.Release release : gametype.releases) {
				for (GameType.ReleaseFile file : release.files) {
					if (file.deleted) continue;
					Map<String, Integer> fileMap = filesAlsoIn.computeIfAbsent(slug(file.originalFilename), s -> new HashMap<>());
					for (Content.ContentFile cf : file.files) {
						int alsoInCount = content.containingFileCount(cf.hash);
						if (alsoInCount > 0) fileMap.put(cf.hash, alsoInCount);
					}
				}
			}

			Collections.sort(gametype.releases);
			Collections.reverse(gametype.releases);

			if (gametype.bannerImage.isBlank()) {
				this.fallbackTitle = gametype.maps.stream()
												  .filter(g -> g.screenshot != null)
												  .map(g -> g.screenshot.url)
												  .findAny()
												  .orElse("");
			} else this.fallbackTitle = "";

			this.gallery = new LinkedHashMap<>();
		}

		protected void buildGallery() {
			try {
				Path gametypePath = gametypes.path(gametype).getParent().toAbsolutePath();
				this.gallery.putAll(Files.list(gametypePath.resolve("gallery"))
										 .filter(f -> Files.isRegularFile(f) && Util.image(f))
										 .sorted()
										 .collect(Collectors.toMap(f -> gametypePath.relativize(f).toString(), f -> {
											 try {
												 Path thumb = Util.thumbnail(f, path.resolve("gallery").resolve("t_" + Util.fileName(f)),
																			 THUMB_WIDTH);
												 return path.relativize(thumb).toString();
											 } catch (Exception e) {
												 return "";
											 }
										 }, (k, v) -> v, () -> new LinkedHashMap<>())));
			} catch (IOException e) {
				// pass
			}
		}

	}
}
