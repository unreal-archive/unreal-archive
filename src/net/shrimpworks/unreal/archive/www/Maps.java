package net.shrimpworks.unreal.archive.www;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
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
			Gametype gametype = g.gametypes.computeIfAbsent(m.gametype, Gametype::new);
			LetterGroup letter = gametype.letters.computeIfAbsent(m.subGrouping(), LetterGroup::new);
			letter.add(m);

			Author a = authors.authors.computeIfAbsent(m.author, Author::new);
			a.maps.add(new MapInfo(m));
		}
	}

	public void generate() {
		try {
			// url structure:
			// landing with games: /maps/index.html
			// page page with gametypes: /maps/game.html
			// gametype page: /maps/game/gametype/a/1.html
			// map page: /maps/game/gametype/a/1/name_hash8.html

			Path mapsPath = output.resolve("maps");
			try (Writer writer = templateOut(mapsPath.resolve("games.html"))) {
				Template tpl = template("maps/games.ftl");
				java.util.Map<String, Object> vars = new HashMap<>();
				vars.put("title", "Maps");
				vars.put("games", games);
				tpl.process(vars, writer);
			}

			for (java.util.Map.Entry<String, Game> g : games.games.entrySet()) {
				try (Writer writer = templateOut(mapsPath.resolve(g.getValue().slug + ".html"))) {
					Template tpl = template("maps/gametypes.ftl");
					java.util.Map<String, Object> vars = new HashMap<>();
					vars.put("title", String.join(" / ", "Maps", g.getKey()));
					vars.put("game", g.getValue());
					tpl.process(vars, writer);
				}

				Path gamePath = mapsPath.resolve(g.getValue().slug);

				for (java.util.Map.Entry<String, Gametype> gt : g.getValue().gametypes.entrySet()) {
					Path gameTypePath = gamePath.resolve(gt.getValue().slug);

					for (java.util.Map.Entry<String, LetterGroup> l : gt.getValue().letters.entrySet()) {
						Path pagePath = gameTypePath.resolve(l.getValue().letter);

						for (Page p : l.getValue().pages) {
							try (Writer writer = templateOut(pagePath.resolve(Integer.toString(p.number) + ".html"))) {

								Template tpl = template("maps/listing.ftl");
								java.util.Map<String, Object> vars = new HashMap<>();
								vars.put("title", String.join(" / ", "Maps", g.getKey(), gt.getKey()));
								vars.put("game", g.getValue());
								vars.put("gametype", gt.getValue());
								vars.put("letter", l.getValue());
								vars.put("page", p);
								tpl.process(vars, writer);
							}
						}
					}

				}
			}

		} catch (TemplateException | IOException e) {
			throw new RuntimeException("Failed to render page", e);
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
		public final java.util.Map<String, Gametype> gametypes = new TreeMap<>();

		public Game(String name) {
			this.name = name;
			this.slug = slug(name);
		}
	}

	public static class Gametype {

		public final String name;
		public final String slug;
		public final java.util.Map<String, LetterGroup> letters = new TreeMap<>();

		public Gametype(String name) {
			this.name = name;
			this.slug = slug(name);
		}
	}

	public static class LetterGroup {

		public final String letter;
		public final List<Page> pages = new ArrayList<>();

		public LetterGroup(String letter) {
			this.letter = letter;
		}

		public void add(Map map) {
			if (pages.isEmpty()) pages.add(new Page(pages.size() + 1));
			Page page = pages.get(pages.size() - 1);
			if (page.maps.size() == PAGE_SIZE) {
				page = new Page(pages.size() + 1);
				pages.add(page);
			}

			page.maps.add(new MapInfo(map));
		}
	}

	public static class Page {

		public final int number;
		public final List<MapInfo> maps = new ArrayList<>();

		public Page(int number) {
			this.number = number;
		}
	}

	public static class MapInfo {

		public final Map map;
		public final String slug;

		public MapInfo(Map map) {
			this.map = map;
			this.slug = slug(map.name + "_" + map.hash.substring(0, 8));
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
