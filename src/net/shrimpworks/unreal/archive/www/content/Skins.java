package net.shrimpworks.unreal.archive.www.content;

import java.io.IOException;
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
import net.shrimpworks.unreal.archive.content.skins.Skin;
import net.shrimpworks.unreal.archive.www.SiteMap;
import net.shrimpworks.unreal.archive.www.Templates;

import static net.shrimpworks.unreal.archive.Util.slug;

public class Skins extends ContentPageGenerator {

	private static final String SECTION = "Skins";

	private final Games games;
	private final Path siteRoot;

	public Skins(ContentManager content, Path output, Path staticRoot, boolean localImages) {
		super(content, output.resolve("skins"), staticRoot, localImages);
		this.siteRoot = output;

		this.games = new Games();

		content.get(Skin.class).stream()
			   .filter(s -> !s.deleted)
			   .filter(s -> s.variationOf == null || s.variationOf.isEmpty())
			   .sorted()
			   .forEach(s -> {
				   Game g = games.games.computeIfAbsent(s.game, Game::new);
				   g.add(s);
			   });

	}

	@Override
	public Set<SiteMap.Page> generate() {
		Templates.PageSet pages = new Templates.PageSet("content/skins", siteRoot, staticRoot, root);
		try {
			pages.add("games.ftl", SiteMap.Page.monthly(0.8f), SECTION)
				 .put("games", games)
				 .write(root.resolve("index.html"));

			for (java.util.Map.Entry<String, Game> g : games.games.entrySet()) {

				var game = net.shrimpworks.unreal.archive.content.Games.byName(g.getKey());

				if (g.getValue().skins < Templates.PAGE_SIZE) {
					List<SkinInfo> all = g.getValue().letters.values().stream()
															 .flatMap(l -> l.pages.stream())
															 .flatMap(e -> e.skins.stream())
															 .sorted()
															 .collect(Collectors.toList());
					pages.add("listing_single.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", "Skins", game.bigName))
						 .put("game", g.getValue())
						 .put("skins", all)
						 .write(g.getValue().path.resolve("index.html"));

					// still generate all map pages
					for (SkinInfo skin : all) {
						skinPage(pages, skin);
					}

					continue;
				}

				for (java.util.Map.Entry<String, LetterGroup> l : g.getValue().letters.entrySet()) {

					for (Page p : l.getValue().pages) {
						pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", "Skins", game.bigName))
							 .put("page", p)
							 .write(p.path.resolve("index.html"));

						for (SkinInfo skin : p.skins) {
							skinPage(pages, skin);
						}
					}

					// output first letter/page combo, with appropriate relative links
					pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", "Skins", game.bigName))
						 .put("page", l.getValue().pages.get(0))
						 .write(l.getValue().path.resolve("index.html"));
				}

				// output first letter/page combo, with appropriate relative links
				pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", "Skins", game.bigName))
					 .put("page", g.getValue().letters.firstEntry().getValue().pages.get(0))
					 .write(g.getValue().path.resolve("index.html"));
			}

		} catch (IOException e) {
			throw new RuntimeException("Failed to render page", e);
		}

		return pages.pages;
	}

	private void skinPage(Templates.PageSet pages, SkinInfo skin) throws IOException {
		localImages(skin.skin, root.resolve(skin.path).getParent());

		pages.add("skin.ftl", SiteMap.Page.monthly(0.9f, skin.skin.lastIndex), String.join(" / ", "Skins",
																						   skin.page.letter.game.game.name,
																						   skin.skin.name))
			 .put("skin", skin)
			 .write(Paths.get(skin.path.toString() + ".html"));

		// since variations are not top-level things, we need to generate them here
		for (SkinInfo variation : skin.variations) {
			this.skinPage(pages, variation);
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
		public final TreeMap<String, LetterGroup> letters = new TreeMap<>();
		public int skins;

		public Game(String name) {
			this.game = net.shrimpworks.unreal.archive.content.Games.byName(name);
			this.name = name;
			this.slug = slug(name);
			this.path = root.resolve(slug);
			this.skins = 0;
		}

		public void add(Skin s) {
			LetterGroup letter = letters.computeIfAbsent(s.subGrouping(), l -> new LetterGroup(this, l));
			letter.add(s);
			this.skins++;
		}
	}

	public class LetterGroup {

		public final Game game;
		public final String letter;
		public final Path path;
		public final List<Page> pages = new ArrayList<>();
		public int skins;

		public LetterGroup(Game game, String letter) {
			this.game = game;
			this.letter = letter;
			this.path = game.path.resolve(letter);
			this.skins = 0;
		}

		public void add(Skin s) {
			if (pages.isEmpty()) pages.add(new Page(this, pages.size() + 1));
			Page page = pages.get(pages.size() - 1);
			if (page.skins.size() == Templates.PAGE_SIZE) {
				page = new Page(this, pages.size() + 1);
				pages.add(page);
			}

			page.add(s);
			this.skins++;
		}
	}

	public class Page {

		public final LetterGroup letter;
		public final int number;
		public final Path path;
		public final List<SkinInfo> skins = new ArrayList<>();

		public Page(LetterGroup letter, int number) {
			this.letter = letter;
			this.number = number;
			this.path = letter.path.resolve(Integer.toString(number));
		}

		public void add(Skin s) {
			this.skins.add(new SkinInfo(this, s));
			Collections.sort(skins);
		}
	}

	public class SkinInfo implements Comparable<SkinInfo> {

		public final Page page;
		public final Skin skin;
		public final String slug;
		public final Path path;

		public final Collection<SkinInfo> variations;
		public final java.util.Map<String, Integer> alsoIn;

		public SkinInfo(Page page, Skin s) {
			this.page = page;
			this.skin = s;
			this.slug = slug(s.name + "_" + s.hash.substring(0, 8));

			this.path = s.slugPath(siteRoot);

			this.alsoIn = new HashMap<>();
			for (Content.ContentFile f : s.files) {
				Collection<Content> containing = content.containingFile(f.hash);
				if (containing.size() > 1) {
					alsoIn.put(f.hash, containing.size() - 1);
				}
			}

			this.variations = content.variationsOf(s.hash).stream()
									 .filter(p -> p instanceof Skin)
									 .map(p -> new SkinInfo(page, (Skin)p))
									 .sorted()
									 .collect(Collectors.toList());

			Collections.sort(this.skin.downloads);
			Collections.sort(this.skin.files);
		}

		@Override
		public int compareTo(SkinInfo o) {
			return skin.name.toLowerCase().compareTo(o.skin.name.toLowerCase());
		}
	}

}
