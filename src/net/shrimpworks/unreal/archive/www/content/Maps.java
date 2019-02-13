package net.shrimpworks.unreal.archive.www.content;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.ContentManager;
import net.shrimpworks.unreal.archive.content.maps.Map;
import net.shrimpworks.unreal.archive.www.Templates;

import static net.shrimpworks.unreal.archive.www.Templates.slug;

public class Maps extends ContentPageGenerator {

	private final Games games;
	private final Authors authors;

	public Maps(ContentManager content, Path output, Path staticRoot, boolean localImages) {
		super(content, output.resolve("maps"), staticRoot, localImages);

		this.games = new Games();
		this.authors = new Authors();

		content.get(Map.class).stream()
			   .filter(m -> !m.deleted)
			   .filter(m -> m.variationOf == null || m.variationOf.isEmpty())
			   .sorted()
			   .forEach(m -> {
				   Game g = games.games.computeIfAbsent(m.game, Game::new);
				   g.add(m);

//					Author a = authors.authors.computeIfAbsent(m.author, Author::new);
//					a.maps.add(new MapInfo(null, m)); // FIXME
			   });

	}

	@Override
	public int generate() {
		int count = 0;
		try {
			// url structure:
			// landing with games: /maps/index.html
			// page page with gametypes: /maps/game.html
			// gametype page: /maps/game/gametype/a/1.html
			// map page: /maps/game/gametype/a/1/name_hash8.html

			Templates.template("content/maps/games.ftl")
					 .put("static", root.relativize(staticRoot))
					 .put("title", "Maps")
					 .put("games", games)
					 .put("siteRoot", root)
					 .write(root.resolve("index.html"));
			count++;

			for (java.util.Map.Entry<String, Game> g : games.games.entrySet()) {
				Templates.template("content/maps/gametypes.ftl")
						 .put("static", root.resolve(g.getValue().path).relativize(staticRoot))
						 .put("title", String.join(" / ", "Maps", g.getKey()))
						 .put("game", g.getValue())
						 .put("siteRoot", root.resolve(g.getValue().path).relativize(root))
						 .write(root.resolve(g.getValue().path).resolve("index.html"));
				count++;

				for (java.util.Map.Entry<String, Gametype> gt : g.getValue().gametypes.entrySet()) {

					if (gt.getValue().maps < Templates.PAGE_SIZE) {
						// we can output all maps on a single page
						List<MapInfo> all = gt.getValue().letters.values().stream()
																 .flatMap(l -> l.pages.stream())
																 .flatMap(e -> e.maps.stream())
																 .sorted()
																 .collect(Collectors.toList());
						Templates.template("content/maps/listing_single.ftl")
								 .put("static", root.resolve(gt.getValue().path).relativize(staticRoot))
								 .put("title", String.join(" / ", "Maps", g.getKey(), gt.getKey()))
								 .put("gametype", gt.getValue())
								 .put("maps", all)
								 .put("siteRoot", root.resolve(gt.getValue().path).relativize(root))
								 .write(root.resolve(gt.getValue().path).resolve("index.html"));
						count++;

						// still generate all map pages
						for (MapInfo map : all) {
							mapPage(map);
							count++;
						}

						continue;
					}

					for (java.util.Map.Entry<String, LetterGroup> l : gt.getValue().letters.entrySet()) {

						for (Page p : l.getValue().pages) {
							Templates.template("content/maps/listing.ftl")
									 .put("static", root.resolve(p.path).relativize(staticRoot))
									 .put("title", String.join(" / ", "Maps", g.getKey(), gt.getKey()))
									 .put("page", p)
									 .put("root", p.path)
									 .put("siteRoot", root.resolve(p.path).relativize(root))
									 .write(root.resolve(p.path).resolve("index.html"));
							count++;

							for (MapInfo map : p.maps) {
								mapPage(map);
								count++;
							}
						}

						// output first letter/page combo, with appropriate relative links
						Templates.template("content/maps/listing.ftl")
								 .put("static", root.resolve(l.getValue().path).relativize(staticRoot))
								 .put("title", String.join(" / ", "Maps", g.getKey(), gt.getKey()))
								 .put("page", l.getValue().pages.get(0))
								 .put("root", l.getValue().path)
								 .put("siteRoot", root.resolve(l.getValue().path).relativize(root))
								 .write(root.resolve(l.getValue().path).resolve("index.html"));
						count++;

					}

					// output first letter/page combo, with appropriate relative links
					Templates.template("content/maps/listing.ftl")
							 .put("static", root.resolve(gt.getValue().path).relativize(staticRoot))
							 .put("title", String.join(" / ", "Maps", g.getKey(), gt.getKey()))
							 .put("page", gt.getValue().letters.firstEntry().getValue().pages.get(0))
							 .put("root", gt.getValue().path)
							 .put("siteRoot", root.resolve(gt.getValue().path).relativize(root))
							 .write(root.resolve(gt.getValue().path).resolve("index.html"));
					count++;
				}
			}

		} catch (IOException e) {
			throw new RuntimeException("Failed to render page", e);
		}

		return count;
	}

	private void mapPage(MapInfo map) throws IOException {
		localImages(map.map, root.resolve(map.path).getParent());

		Templates.template("content/maps/map.ftl")
				 .put("static", root.resolve(map.path).getParent().relativize(staticRoot))
				 .put("title", String.join(" / ", "Maps", map.page.letter.gametype.game.name, map.page.letter.gametype.name, map.map.title))
				 .put("map", map)
				 .put("siteRoot", root.resolve(map.path).getParent().relativize(root))
				 .write(root.resolve(map.path + ".html"));

		for (MapInfo variation : map.variations) {
			this.mapPage(variation);
		}
	}

	public class Games {

		public final TreeMap<String, Game> games = new TreeMap<>();
	}

	public class Game {

		public final String name;
		public final String slug;
		public final String path;
		public final TreeMap<String, Gametype> gametypes = new TreeMap<>();
		public int maps;

		public Game(String name) {
			this.name = name;
			this.slug = slug(name);
			this.path = slug;
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
		public final String path;
		public final TreeMap<String, LetterGroup> letters = new TreeMap<>();
		public int maps;

		public Gametype(Game game, String name) {
			this.game = game;
			this.name = name;
			this.slug = slug(name);
			this.path = String.join("/", game.path, slug);
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
		public final String path;
		public final List<Page> pages = new ArrayList<>();
		public int maps;

		public LetterGroup(Gametype gametype, String letter) {
			this.gametype = gametype;
			this.letter = letter;
			this.path = String.join("/", gametype.path, letter);
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
		public final String path;
		public final List<MapInfo> maps = new ArrayList<>();

		public Page(LetterGroup letter, int number) {
			this.letter = letter;
			this.number = number;
			this.path = String.join("/", letter.path, Integer.toString(number));
		}

		public void add(Map map) {
			this.maps.add(new MapInfo(this, map));
			Collections.sort(maps);
		}
	}

	public class MapInfo implements Comparable<MapInfo> {

		public final Page page;
		public final Map map;
		public final String slug;
		public final String path;

		public final Collection<MapInfo> variations;
		public final java.util.Map<String, Integer> alsoIn;

		public MapInfo(Page page, Map map) {
			this.page = page;
			this.map = map;
			this.slug = slug(map.name + "_" + map.hash.substring(0, 8));

			if (page != null) this.path = String.join("/", page.path, slug);
			else this.path = slug;

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

	public class Authors {

		public final TreeMap<String, Author> authors = new TreeMap<>();
	}

	public class Author {

		public final String name;
		public final String slug;
		public final List<MapInfo> maps = new ArrayList<>();

		public Author(String name) {
			this.name = name;
			this.slug = slug(name);
		}
	}

}
