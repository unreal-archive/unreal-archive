package net.shrimpworks.unreal.archive.www;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.regex.Pattern;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

import net.shrimpworks.unreal.archive.indexer.ContentManager;
import net.shrimpworks.unreal.archive.indexer.maps.Map;

public class Maps {

	private static final Configuration TPL_CONFIG = new Configuration(Configuration.VERSION_2_3_27);
	private static final int PAGE_SIZE = 100;

	private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
	private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

	static {
		TPL_CONFIG.setClassForTemplateLoading(Maps.class, "");
		DefaultObjectWrapper ow = new DefaultObjectWrapper(TPL_CONFIG.getIncompatibleImprovements());
		ow.setExposeFields(true);
		TPL_CONFIG.setObjectWrapper(ow);
	}

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

			try (Writer writer = templateOut(root.resolve("games.html"))) {
				Template tpl = template("maps/games.ftl");
				java.util.Map<String, Object> vars = new HashMap<>();
				vars.put("relUrl", new RelUrlMethod());
				vars.put("title", "Maps");
				vars.put("games", games);
				tpl.process(vars, writer);
			}

			for (java.util.Map.Entry<String, Game> g : games.games.entrySet()) {
				try (Writer writer = templateOut(root.resolve(g.getValue().path).resolve("index.html"))) {
					Template tpl = template("maps/gametypes.ftl");
					java.util.Map<String, Object> vars = new HashMap<>();
					vars.put("relUrl", new RelUrlMethod());
					vars.put("title", String.join(" / ", "Maps", g.getKey()));
					vars.put("game", g.getValue());
					tpl.process(vars, writer);
				}

				for (java.util.Map.Entry<String, Gametype> gt : g.getValue().gametypes.entrySet()) {

					boolean first = true;

					for (java.util.Map.Entry<String, LetterGroup> l : gt.getValue().letters.entrySet()) {

						for (Page p : l.getValue().pages) {
							try (Writer writer = templateOut(root.resolve(p.path).resolve("index.html"))) {
								Template tpl = template("maps/listing.ftl");
								java.util.Map<String, Object> vars = new HashMap<>();
								vars.put("relUrl", new RelUrlMethod());
								vars.put("title", String.join(" / ", "Maps", g.getKey(), gt.getKey()));
								vars.put("game", g.getValue());
								vars.put("gametype", gt.getValue());
								vars.put("letter", l.getValue());
								vars.put("page", p);
								tpl.process(vars, writer);
							}

							if (first) {
								// FIXME urls are obviously broken like this
								Files.copy(root.resolve(p.path).resolve("index.html"),
										   root.resolve(l.getValue().path).resolve("index.html"),
										   StandardCopyOption.REPLACE_EXISTING);
								Files.copy(root.resolve(p.path).resolve("index.html"),
										   root.resolve(gt.getValue().path).resolve("index.html"),
										   StandardCopyOption.REPLACE_EXISTING);
								first = false;
							}

							for (MapInfo map : p.maps) {
								mapPage(root, map);
							}
						}
					}
				}
			}

		} catch (TemplateException | IOException e) {
			throw new RuntimeException("Failed to render page", e);
		}
	}

	private void mapPage(Path root, MapInfo map) throws IOException, TemplateException {
		try (Writer writer = templateOut(root.resolve(map.path + ".html"))) {
			Template tpl = template("maps/map.ftl");
			java.util.Map<String, Object> vars = new HashMap<>();
			vars.put("relUrl", new RelUrlMethod());
			vars.put("title", String.join(" / ", "Maps", map.page.letter.gametype.game.name, map.page.letter.gametype.name, map.map.title));
			vars.put("map", map);
			tpl.process(vars, writer);
		}
	}

	private Template template(String name) throws IOException {
		return TPL_CONFIG.getTemplate(name);
	}

	private Writer templateOut(Path target) throws IOException {
		if (!Files.exists(target.getParent())) Files.createDirectories(target.getParent());
		return new BufferedWriter(new FileWriter(target.toFile()));
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

	public class RelUrlMethod implements TemplateMethodModelEx {

		public Object exec(List args) throws TemplateModelException {
			if (args.size() != 2) {
				throw new TemplateModelException("Wrong arguments");
			}
			return Paths.get(args.get(0).toString()).relativize(Paths.get(args.get(1).toString()));
		}
	}

}
