package org.unrealarchive.www.content;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.unrealarchive.content.Games;
import org.unrealarchive.content.RepositoryManager;
import org.unrealarchive.content.addons.Addon;
import org.unrealarchive.content.addons.Mutator;
import org.unrealarchive.www.SiteFeatures;
import org.unrealarchive.www.SiteMap;
import org.unrealarchive.www.Templates;

public class Mutators extends GenericContentPage<Mutator> {

	private static final String SECTION = "Mutators";
	private static final String SUBGROUP = "all";

	public Mutators(RepositoryManager repos, Path root, Path staticRoot, SiteFeatures localImages) {
		super(repos, root, staticRoot, localImages);
	}

	@Override
	public Set<SiteMap.Page> generate() {
		GameList games = loadContent(Mutator.class, "mutators");

		Templates.PageSet pages = pageSet("content/mutators");

		games.games.entrySet().parallelStream().forEach(g -> {

			Map<Integer, Map<Integer, Integer>> timeline = timeline(g.getValue());

			Games game = Games.byName(g.getKey());

			if (g.getValue().count < Templates.PAGE_SIZE) {
				List<ContentInfo> all = g.getValue().groups.get(SUBGROUP).letters.values().stream()
																				 .flatMap(l -> l.pages.stream())
																				 .flatMap(e -> e.items.stream())
																				 .sorted()
																				 .toList();
				pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", game.bigName, SECTION))
					 .put("game", g.getValue())
					 .put("timeline", timeline)
					 .put("mutators", all)
					 .write(g.getValue().path.resolve("index.html"));

				// still generate all mutator pages
				all.parallelStream().forEach(mutator -> mutatorPage(pages, mutator));

				generateTimeline(pages, timeline, g.getValue(), SECTION);

				return;
			}

			g.getValue().groups.get(SUBGROUP).letters.entrySet().parallelStream().forEach(l -> {
				l.getValue().pages.parallelStream().forEach(p -> {
					pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", game.bigName, SECTION))
						 .put("timeline", timeline)
						 .put("page", p)
						 .write(p.path.resolve("index.html"));

					p.items.parallelStream().forEach(mutator -> mutatorPage(pages, mutator));
				});

				// output first letter/page combo, with appropriate relative links
				pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", game.bigName, SECTION))
					 .put("timeline", timeline)
					 .put("page", l.getValue().pages.getFirst())
					 .write(l.getValue().path.resolve("index.html"));
			});

			// output first letter/page combo, with appropriate relative links
			pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", game.bigName, SECTION))
				 .put("timeline", timeline)
				 .put("page", g.getValue().groups.get(SUBGROUP).letters.firstEntry().getValue().pages.getFirst())
				 .write(g.getValue().path.resolve("index.html"));

			generateTimeline(pages, timeline, g.getValue(), SECTION);
		});

		return pages.pages;
	}

	private void mutatorPage(Templates.PageSet pages, ContentInfo mutator) {
		final Addon item = mutator.item();

		pages.add("mutator.ftl", SiteMap.Page.monthly(0.9f, item.firstIndex),
				  String.join(" / ", mutator.page.letter.group.game.game.bigName, SECTION, item.name))
			 .put("mutator", mutator)
			 .write(Paths.get(mutator.path + ".html"));

		// since variations are not top-level things, we need to generate them here
		for (ContentInfo variation : mutator.variations) {
			mutatorPage(pages, variation);
		}
	}

	@Override
	protected String gameSubGroup(Mutator item) {
		return SUBGROUP;
	}
}
