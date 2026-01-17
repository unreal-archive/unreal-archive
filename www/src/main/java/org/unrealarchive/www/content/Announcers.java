package org.unrealarchive.www.content;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.unrealarchive.content.Games;
import org.unrealarchive.content.RepositoryManager;
import org.unrealarchive.content.addons.Addon;
import org.unrealarchive.content.addons.Announcer;
import org.unrealarchive.www.SiteFeatures;
import org.unrealarchive.www.SiteMap;
import org.unrealarchive.www.Templates;

public class Announcers extends GenericContentPage<Announcer> {

	private static final String SECTION = "Announcers";
	private static final String SUBGROUP = "all";

	public Announcers(RepositoryManager repos, Path root, Path staticRoot, SiteFeatures localImages) {
		super(repos, root, staticRoot, localImages);
	}

	@Override
	public Set<SiteMap.Page> generate() {
		GameList games = loadContent(Announcer.class, "announcers");

		Templates.PageSet pages = pageSet("content/announcers");

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
					 .put("announcers", all)
					 .write(g.getValue().path.resolve("index.html"));

				// still generate all map pages
				all.parallelStream().forEach(announcer -> announcerPage(pages, announcer));

				generateTimeline(pages, timeline, g.getValue(), SECTION);

				return;
			}

			g.getValue().groups.get(SUBGROUP).letters.entrySet().parallelStream().forEach(l -> {
				l.getValue().pages.parallelStream().forEach(p -> {
					pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", game.bigName, SECTION))
						 .put("timeline", timeline)
						 .put("page", p)
						 .write(p.path.resolve("index.html"));

					p.items.parallelStream().forEach(announcer -> announcerPage(pages, announcer));
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

	private void announcerPage(Templates.PageSet pages, ContentInfo announcer) {
		final Addon item = announcer.item();

		pages.add("announcer.ftl", SiteMap.Page.monthly(0.9f, item.firstIndex),
				  String.join(" / ", announcer.page.letter.group.game.game.bigName, SECTION, item.name))
			 .put("announcer", announcer)
			 .write(Paths.get(announcer.path + ".html"));

		// since variations are not top-level things, we need to generate them here
		for (ContentInfo variation : announcer.variations) {
			announcerPage(pages, variation);
		}
	}

	@Override
	protected String gameSubGroup(Announcer item) {
		return SUBGROUP;
	}

}
