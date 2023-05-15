package org.unrealarchive.www.content;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.unrealarchive.content.addons.SimpleAddonRepository;
import org.unrealarchive.content.addons.Addon;
import org.unrealarchive.content.Games;
import org.unrealarchive.content.addons.Skin;
import org.unrealarchive.www.SiteFeatures;
import org.unrealarchive.www.SiteMap;
import org.unrealarchive.www.Templates;

public class Skins extends GenericContentPage<Skin> {

	private static final String SECTION = "Skins";
	private static final String SUBGROUP = "all";

	public Skins(SimpleAddonRepository content, Path output, Path staticRoot, SiteFeatures localImages) {
		super(content, output, output.resolve("skins"), staticRoot, localImages);
	}

	@Override
	public Set<SiteMap.Page> generate() {
		GameList games = loadContent(Skin.class, content);

		Templates.PageSet pages = pageSet("content/skins");

		pages.add("games.ftl", SiteMap.Page.monthly(0.8f), SECTION)
			 .put("games", games)
			 .write(root.resolve("index.html"));

		games.games.entrySet().parallelStream().forEach(g -> {

			Map<Integer, Map<Integer, Integer>> timeline = timeline(g.getValue());

			Games game = Games.byName(g.getKey());

			if (g.getValue().count < Templates.PAGE_SIZE) {
				List<ContentInfo<Skin>> all = g.getValue().groups.get(SUBGROUP).letters.values().stream()
																					   .flatMap(l -> l.pages.stream())
																					   .flatMap(e -> e.items.stream())
																					   .sorted()
																					   .toList();
				pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName))
					 .put("game", g.getValue())
					 .put("timeline", timeline)
					 .put("skins", all)
					 .write(g.getValue().path.resolve("index.html"));

				// still generate all map pages
				all.parallelStream().forEach(skin -> skinPage(pages, skin));

				generateTimeline(pages, timeline, g.getValue(), SECTION);

				return;
			}

			g.getValue().groups.get(SUBGROUP).letters.entrySet().parallelStream().forEach(l -> {
				l.getValue().pages.parallelStream().forEach(p -> {
					pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName))
						 .put("timeline", timeline)
						 .put("page", p)
						 .write(p.path.resolve("index.html"));

					p.items.parallelStream().forEach(skin -> skinPage(pages, skin));
				});

				// output first letter/page combo, with appropriate relative links
				pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName))
					 .put("timeline", timeline)
					 .put("page", l.getValue().pages.get(0))
					 .write(l.getValue().path.resolve("index.html"));
			});

			// output first letter/page combo, with appropriate relative links
			pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName))
				 .put("timeline", timeline)
				 .put("page", g.getValue().groups.get(SUBGROUP).letters.firstEntry().getValue().pages.get(0))
				 .write(g.getValue().path.resolve("index.html"));

			generateTimeline(pages, timeline, g.getValue(), SECTION);
		});

		return pages.pages;
	}

	private void skinPage(Templates.PageSet pages, ContentInfo<Skin> skin) {
		final Addon item = skin.item();
		localImages(item, root.resolve(skin.path).getParent());

		pages.add("skin.ftl", SiteMap.Page.monthly(0.9f, item.firstIndex), String.join(" / ", SECTION,
																					   skin.page.letter.group.game.game.bigName,
																					   item.name))
			 .put("skin", skin)
			 .write(Paths.get(skin.path + ".html"));

		// since variations are not top-level things, we need to generate them here
		for (ContentInfo<Skin> variation : skin.variations) {
			this.skinPage(pages, variation);
		}
	}

	@Override
	protected String gameSubGroup(Skin item) {
		return SUBGROUP;
	}
}
