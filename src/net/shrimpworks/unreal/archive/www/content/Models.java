package net.shrimpworks.unreal.archive.www.content;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.ContentManager;
import net.shrimpworks.unreal.archive.content.models.Model;
import net.shrimpworks.unreal.archive.www.SiteMap;
import net.shrimpworks.unreal.archive.www.Templates;

import static net.shrimpworks.unreal.archive.www.Templates.slug;

public class Models extends ContentPageGenerator {

	private final Games games;

	public Models(ContentManager content, Path output, Path staticRoot, boolean localImages) {
		super(content, output.resolve("models"), staticRoot, localImages);

		this.games = new Games();

		content.get(Model.class).stream()
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
		Set<SiteMap.Page> pages = new HashSet<>();
		try {
			pages.add(Templates.template("content/models/games.ftl", SiteMap.Page.monthly(0.6f))
							   .put("static", root.relativize(staticRoot))
							   .put("title", "Models")
							   .put("games", games)
							   .put("siteRoot", root)
							   .write(root.resolve("index.html")));

			for (java.util.Map.Entry<String, Game> g : games.games.entrySet()) {

				var game = net.shrimpworks.unreal.archive.content.Games.byName(g.getKey());

				if (g.getValue().models < Templates.PAGE_SIZE) {
					List<ModelInfo> all = g.getValue().letters.values().stream()
															  .flatMap(l -> l.pages.stream())
															  .flatMap(e -> e.models.stream())
															  .sorted()
															  .collect(Collectors.toList());
					pages.add(Templates.template("content/models/listing_single.ftl", SiteMap.Page.monthly(0.65f))
									   .put("static", root.resolve(g.getValue().path).relativize(staticRoot))
									   .put("title", String.join(" / ", "Models", game.bigName))
									   .put("game", g.getValue())
									   .put("models", all)
									   .put("siteRoot", root.resolve(g.getValue().path).relativize(root))
									   .write(root.resolve(g.getValue().path).resolve("index.html")));

					// still generate all map pages
					for (ModelInfo skin : all) {
						pages.addAll(modelPage(skin));
					}

					continue;
				}

				for (java.util.Map.Entry<String, LetterGroup> l : g.getValue().letters.entrySet()) {

					for (Page p : l.getValue().pages) {
						pages.add(Templates.template("content/models/listing.ftl", SiteMap.Page.weekly(0.65f))
										   .put("static", root.resolve(p.path).relativize(staticRoot))
										   .put("title", String.join(" / ", "Models", game.bigName))
										   .put("page", p)
										   .put("root", p.path)
										   .put("siteRoot", root.resolve(p.path).relativize(root))
										   .write(root.resolve(p.path).resolve("index.html")));

						for (ModelInfo skin : p.models) {
							pages.addAll(modelPage(skin));
						}
					}

					// output first letter/page combo, with appropriate relative links
					pages.add(Templates.template("content/models/listing.ftl", SiteMap.Page.weekly(0.65f))
									   .put("static", root.resolve(l.getValue().path).relativize(staticRoot))
									   .put("title", String.join(" / ", "Models", game.bigName))
									   .put("page", l.getValue().pages.get(0))
									   .put("root", l.getValue().path)
									   .put("siteRoot", root.resolve(l.getValue().path).relativize(root))
									   .write(root.resolve(l.getValue().path).resolve("index.html")));
				}

				// output first letter/page combo, with appropriate relative links
				pages.add(Templates.template("content/models/listing.ftl", SiteMap.Page.weekly(0.65f))
								   .put("static", root.resolve(g.getValue().path).relativize(staticRoot))
								   .put("title", String.join(" / ", "Models", game.bigName))
								   .put("page", g.getValue().letters.firstEntry().getValue().pages.get(0))
								   .put("root", g.getValue().path)
								   .put("siteRoot", root.resolve(g.getValue().path).relativize(root))
								   .write(root.resolve(g.getValue().path).resolve("index.html")));
			}

		} catch (IOException e) {
			throw new RuntimeException("Failed to render page", e);
		}

		return pages;
	}

	private Set<SiteMap.Page> modelPage(ModelInfo model) throws IOException {
		Set<SiteMap.Page> pages = new HashSet<>();
		localImages(model.model, root.resolve(model.path).getParent());

		pages.add(Templates.template("content/models/model.ftl", SiteMap.Page.monthly(0.9f, model.model.lastIndex))
						   .put("static", root.resolve(model.path).getParent().relativize(staticRoot))
						   .put("title", String.join(" / ", "Models", model.page.letter.game.game.bigName, model.model.name))
						   .put("model", model)
						   .put("siteRoot", root.resolve(model.path).getParent().relativize(root))
						   .write(root.resolve(model.path + ".html")));

		// since variations are not top-level things, we need to generate them here
		for (ModelInfo variation : model.variations) {
			pages.addAll(modelPage(variation));
		}

		return pages;
	}

	public class Games {

		public final TreeMap<String, Game> games = new TreeMap<>();
	}

	public class Game {

		public final net.shrimpworks.unreal.archive.content.Games game;
		public final String name;
		public final String slug;
		public final String path;
		public final TreeMap<String, LetterGroup> letters = new TreeMap<>();
		public int models;

		public Game(String name) {
			this.game = net.shrimpworks.unreal.archive.content.Games.byName(name);
			this.name = name;
			this.slug = slug(name);
			this.path = slug;
			this.models = 0;
		}

		public void add(Model model) {
			LetterGroup letter = letters.computeIfAbsent(model.subGrouping(), l -> new LetterGroup(this, l));
			letter.add(model);
			this.models++;
		}
	}

	public class LetterGroup {

		public final Game game;
		public final String letter;
		public final String path;
		public final List<Page> pages = new ArrayList<>();
		public int models;

		public LetterGroup(Game game, String letter) {
			this.game = game;
			this.letter = letter;
			this.path = String.join("/", game.path, letter);
			this.models = 0;
		}

		public void add(Model model) {
			if (pages.isEmpty()) pages.add(new Page(this, pages.size() + 1));
			Page page = pages.get(pages.size() - 1);
			if (page.models.size() == Templates.PAGE_SIZE) {
				page = new Page(this, pages.size() + 1);
				pages.add(page);
			}

			page.add(model);
			this.models++;
		}
	}

	public class Page {

		public final LetterGroup letter;
		public final int number;
		public final String path;
		public final List<ModelInfo> models = new ArrayList<>();

		public Page(LetterGroup letter, int number) {
			this.letter = letter;
			this.number = number;
			this.path = String.join("/", letter.path, Integer.toString(number));
		}

		public void add(Model model) {
			this.models.add(new ModelInfo(this, model));
			Collections.sort(models);
		}
	}

	public class ModelInfo implements Comparable<ModelInfo> {

		public final Page page;
		public final Model model;
		public final String slug;
		public final String path;

		public final Collection<ModelInfo> variations;
		public final java.util.Map<String, Integer> alsoIn;

		public ModelInfo(Page page, Model model) {
			this.page = page;
			this.model = model;
			this.slug = slug(model.name + "_" + model.hash.substring(0, 8));

			if (page != null) this.path = String.join("/", page.path, slug);
			else this.path = slug;

			this.alsoIn = new HashMap<>();
			for (Content.ContentFile f : model.files) {
				Collection<Content> containing = content.containingFile(f.hash);
				if (containing.size() > 1) {
					alsoIn.put(f.hash, containing.size() - 1);
				}
			}

			this.variations = content.variationsOf(model.hash).stream()
									 .filter(p -> p instanceof Model)
									 .map(p -> new ModelInfo(page, (Model)p))
									 .sorted()
									 .collect(Collectors.toList());

			Collections.sort(this.model.downloads);
			Collections.sort(this.model.files);
		}

		@Override
		public int compareTo(ModelInfo o) {
			return model.name.toLowerCase().compareTo(o.model.name.toLowerCase());
		}
	}

}
