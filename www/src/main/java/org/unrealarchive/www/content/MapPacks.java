package org.unrealarchive.www.content;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.unrealarchive.content.Games;
import org.unrealarchive.content.RepositoryManager;
import org.unrealarchive.content.addons.GameType;
import org.unrealarchive.content.addons.MapPack;
import org.unrealarchive.www.SiteFeatures;
import org.unrealarchive.www.SiteMap;
import org.unrealarchive.www.Templates;

public class MapPacks extends GenericContentPage<MapPack> {

	private static final String SECTION = "Map Packs";

	private static final String LETTER_SUBGROUP = "all";

	private final java.util.Map<Integer, GameType> gameTypeCache = new ConcurrentHashMap<>();

	public MapPacks(RepositoryManager repos, Path root, Path staticRoot, SiteFeatures features) {
		super(repos, root, staticRoot, features);
	}

	@Override
	public Set<SiteMap.Page> generate() {
		GameList games = loadContent(MapPack.class, "mappacks");

		Templates.PageSet pages = pageSet("content/mappacks");

		games.games.entrySet().parallelStream().forEach(g -> {

			Map<Integer, Map<Integer, Integer>> timeline = timeline(g.getValue());

			Games game = Games.byName(g.getKey());

			pages.add("gametypes.ftl", SiteMap.Page.monthly(0.62f), String.join(" / ", game.bigName, SECTION))
				 .put("game", g.getValue())
				 .put("timeline", timeline)
				 .write(g.getValue().path.resolve("index.html"));

			g.getValue().groups.entrySet().parallelStream().forEach(gt -> {

				final GameType gtInfo = getGameType(gt.getValue().game.name, gt.getValue().name);

				// skip the letter breakdown
				gt.getValue().letters.get(LETTER_SUBGROUP).pages.parallelStream().forEach(p -> {
					// don't bother creating numbered single page, default landing page will suffice
					if (gt.getValue().letters.get(LETTER_SUBGROUP).pages.size() > 1) {
						pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", game.bigName, SECTION, gt.getKey()))
							 .put("page", p)
							 .put("pages", gt.getValue().letters.get(LETTER_SUBGROUP).pages)
							 .put("gametype", gt.getValue())
							 .put("gameTypeInfo", gtInfo)
							 .put("gameTypeInfoPath", gtInfo != null ? gtInfo.slugPath(root) : null)
							 .write(p.path.resolve("index.html"));
					}

					p.items.parallelStream().forEach(pack -> packPage(pages, pack));
				});

				// output first letter/page combo, with appropriate relative links
				pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", game.bigName, SECTION, gt.getKey()))
					 .put("page", gt.getValue().letters.get(LETTER_SUBGROUP).pages.getFirst())
					 .put("pages", gt.getValue().letters.get(LETTER_SUBGROUP).pages)
					 .put("gametype", gt.getValue())
					 .put("gameTypeInfo", gtInfo)
					 .put("gameTypeInfoPath", gtInfo != null ? gtInfo.slugPath(root) : null)
					 .write(gt.getValue().path.resolve("index.html"));
			});

			generateTimeline(pages, timeline, g.getValue(), SECTION);
		});

		return pages.pages;
	}

	private void packPage(Templates.PageSet pages, ContentInfo pack) {
		final MapPack item = pack.item();

		final GameType gt = getGameType(item.game, item.gametype);

		pages.add("mappack.ftl", SiteMap.Page.monthly(0.9f, item.firstIndex),
				  String.join(" / ", pack.page.letter.group.game.game.bigName, SECTION, pack.page.letter.group.name, item.name))
			 .put("pack", pack)
			 .put("gameTypeInfo", gt)
			 .put("gameTypeInfoPath", gt != null ? gt.slugPath(root) : null)
			 .write(Paths.get(pack.path + ".html"));

		for (ContentInfo variation : pack.variations) {
			packPage(pages, variation);
		}
	}

	private GameType getGameType(String game, String gametype) {
		return gameTypeCache.computeIfAbsent(
			Objects.hash(game.toLowerCase(), gametype.toLowerCase()),
			k -> repos.gameTypes().findGametype(Games.byName(game), gametype)
		);
	}

	@Override
	protected String gameSubGroup(MapPack item) {
		return item.gametype;
	}

	protected String letterSubGroup(MapPack item) {
		return LETTER_SUBGROUP;
	}
}
