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
import net.shrimpworks.unreal.archive.content.mutators.Mutator;
import net.shrimpworks.unreal.archive.www.SiteMap;
import net.shrimpworks.unreal.archive.www.Templates;

import static net.shrimpworks.unreal.archive.Util.slug;

public class Mutators extends ContentPageGenerator {

	private final Games games;

	public Mutators(ContentManager content, Path output, Path staticRoot, boolean localImages) {
		super(content, output.resolve("mutators"), staticRoot, localImages);

		this.games = new Games();

		content.get(Mutator.class).stream()
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
			pages.add(Templates.template("content/mutators/games.ftl", SiteMap.Page.monthly(0.6f))
							   .put("static", root.relativize(staticRoot))
							   .put("title", "Mutators")
							   .put("games", games)
							   .put("siteRoot", root)
							   .write(root.resolve("index.html")));

			for (java.util.Map.Entry<String, Game> g : games.games.entrySet()) {

				var game = net.shrimpworks.unreal.archive.content.Games.byName(g.getKey());

				if (g.getValue().mutators < Templates.PAGE_SIZE) {
					List<MutatorInfo> all = g.getValue().letters.values().stream()
																.flatMap(l -> l.pages.stream())
																.flatMap(e -> e.mutators.stream())
																.sorted()
																.collect(Collectors.toList());
					pages.add(Templates.template("content/mutators/listing_single.ftl", SiteMap.Page.weekly(0.65f))
									   .put("static", root.resolve(g.getValue().path).relativize(staticRoot))
									   .put("title", String.join(" / ", "Mutators", game.bigName))
									   .put("game", g.getValue())
									   .put("mutators", all)
									   .put("siteRoot", root.resolve(g.getValue().path).relativize(root))
									   .write(root.resolve(g.getValue().path).resolve("index.html")));

					// still generate all mutator pages
					for (MutatorInfo skin : all) {
						pages.addAll(skinPage(skin));
					}

					continue;
				}

				for (java.util.Map.Entry<String, LetterGroup> l : g.getValue().letters.entrySet()) {

					for (Page p : l.getValue().pages) {
						pages.add(Templates.template("content/mutators/listing.ftl", SiteMap.Page.weekly(0.65f))
										   .put("static", root.resolve(p.path).relativize(staticRoot))
										   .put("title", String.join(" / ", "Mutators", game.bigName))
										   .put("page", p)
										   .put("root", p.path)
										   .put("siteRoot", root.resolve(p.path).relativize(root))
										   .write(root.resolve(p.path).resolve("index.html")));

						for (MutatorInfo skin : p.mutators) {
							pages.addAll(skinPage(skin));
						}
					}

					// output first letter/page combo, with appropriate relative links
					pages.add(Templates.template("content/mutators/listing.ftl", SiteMap.Page.weekly(0.65f))
									   .put("static", root.resolve(l.getValue().path).relativize(staticRoot))
									   .put("title", String.join(" / ", "Mutators", game.bigName))
									   .put("page", l.getValue().pages.get(0))
									   .put("root", l.getValue().path)
									   .put("siteRoot", root.resolve(l.getValue().path).relativize(root))
									   .write(root.resolve(l.getValue().path).resolve("index.html")));
				}

				// output first letter/page combo, with appropriate relative links
				pages.add(Templates.template("content/mutators/listing.ftl", SiteMap.Page.weekly(0.65f))
								   .put("static", root.resolve(g.getValue().path).relativize(staticRoot))
								   .put("title", String.join(" / ", "Mutators", game.bigName))
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

	private Set<SiteMap.Page> skinPage(MutatorInfo mutator) throws IOException {
		Set<SiteMap.Page> pages = new HashSet<>();
		localImages(mutator.mutator, root.resolve(mutator.path).getParent());

		pages.add(Templates.template("content/mutators/mutator.ftl", SiteMap.Page.monthly(0.9f, mutator.mutator.lastIndex))
						   .put("static", root.resolve(mutator.path).getParent().relativize(staticRoot))
						   .put("title", String.join(" / ", "Mutator", mutator.page.letter.game.game.bigName, mutator.mutator.name))
						   .put("mutator", mutator)
						   .put("siteRoot", root.resolve(mutator.path).getParent().relativize(root))
						   .write(root.resolve(mutator.path + ".html")));

		// since variations are not top-level things, we need to generate them here
		for (MutatorInfo variation : mutator.variations) {
			pages.addAll(skinPage(variation));
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
		public int mutators;

		public Game(String name) {
			this.game = net.shrimpworks.unreal.archive.content.Games.byName(name);
			this.name = name;
			this.slug = slug(name);
			this.path = slug;
			this.mutators = 0;
		}

		public void add(Mutator s) {
			LetterGroup letter = letters.computeIfAbsent(s.subGrouping(), l -> new LetterGroup(this, l));
			letter.add(s);
			this.mutators++;
		}
	}

	public class LetterGroup {

		public final Game game;
		public final String letter;
		public final String path;
		public final List<Page> pages = new ArrayList<>();
		public int mutators;

		public LetterGroup(Game game, String letter) {
			this.game = game;
			this.letter = letter;
			this.path = String.join("/", game.path, letter);
			this.mutators = 0;
		}

		public void add(Mutator s) {
			if (pages.isEmpty()) pages.add(new Page(this, pages.size() + 1));
			Page page = pages.get(pages.size() - 1);
			if (page.mutators.size() == Templates.PAGE_SIZE) {
				page = new Page(this, pages.size() + 1);
				pages.add(page);
			}

			page.add(s);
			this.mutators++;
		}
	}

	public class Page {

		public final LetterGroup letter;
		public final int number;
		public final String path;
		public final List<MutatorInfo> mutators = new ArrayList<>();

		public Page(LetterGroup letter, int number) {
			this.letter = letter;
			this.number = number;
			this.path = String.join("/", letter.path, Integer.toString(number));
		}

		public void add(Mutator s) {
			this.mutators.add(new MutatorInfo(this, s));
			Collections.sort(mutators);
		}
	}

	public class MutatorInfo implements Comparable<MutatorInfo> {

		public final Page page;
		public final Mutator mutator;
		public final String slug;
		public final String path;

		public final Collection<MutatorInfo> variations;
		public final java.util.Map<String, Integer> alsoIn;

		public MutatorInfo(Page page, Mutator s) {
			this.page = page;
			this.mutator = s;
			this.slug = slug(s.name + "_" + s.hash.substring(0, 8));

			if (page != null) this.path = String.join("/", page.path, slug);
			else this.path = slug;

			this.alsoIn = new HashMap<>();
			for (Content.ContentFile f : s.files) {
				Collection<Content> containing = content.containingFile(f.hash);
				if (containing.size() > 1) {
					alsoIn.put(f.hash, containing.size() - 1);
				}
			}

			this.variations = content.variationsOf(s.hash).stream()
									 .filter(p -> p instanceof Mutator)
									 .map(p -> new MutatorInfo(page, (Mutator)p))
									 .sorted()
									 .collect(Collectors.toList());

			Collections.sort(this.mutator.downloads);
			Collections.sort(this.mutator.files);
		}

		@Override
		public int compareTo(MutatorInfo o) {
			return mutator.name.toLowerCase().compareTo(o.mutator.name.toLowerCase());
		}
	}

}
