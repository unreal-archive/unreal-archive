package net.shrimpworks.unreal.archive.www;

import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.regex.Pattern;

import net.shrimpworks.unreal.archive.indexer.ContentManager;
import net.shrimpworks.unreal.archive.indexer.maps.Map;

public class Maps {

	private static final int PAGE_SIZE = 100;

	private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
	private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

	private final ContentManager content;
	private final Path output;

	private final Games games;
	private final Authors authors;

	public Maps(ContentManager content, Path output) {
		this.content = content;
		this.output = output;
		this.games = new Games();
		this.authors = new Authors();

		Collection<Map> maps = content.get(Map.class);
		for (Map m : maps) {
			Game g = games.games.computeIfAbsent(m.game, Game::new);
			g.add(m);

//			Author a = authors.authors.computeIfAbsent(m.author, Author::new);
//			a.maps.add(new MapInfo(null, m)); // FIXME
		}
	}

	public void generate() {
		try {
			// url structure:
			// landing with games: /maps/index.html
			// page page with gametypes: /maps/game.html
			// gametype page: /maps/game/gametype/a/1.html
			// map page: /maps/game/gametype/a/1/name_hash8.html

			Path root = output.resolve("maps");

			Templates.template("maps/games.ftl")
					 .put("title", "Maps")
					 .put("games", games)
					 .write(root.resolve("games.html"));

			for (java.util.Map.Entry<String, Game> g : games.games.entrySet()) {
				Templates.template("maps/gametypes.ftl")
						 .put("title", String.join(" / ", "Maps", g.getKey()))
						 .put("game", g.getValue())
						 .write(root.resolve(g.getValue().path).resolve("index.html"));

				for (java.util.Map.Entry<String, Gametype> gt : g.getValue().gametypes.entrySet()) {

					boolean first = true;

					for (java.util.Map.Entry<String, LetterGroup> l : gt.getValue().letters.entrySet()) {

						for (Page p : l.getValue().pages) {
							Templates.Tpl tpl = Templates.template("maps/listing.ftl")
														 .put("title", String.join(" / ", "Maps", g.getKey(), gt.getKey()))
														 .put("page", p)
														 .write(root.resolve(p.path).resolve("index.html"));

							if (first) {
								// FIXME urls are obviously broken like this
								tpl.write(root.resolve(l.getValue().path).resolve("index.html"));
								tpl.write(root.resolve(gt.getValue().path).resolve("index.html"));
								first = false;
							}

							for (MapInfo map : p.maps) {
								mapPage(root, map);
							}
						}
					}
				}
			}

		} catch (IOException e) {
			throw new RuntimeException("Failed to render page", e);
		}
	}

	private void mapPage(Path root, MapInfo map) throws IOException {
		Templates.template("maps/map.ftl")
				 .put("title", String.join(" / ", "Maps", map.page.letter.gametype.game.name, map.page.letter.gametype.name, map.map.title))
				 .put("map", map)
				 .write(root.resolve(map.path + ".html"));
	}

	public static String slug(String input) {
		String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
		String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
		String slug = NONLATIN.matcher(normalized).replaceAll("");
		return slug.toLowerCase(Locale.ENGLISH);
	}

	public static class Games {

		public final java.util.Map<String, Game> games = new TreeMap<>();
	}

	public static class Game {

		public final String name;
		public final String slug;
		public final String path;
		public final java.util.Map<String, Gametype> gametypes = new TreeMap<>();
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

	public static class Gametype {

		public final Game game;

		public final String name;
		public final String slug;
		public final String path;
		public final java.util.Map<String, LetterGroup> letters = new TreeMap<>();
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

	public static class LetterGroup {

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
			if (page.maps.size() == PAGE_SIZE) {
				page = new Page(this, pages.size() + 1);
				pages.add(page);
			}

			page.add(map);
			this.maps++;
		}
	}

	public static class Page {

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
		}
	}

	public static class MapInfo {

		public final Page page;
		public final Map map;
		public final String slug;
		public final String path;

		public MapInfo(Page page, Map map) {
			this.page = page;
			this.map = map;
			this.slug = slug(map.name + "_" + map.hash.substring(0, 8));

			if (page != null) this.path = String.join("/", page.path, slug);
			else this.path = slug;
		}
	}

	public static class Authors {

		public final java.util.Map<String, Author> authors = new TreeMap<>();
	}

	public static class Author {

		public final String name;
		public final String slug;
		public final List<MapInfo> maps = new ArrayList<>();

		public Author(String name) {
			this.name = name;
			this.slug = slug(name);
		}
	}

}
