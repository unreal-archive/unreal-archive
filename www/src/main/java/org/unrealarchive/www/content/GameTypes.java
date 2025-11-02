package org.unrealarchive.www.content;

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
import java.util.stream.Stream;

import org.unrealarchive.common.Util;
import org.unrealarchive.content.RepositoryManager;
import org.unrealarchive.content.addons.Addon;
import org.unrealarchive.content.addons.GameType;
import org.unrealarchive.www.Markdown;
import org.unrealarchive.www.PageGenerator;
import org.unrealarchive.www.SiteFeatures;
import org.unrealarchive.www.SiteMap;
import org.unrealarchive.www.Templates;
import org.unrealarchive.www.Thumbnails;

import static org.unrealarchive.common.Util.slug;

public class GameTypes implements PageGenerator {

	private static final String SECTION = "Game Types & Mods";

	private static final int THUMB_WIDTH = 350;

	private final RepositoryManager repos;
	private final Path root;
	private final Path staticRoot;
	private final SiteFeatures features;

	public GameTypes(RepositoryManager repos, Path root, Path staticRoot, SiteFeatures features) {
		this.repos = repos;
		this.root = root;
		this.staticRoot = staticRoot;
		this.features = features;
	}

	private Map<String, Game> loadGames() {
		final Map<String, Game> games = new TreeMap<>();

		repos.gameTypes().all().stream()
			 .filter(g -> !g.isVariation())
			 .sorted()
			 .forEach(d -> {
				 Game game = games.computeIfAbsent(d.game, Game::new);
				 game.add(d, repos.gameTypes().variations(d).stream().sorted().toList());
			 });

		return games;
	}

	/**
	 * Generate one or more HTML pages of output.
	 */
	@Override
	public Set<SiteMap.Page> generate() {
		final Map<String, Game> games = loadGames();

		Templates.PageSet pages = new Templates.PageSet("content/gametypes", features, root, staticRoot);
		try {
			for (Game game : games.values()) {
				generateGame(pages, game);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to render gametypes page", e);
		}

		return pages.pages;
	}

	private void generateGame(Templates.PageSet pages, Game game) throws IOException {
		pages.add("game.ftl", SiteMap.Page.weekly(0.95f), String.join(" / ", game.name, SECTION))
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
//		final Path sourcePath = gametypes.path(gametype.gametype).getParent();
		try (ReadableByteChannel docChan = repos.gameTypes().document(gametype.gametype)) {
			final Path outPath = Files.isDirectory(gametype.path) ? gametype.path : Files.createDirectories(gametype.path);
			// copy contents to the output directory
			repos.gameTypes().writeContent(gametype.gametype, outPath);

			// create gallery thumbnails
			gametype.buildGallery(outPath);

			final String page = Markdown.renderMarkdown(docChan);

			Path indexPath = gametype.variationOf == null
				? gametype.indexPath
				: gametype.path.resolve("index.html");

			pages.add("gametype.ftl", SiteMap.Page.weekly(0.97f),
					  String.join(" / ", gametype.gametype.game, SECTION, gametype.gametype.name))
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
				  String.join(" / ", gametype.gametype.game, SECTION, gametype.gametype.name, release.title))
			 .put("gametype", gametype)
			 .put("release", release)
			 .write(outPath.resolve("index.html"));
	}

	public class Game {

		public final String name;
		public final String slug;
		public final Path path;
		public final Path root;

		public final List<GameTypeInfo> gametypes = new ArrayList<>();

		public int count;

		public Game(String name) {
			this.name = name;
			this.slug = slug(name);
			this.root = GameTypes.this.root.resolve(slug);
			this.path = root.resolve("gametypes");
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
				? gametype.slugPath(root)
				: variationOf.slugPath(root).resolve(Util.slug(gametype.name));
			this.indexPath = gametype.pagePath(root);
			this.variationOf = variationOf;
			this.variations = variations;

			this.filesAlsoIn = new HashMap<>();

			for (GameType.Release release : gametype.releases) {
				for (GameType.ReleaseFile file : release.files) {
					if (file.deleted) continue;
					Map<String, Integer> fileMap = filesAlsoIn.computeIfAbsent(slug(file.originalFilename), s -> new HashMap<>());
					for (Addon.ContentFile cf : file.files) {
						int alsoInCount = repos.addons().containingFileCount(cf.hash);
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

		protected void buildGallery(Path outPath) {
			try {
				Path gametypePath = outPath.toAbsolutePath();
				try (Stream<Path> files = Files.list(gametypePath.resolve("gallery"))) {
					this.gallery.putAll(
						files
							.filter(f -> Files.isRegularFile(f) && Util.image(f))
							.filter(f -> !f.getFileName().toString().startsWith("t_"))
							.sorted()
							.collect(Collectors.toMap(f -> gametypePath.relativize(f).toString(), f -> {
								try {
									Path thumb = Thumbnails.thumbnail(
										f, outPath.resolve("gallery").resolve("t_" + Util.fileName(f)), THUMB_WIDTH
									);
									return outPath.relativize(thumb).toString();
								} catch (Exception e) {
									return "";
								}
							}, (k, v) -> v, () -> new LinkedHashMap<>()))
					);
				}
			} catch (IOException e) {
				// pass
			}
		}

	}
}
