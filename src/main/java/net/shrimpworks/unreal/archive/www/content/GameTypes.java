package net.shrimpworks.unreal.archive.www.content;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

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

	private static final Set<String> IMGS = Set.of("png", "bmp", "gif", "jpg", "jpeg");
	private static final int THUMB_WIDTH = 350;

	private final GameTypeManager gametypes;
	private final ContentManager content;
	private final Path siteRoot;
	private final Path root;
	private final Path staticRoot;
	private final SiteFeatures features;

	private final Map<String, Game> games;

	public GameTypes(GameTypeManager gametypes, ContentManager content, Path root, Path staticRoot,
					 SiteFeatures features) {
		this.gametypes = gametypes;
		this.content = content;
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

			// create gallery thumbnails
			gametype.buildGallery();

			final String page = Templates.renderMarkdown(docChan);

			pages.add("gametype.ftl", SiteMap.Page.weekly(0.97f),
					  String.join(" / ", SECTION, gametype.gametype.game, gametype.gametype.name))
				 .put("gametype", gametype)
				 .put("page", page)
				 .write(outPath.resolve("index.html"));

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

		public final Map<String, String> gallery; // map of { image -> thumbnail }

		public final Map<String, Map<String, Integer>> filesAlsoIn;

		public GameTypeInfo(GameType gametype, Game game) {
			this.gametype = gametype;
			this.game = game;

			this.slug = slug(gametype.name);
			this.path = gametype.slugPath(root);

			this.filesAlsoIn = new HashMap<>();

			for (GameType.Release release : gametype.releases) {
				for (GameType.ReleaseFile file : release.files) {
					if (file.deleted) continue;
					Map<String, Integer> fileMap = filesAlsoIn.computeIfAbsent(slug(file.originalFilename), s -> new HashMap<>());
					for (Content.ContentFile cf : file.files) {
						Collection<Content> containing = content.containingFile(cf.hash);
						if (!containing.isEmpty()) {
							fileMap.put(cf.hash, containing.size());
						}
					}
				}
			}

			this.gallery = new HashMap<>();
		}

		protected void buildGallery() {
			try {
				Path gametypePath = gametypes.path(gametype).getParent().toAbsolutePath();
				this.gallery.putAll(Files.list(gametypePath.resolve("gallery"))
										 .filter(f -> Files.isRegularFile(f) && IMGS.contains(Util.extension(f).toLowerCase()))
										 .collect(Collectors.toMap(f -> gametypePath.relativize(f).toString(), f -> {
											 try {
												 Path thumb = makeThumb(f, path.resolve("gallery"), THUMB_WIDTH);
												 return path.relativize(thumb).toString();
											 } catch (Exception e) {
												 return "";
											 }
										 })));
			} catch (IOException e) {
				// pass
			}
		}

		private Path makeThumb(Path source, Path dest, int width) throws IOException {
			Path thumbPath = dest.resolve("t_" + Util.fileName(source));
			if (Files.exists(thumbPath)) return thumbPath;

			BufferedImage image = ImageIO.read(source.toFile());
			double scale = (double)width / image.getWidth();
			BufferedImage thumb = new BufferedImage((int)(image.getWidth() * scale),
													(int)(image.getHeight() * scale),
													BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics = thumb.createGraphics();
			graphics.drawImage(image.getScaledInstance(thumb.getWidth(), thumb.getHeight(), Image.SCALE_FAST), 0, 0, null);

			ImageIO.write(thumb, Util.extension(source), thumbPath.toFile());

			return thumbPath;
		}
	}
}
