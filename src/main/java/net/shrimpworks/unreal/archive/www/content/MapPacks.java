package net.shrimpworks.unreal.archive.www.content;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.ContentManager;
import net.shrimpworks.unreal.archive.content.mappacks.MapPack;
import net.shrimpworks.unreal.archive.www.SiteFeatures;
import net.shrimpworks.unreal.archive.www.SiteMap;
import net.shrimpworks.unreal.archive.www.Templates;

import static net.shrimpworks.unreal.archive.Util.slug;

public class MapPacks extends ContentPageGenerator {

	private static final String SECTION = "Map Packs";

	private final Games games;

	public MapPacks(ContentManager content, Path output, Path staticRoot, SiteFeatures features) {
		super(content, output, output.resolve("mappacks"), staticRoot, features);

		this.games = new Games();

		content.get(MapPack.class).stream()
			   .filter(m -> !m.deleted)
			   .filter(m -> m.variationOf == null || m.variationOf.isEmpty())
			   .sorted()
			   .forEach(p -> {
				   Game g = games.games.computeIfAbsent(p.game, Game::new);
				   g.add(p);
			   });
	}

	@Override
	public Set<SiteMap.Page> generate() {
		Templates.PageSet pages = pageSet("content/mappacks");

		pages.add("games.ftl", SiteMap.Page.monthly(0.6f), SECTION)
			 .put("games", games)
			 .write(root.resolve("index.html"));

		games.games.entrySet().parallelStream().forEach(g -> {

			var game = net.shrimpworks.unreal.archive.content.Games.byName(g.getKey());

			pages.add("gametypes.ftl", SiteMap.Page.monthly(0.62f), String.join(" / ", SECTION, game.bigName))
				 .put("game", g.getValue())
				 .write(g.getValue().path.resolve("index.html"));

			g.getValue().gametypes.entrySet().parallelStream().forEach(gt -> {
				gt.getValue().pages.parallelStream().forEach(p -> {
					// don't bother creating numbered single page, default landing page will suffice
					if (gt.getValue().pages.size() > 1) {
						pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName, gt.getKey()))
							 .put("page", p)
							 .write(p.path.resolve("index.html"));
					}

					p.packs.parallelStream().forEach(pack -> packPage(pages, pack));
				});

				// output first letter/page combo, with appropriate relative links
				pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName, gt.getKey()))
					 .put("page", gt.getValue().pages.get(0))
					 .write(gt.getValue().path.resolve("index.html"));
			});
		});

		return pages.pages;
	}

	private void packPage(Templates.PageSet pages, MapPackInfo pack) {
		localImages(pack.pack, pack.path.getParent());

		pages.add("mappack.ftl", SiteMap.Page.monthly(0.9f, pack.pack.firstIndex), String.join(" / ", SECTION,
																							  pack.page.gametype.game.game.bigName,
																							  pack.page.gametype.name, pack.pack.name))
			 .put("pack", pack)
			 .write(Paths.get(pack.path.toString() + ".html"));

		for (MapPackInfo variation : pack.variations) {
			packPage(pages, variation);
		}
	}

	public class Games {

		public final TreeMap<String, Game> games = new TreeMap<>();
	}

	public class Game {

		public final net.shrimpworks.unreal.archive.content.Games game;
		public final String name;
		public final String slug;
		public final Path path;
		public final TreeMap<String, Gametype> gametypes = new TreeMap<>();
		public int packs;

		public Game(String name) {
			this.game = net.shrimpworks.unreal.archive.content.Games.byName(name);
			this.name = name;
			this.slug = slug(name);
			this.path = root.resolve(slug);
		}

		public void add(MapPack p) {
			Gametype gametype = gametypes.computeIfAbsent(p.gametype, g -> new Gametype(this, g));
			gametype.add(p);
			this.packs++;
		}
	}

	public class Gametype {

		public final Game game;

		public final String name;
		public final String slug;
		public final Path path;
		public final List<Page> pages = new ArrayList<>();
		public int packs;

		public Gametype(Game game, String name) {
			this.game = game;
			this.name = name;
			this.slug = slug(name);
			this.path = game.path.resolve(slug);
			this.packs = 0;
		}

		public void add(MapPack p) {
			if (pages.isEmpty()) pages.add(new Page(this, pages.size() + 1));
			Page page = pages.get(pages.size() - 1);
			if (page.packs.size() == Templates.PAGE_SIZE) {
				page = new Page(this, pages.size() + 1);
				pages.add(page);
			}

			page.add(p);
			this.packs++;
		}
	}

	public class Page {

		public final Gametype gametype;
		public final int number;
		public final Path path;
		public final List<MapPackInfo> packs = new ArrayList<>();

		public Page(Gametype gametype, int number) {
			this.gametype = gametype;
			this.number = number;
			this.path = gametype.path.resolve(Integer.toString(number));
		}

		public void add(MapPack p) {
			this.packs.add(new MapPackInfo(this, p));
			Collections.sort(packs);
		}
	}

	public class MapPackInfo implements Comparable<MapPackInfo> {

		public final Page page;
		public final MapPack pack;
		public final Path path;

		public final Collection<MapPackInfo> variations;
		public final Map<String, Integer> alsoIn;

		public MapPackInfo(Page page, MapPack pack) {
			this.page = page;
			this.pack = pack;
			this.path = pack.slugPath(siteRoot);

			this.alsoIn = new HashMap<>();
			for (Content.ContentFile f : pack.files) {
				Collection<Content> containing = content.containingFile(f.hash);
				if (containing.size() > 1) {
					alsoIn.put(f.hash, containing.size() - 1);
				}
			}

			this.variations = content.variationsOf(pack.hash).stream()
									 .filter(p -> p instanceof MapPack)
									 .map(p -> new MapPackInfo(page, (MapPack)p))
									 .sorted()
									 .collect(Collectors.toList());

			Collections.sort(this.pack.downloads);
			Collections.sort(this.pack.files);
			Collections.sort(this.pack.maps);
		}

		@Override
		public int compareTo(MapPackInfo o) {
			return pack.name.toLowerCase().compareTo(o.pack.name.toLowerCase());
		}
	}

}
