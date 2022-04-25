package net.shrimpworks.unreal.archive.www.content;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import net.shrimpworks.unreal.archive.content.ContentManager;
import net.shrimpworks.unreal.archive.content.Games;
import net.shrimpworks.unreal.archive.content.mappacks.MapPack;
import net.shrimpworks.unreal.archive.www.SiteFeatures;
import net.shrimpworks.unreal.archive.www.SiteMap;
import net.shrimpworks.unreal.archive.www.Templates;

public class MapPacks extends GenericContentPage<MapPack> {

	private static final String SECTION = "Map Packs";

	private static final String LETTER_SUBGROUP = "all";

	private final GameList games;

	public MapPacks(ContentManager content, Path output, Path staticRoot, SiteFeatures features) {
		super(content, output, output.resolve("mappacks"), staticRoot, features);

		this.games = new GameList();

		content.get(MapPack.class).stream()
			   .filter(m -> !m.deleted)
			   .filter(m -> m.variationOf == null || m.variationOf.isEmpty())
			   .sorted()
			   .forEach(p -> {
				   Game g = games.games.computeIfAbsent(p.game, Game::new);
				   g.add(p);
			   });
	}

	@Override
	public Set<SiteMap.Page> generate() {
		Templates.PageSet pages = pageSet("content/mappacks");

		pages.add("games.ftl", SiteMap.Page.monthly(0.6f), SECTION)
			 .put("games", games)
			 .write(root.resolve("index.html"));

		games.games.entrySet().parallelStream().forEach(g -> {

			Map<Integer, Map<Integer, Integer>> timeline = timeline(g.getValue());

			Games game = Games.byName(g.getKey());

			pages.add("gametypes.ftl", SiteMap.Page.monthly(0.62f), String.join(" / ", SECTION, game.bigName))
				 .put("game", g.getValue())
				 .put("timeline", timeline)
				 .write(g.getValue().path.resolve("index.html"));

			g.getValue().groups.entrySet().parallelStream().forEach(gt -> {
				// skip the letter breakdown
				gt.getValue().letters.get(LETTER_SUBGROUP).pages.parallelStream().forEach(p -> {
					// don't bother creating numbered single page, default landing page will suffice
					if (gt.getValue().letters.get(LETTER_SUBGROUP).pages.size() > 1) {
						pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName, gt.getKey()))
							 .put("page", p)
							 .put("pages", gt.getValue().letters.get(LETTER_SUBGROUP).pages)
							 .put("gametype", gt.getValue())
							 .write(p.path.resolve("index.html"));
					}

					p.items.parallelStream().forEach(pack -> packPage(pages, pack));
				});

				// output first letter/page combo, with appropriate relative links
				pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName, gt.getKey()))
					 .put("page", gt.getValue().letters.get(LETTER_SUBGROUP).pages.get(0))
					 .put("pages", gt.getValue().letters.get(LETTER_SUBGROUP).pages)
					 .put("gametype", gt.getValue())
					 .write(gt.getValue().path.resolve("index.html"));
			});

			generateTimeline(pages, timeline, g.getValue(), SECTION);
		});

		return pages.pages;
	}

	private void packPage(Templates.PageSet pages, ContentInfo<MapPack> pack) {
		localImages(pack.item, pack.path.getParent());

		pages.add("mappack.ftl", SiteMap.Page.monthly(0.9f, pack.item.firstIndex), String.join(" / ", SECTION,
																							   pack.page.letter.group.game.game.bigName,
																							   pack.page.letter.group.name, pack.item.name))
			 .put("pack", pack)
			 .put("gametype", pack.page.letter.group)
			 .write(Paths.get(pack.path.toString() + ".html"));

		for (ContentInfo<MapPack> variation : pack.variations) {
			packPage(pages, variation);
		}
	}

	@Override
	String gameSubGroup(MapPack item) {
		return item.gametype;
	}

	String letterSubGroup(MapPack item) {
		return LETTER_SUBGROUP;
	}
}
