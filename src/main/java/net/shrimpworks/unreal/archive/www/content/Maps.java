package net.shrimpworks.unreal.archive.www.content;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.ContentManager;
import net.shrimpworks.unreal.archive.content.maps.Map;
import net.shrimpworks.unreal.archive.www.SiteFeatures;
import net.shrimpworks.unreal.archive.www.SiteMap;
import net.shrimpworks.unreal.archive.www.Templates;

import static net.shrimpworks.unreal.archive.Util.slug;

public class Maps extends ContentPageGenerator {

	private static final String SECTION = "Maps";

	private final Games games;

	public Maps(ContentManager content, Path output, Path staticRoot, SiteFeatures features) {
		super(content, output, output.resolve("maps"), staticRoot, features);

		this.games = new Games();

		content.get(Map.class).stream()
			   .filter(m -> !m.deleted)
			   .filter(m -> m.variationOf == null || m.variationOf.isEmpty())
			   .sorted()
			   .forEach(m -> {
				   Game g = games.games.computeIfAbsent(m.game, Game::new);
				   g.add(m);
			   });

	}

	@Override
	public Set<SiteMap.Page> generate() {
		Templates.PageSet pages = pageSet("content/maps");

		pages.add("games.ftl", SiteMap.Page.monthly(0.6f), SECTION)
			 .put("games", games)
			 .write(root.resolve("index.html"));

		games.games.entrySet().parallelStream().forEach(g -> {

			var game = net.shrimpworks.unreal.archive.content.Games.byName(g.getKey());

			pages.add("gametypes.ftl", SiteMap.Page.monthly(0.62f), String.join(" / ", SECTION, game.bigName))
				 .put("game", g.getValue())
				 .write(g.getValue().path.resolve("index.html"));

			g.getValue().gametypes.entrySet().parallelStream().forEach(gt -> {

				if (gt.getValue().maps < Templates.PAGE_SIZE) {
					// we can output all maps on a single page
					List<MapInfo> all = gt.getValue().letters.values().stream()
															 .flatMap(l -> l.pages.stream())
															 .flatMap(e -> e.maps.stream())
															 .sorted()
															 .collect(Collectors.toList());
					pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName, gt.getKey()))
						 .put("gametype", gt.getValue())
						 .put("maps", all)
						 .write(gt.getValue().path.resolve("index.html"));

					// still generate all map pages
					all.parallelStream().forEach(map -> mapPage(pages, map));

					return;
				}

				gt.getValue().letters.entrySet().parallelStream().forEach(l -> {
					l.getValue().pages.parallelStream().forEach(p -> {
						pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName, gt.getKey()))
							 .put("page", p)
							 .write(p.path.resolve("index.html"));

						p.maps.parallelStream().forEach(map -> mapPage(pages, map));
					});

					// output first letter/page combo, with appropriate relative links
					pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName, gt.getKey()))
						 .put("page", l.getValue().pages.get(0))
						 .write(l.getValue().path.resolve("index.html"));
				});

				// output first letter/page combo, with appropriate relative links
				pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName, gt.getKey()))
					 .put("page", gt.getValue().letters.firstEntry().getValue().pages.get(0))
					 .write(gt.getValue().path.resolve("index.html"));
			});
		});

		return pages.pages;
	}

	private void mapPage(Templates.PageSet pages, MapInfo map) {
		localImages(map.map, root.resolve(map.path).getParent());

		pages.add("map.ftl", SiteMap.Page.monthly(0.9f, map.map.firstIndex), String.join(" / ", SECTION,
																						 map.page.letter.gametype.game.game.bigName,
																						 map.page.letter.gametype.name,
																						 map.map.title))
			 .put("map", map)
			 .write(Paths.get(map.path.toString() + ".html"));

		for (MapInfo variation : map.variations) {
			this.mapPage(pages, variation);
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
		public int maps;

		public Game(String name) {
			this.game = net.shrimpworks.unreal.archive.content.Games.byName(name);
			this.name = name;
			this.slug = slug(name);
			this.path = root.resolve(slug);
			this.maps = 0;
		}

		public void add(Map m) {
			Gametype gametype = gametypes.computeIfAbsent(m.gametype, g -> new Gametype(this, g));
			gametype.add(m);
			this.maps++;
		}
	}

	public class Gametype {

		public final Game game;

		public final String name;
		public final String slug;
		public final Path path;
		public final TreeMap<String, LetterGroup> letters = new TreeMap<>();
		public int maps;

		public Gametype(Game game, String name) {
			this.game = game;
			this.name = name;
			this.slug = slug(name);
			this.path = game.path.resolve(slug);
			this.maps = 0;
		}

		public void add(Map m) {
			LetterGroup letter = letters.computeIfAbsent(m.subGrouping(), l -> new LetterGroup(this, l));
			letter.add(m);
			this.maps++;
		}
	}

	public class LetterGroup {

		public final Gametype gametype;
		public final String letter;
		public final Path path;
		public final List<Page> pages = new ArrayList<>();
		public int maps;

		public LetterGroup(Gametype gametype, String letter) {
			this.gametype = gametype;
			this.letter = letter;
			this.path = gametype.path.resolve(letter);
			this.maps = 0;
		}

		public void add(Map map) {
			if (pages.isEmpty()) pages.add(new Page(this, pages.size() + 1));
			Page page = pages.get(pages.size() - 1);
			if (page.maps.size() == Templates.PAGE_SIZE) {
				page = new Page(this, pages.size() + 1);
				pages.add(page);
			}

			page.add(map);
			this.maps++;
		}
	}

	public class Page {

		public final LetterGroup letter;
		public final int number;
		public final Path path;
		public final List<MapInfo> maps = new ArrayList<>();

		public Page(LetterGroup letter, int number) {
			this.letter = letter;
			this.number = number;
			this.path = letter.path.resolve(Integer.toString(number));
		}

		public void add(Map map) {
			this.maps.add(new MapInfo(this, map));
			Collections.sort(maps);
		}
	}

	public class MapInfo implements Comparable<MapInfo> {

		public final Page page;
		public final Map map;
		public final Path path;

		public final Collection<MapInfo> variations;
		public final java.util.Map<String, Integer> alsoIn;

		public MapInfo(Page page, Map map) {
			this.page = page;
			this.map = map;
			this.path = map.slugPath(siteRoot);

			this.alsoIn = new HashMap<>();
			for (Content.ContentFile f : map.files) {
				Collection<Content> containing = content.containingFile(f.hash);
				if (containing.size() > 1) {
					alsoIn.put(f.hash, containing.size() - 1);
				}
			}

			this.variations = content.variationsOf(map.hash).stream()
									 .filter(p -> p instanceof Map)
									 .map(p -> new MapInfo(page, (Map)p))
									 .sorted()
									 .collect(Collectors.toList());

			Collections.sort(this.map.downloads);
			Collections.sort(this.map.files);
		}

		@Override
		public int compareTo(MapInfo o) {
			return map.name.toLowerCase().compareTo(o.map.name.toLowerCase());
		}
	}

}
