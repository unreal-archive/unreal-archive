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
import net.shrimpworks.unreal.archive.content.mutators.Mutator;
import net.shrimpworks.unreal.archive.www.SiteFeatures;
import net.shrimpworks.unreal.archive.www.SiteMap;
import net.shrimpworks.unreal.archive.www.Templates;

import static net.shrimpworks.unreal.archive.Util.slug;

public class Mutators extends ContentPageGenerator {

	private static final String SECTION = "Mutators";

	private final Games games;

	public Mutators(ContentManager content, Path output, Path staticRoot, SiteFeatures localImages) {
		super(content, output, output.resolve("mutators"), staticRoot, localImages);

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
		Templates.PageSet pages = pageSet("content/mutators");

		pages.add("games.ftl", SiteMap.Page.monthly(0.6f), SECTION)
			 .put("games", games)
			 .write(root.resolve("index.html"));

		games.games.entrySet().parallelStream().forEach(g -> {

			var game = net.shrimpworks.unreal.archive.content.Games.byName(g.getKey());

			if (g.getValue().mutators < Templates.PAGE_SIZE) {
				List<MutatorInfo> all = g.getValue().letters.values().stream()
															.flatMap(l -> l.pages.stream())
															.flatMap(e -> e.mutators.stream())
															.sorted()
															.collect(Collectors.toList());
				pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName))
					 .put("game", g.getValue())
					 .put("mutators", all)
					 .write(g.getValue().path.resolve("index.html"));

				// still generate all mutator pages
				all.parallelStream().forEach(mutator -> mutatorPage(pages, mutator));

				return;
			}

			g.getValue().letters.entrySet().parallelStream().forEach(l -> {
				l.getValue().pages.parallelStream().forEach(p -> {
					pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName))
						 .put("page", p)
						 .write(p.path.resolve("index.html"));

					p.mutators.parallelStream().forEach(mutator -> mutatorPage(pages, mutator));
				});

				// output first letter/page combo, with appropriate relative links
				pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName))
					 .put("page", l.getValue().pages.get(0))
					 .write(l.getValue().path.resolve("index.html"));
			});

			// output first letter/page combo, with appropriate relative links
			pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName))
				 .put("page", g.getValue().letters.firstEntry().getValue().pages.get(0))
				 .write(g.getValue().path.resolve("index.html"));
		});

		return pages.pages;
	}

	private void mutatorPage(Templates.PageSet pages, MutatorInfo mutator) {
		localImages(mutator.mutator, root.resolve(mutator.path).getParent());

		pages.add("mutator.ftl", SiteMap.Page.monthly(0.9f, mutator.mutator.firstIndex), String.join(" / ", SECTION,
																									 mutator.page.letter.game.game.bigName,
																									 mutator.mutator.name))
			 .put("mutator", mutator)
			 .write(Paths.get(mutator.path.toString() + ".html"));

		// since variations are not top-level things, we need to generate them here
		for (MutatorInfo variation : mutator.variations) {
			mutatorPage(pages, variation);
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
		public int mutators;

		public Game(String name) {
			this.game = net.shrimpworks.unreal.archive.content.Games.byName(name);
			this.name = name;
			this.slug = slug(name);
			this.path = root.resolve(slug);
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
		public final Path path;
		public final List<Page> pages = new ArrayList<>();
		public int mutators;

		public LetterGroup(Game game, String letter) {
			this.game = game;
			this.letter = letter;
			this.path = game.path.resolve(letter);
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
		public final Path path;
		public final List<MutatorInfo> mutators = new ArrayList<>();

		public Page(LetterGroup letter, int number) {
			this.letter = letter;
			this.number = number;
			this.path = letter.path.resolve(Integer.toString(number));
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
		public final Path path;

		public final Collection<MutatorInfo> variations;
		public final java.util.Map<String, Integer> alsoIn;

		public MutatorInfo(Page page, Mutator mutator) {
			this.page = page;
			this.mutator = mutator;
			this.slug = slug(mutator.name + "_" + mutator.hash.substring(0, 8));

			this.path = mutator.slugPath(siteRoot);

			this.alsoIn = new HashMap<>();
			for (Content.ContentFile f : mutator.files) {
				Collection<Content> containing = content.containingFile(f.hash);
				if (containing.size() > 1) {
					alsoIn.put(f.hash, containing.size() - 1);
				}
			}

			this.variations = content.variationsOf(mutator.hash).stream()
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
