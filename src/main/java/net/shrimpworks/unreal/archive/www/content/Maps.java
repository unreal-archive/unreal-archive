package net.shrimpworks.unreal.archive.www.content;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import net.shrimpworks.unreal.archive.content.ContentManager;
import net.shrimpworks.unreal.archive.content.Games;
import net.shrimpworks.unreal.archive.content.maps.Map;
import net.shrimpworks.unreal.archive.www.SiteFeatures;
import net.shrimpworks.unreal.archive.www.SiteMap;
import net.shrimpworks.unreal.archive.www.Templates;

public class Maps extends GenericContentPage<Map> {

	private static final String SECTION = "Maps";

	public Maps(ContentManager content, Path output, Path staticRoot, SiteFeatures features) {
		super(content, output, output.resolve("maps"), staticRoot, features);
	}

	@Override
	public Set<SiteMap.Page> generate() {
		GameList games = loadContent(Map.class, content);

		Templates.PageSet pages = pageSet("content/maps");

		pages.add("games.ftl", SiteMap.Page.monthly(0.6f), SECTION)
			 .put("games", games)
			 .write(root.resolve("index.html"));

		games.games.entrySet().parallelStream().forEach(g -> {

			java.util.Map<Integer, java.util.Map<Integer, Integer>> timeline = timeline(g.getValue());

			Games game = Games.byName(g.getKey());

			pages.add("gametypes.ftl", SiteMap.Page.monthly(0.62f), String.join(" / ", SECTION, game.bigName))
				 .put("game", g.getValue())
				 .put("timeline", timeline)
				 .write(g.getValue().path.resolve("index.html"));

			g.getValue().groups.entrySet().parallelStream().forEach(gt -> {

				if (gt.getValue().count < Templates.PAGE_SIZE) {
					// we can output all maps on a single page
					List<ContentInfo<Map>> all = gt.getValue().letters.values().stream()
																	  .flatMap(l -> l.pages.stream())
																	  .flatMap(e -> e.items.stream())
																	  .sorted()
																	  .toList();
					pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName, gt.getKey()))
						 .put("gametype", gt.getValue())
						 .put("maps", all)
						 .write(gt.getValue().path.resolve("index.html"));

					// still generate all map pages
					all.parallelStream().forEach(map -> mapPage(pages, map));

					return;
				}

				gt.getValue().letters.entrySet().parallelStream().forEach(l -> {
					l.getValue().pages.parallelStream().forEach(p -> {
						pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName, gt.getKey()))
							 .put("page", p)
							 .write(p.path.resolve("index.html"));

						p.items.parallelStream().forEach(map -> mapPage(pages, map));
					});

					// output first letter/page combo, with appropriate relative links
					pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName, gt.getKey()))
						 .put("page", l.getValue().pages.get(0))
						 .write(l.getValue().path.resolve("index.html"));
				});

				// output first letter/page combo, with appropriate relative links
				pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName, gt.getKey()))
					 .put("page", gt.getValue().letters.firstEntry().getValue().pages.get(0))
					 .write(gt.getValue().path.resolve("index.html"));
			});

			generateTimeline(pages, timeline, g.getValue(), SECTION);
		});

		return pages.pages;
	}

	private void mapPage(Templates.PageSet pages, ContentInfo<Map> map) {
		final Map item = map.item();
		localImages(item, root.resolve(map.path).getParent());

		pages.add("map.ftl", SiteMap.Page.monthly(0.9f, item.firstIndex), String.join(" / ", SECTION,
																					  map.page.letter.group.game.game.bigName,
																					  map.page.letter.group.name,
																					  item.title))
			 .put("map", map)
			 .write(item.pagePath(siteRoot));
		for (ContentInfo<Map> variation : map.variations) {
			this.mapPage(pages, variation);
		}
	}

	@Override
	protected String gameSubGroup(Map item) {
		return item.gametype;
	}

}
