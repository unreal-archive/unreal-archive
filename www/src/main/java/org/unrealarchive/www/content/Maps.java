package org.unrealarchive.www.content;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.unrealarchive.content.Games;
import org.unrealarchive.content.addons.GameType;
import org.unrealarchive.content.addons.GameTypeRepository;
import org.unrealarchive.content.addons.Map;
import org.unrealarchive.content.addons.SimpleAddonRepository;
import org.unrealarchive.www.SiteFeatures;
import org.unrealarchive.www.SiteMap;
import org.unrealarchive.www.Templates;

public class Maps extends GenericContentPage<Map> {

	private static final String SECTION = "Maps";

	private final GameTypeRepository gametypes;
	private final java.util.Map<Integer, GameType> gameTypeCache = new ConcurrentHashMap<>();

	public Maps(SimpleAddonRepository content, Path root, Path staticRoot, SiteFeatures features, GameTypeRepository gametypes) {
		super(content, root, staticRoot, features);
		this.gametypes = gametypes;
	}

	@Override
	public Set<SiteMap.Page> generate() {
		GameList games = loadContent(Map.class, content, "maps");

		Templates.PageSet pages = pageSet("content/maps");

		games.games.entrySet().parallelStream().forEach(g -> {

			java.util.Map<Integer, java.util.Map<Integer, Integer>> timeline = timeline(g.getValue());

			Games game = Games.byName(g.getKey());

			pages.add("gametypes.ftl", SiteMap.Page.monthly(0.62f), String.join(" / ", game.bigName, SECTION))
				 .put("game", g.getValue())
				 .put("timeline", timeline)
				 .write(g.getValue().path.resolve("index.html"));

			g.getValue().groups.entrySet().parallelStream().forEach(gt -> {

				final GameType gtInfo = gameTypeCache.computeIfAbsent(
					Objects.hash(g.getValue().game.name.toLowerCase(), gt.getValue().name.toLowerCase()),
					k -> gametypes.findGametype(g.getValue().game, gt.getValue().name)
				);

				if (gt.getValue().count < Templates.PAGE_SIZE) {
					// we can output all maps on a single page
					List<ContentInfo> all = gt.getValue().letters.values().stream()
																 .flatMap(l -> l.pages.stream())
																 .flatMap(e -> e.items.stream())
																 .sorted()
																 .toList();
					pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", game.bigName, SECTION, gt.getKey()))
						 .put("gametype", gt.getValue())
						 .put("maps", all)
						 .put("gameTypeInfo", gtInfo)
						 .put("gameTypeInfoPath", gtInfo != null ? gtInfo.slugPath(root) : null)
						 .write(gt.getValue().path.resolve("index.html"));

					// still generate all map pages
					all.parallelStream().forEach(map -> mapPage(pages, map));

					return;
				}

				gt.getValue().letters.entrySet().parallelStream().forEach(l -> {
					l.getValue().pages.parallelStream().forEach(p -> {
						pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", game.bigName, SECTION, gt.getKey()))
							 .put("page", p)
							 .put("gameTypeInfo", gtInfo)
							 .put("gameTypeInfoPath", gtInfo != null ? gtInfo.slugPath(root) : null)
							 .write(p.path.resolve("index.html"));

						p.items.parallelStream().forEach(map -> mapPage(pages, map));
					});

					// output first letter/page combo, with appropriate relative links
					pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", game.bigName, SECTION, gt.getKey()))
						 .put("page", l.getValue().pages.getFirst())
						 .put("gameTypeInfo", gtInfo)
						 .put("gameTypeInfoPath", gtInfo != null ? gtInfo.slugPath(root) : null)
						 .write(l.getValue().path.resolve("index.html"));
				});

				// output first letter/page combo, with appropriate relative links
				pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", game.bigName, SECTION, gt.getKey()))
					 .put("page", gt.getValue().letters.firstEntry().getValue().pages.getFirst())
					 .put("gameTypeInfo", gtInfo)
					 .put("gameTypeInfoPath", gtInfo != null ? gtInfo.slugPath(root) : null)
					 .write(gt.getValue().path.resolve("index.html"));
			});

			generateTimeline(pages, timeline, g.getValue(), SECTION);
		});

		return pages.pages;
	}

	private void mapPage(Templates.PageSet pages, ContentInfo map) {
		final Map item = map.item();

		final GameType gtInfo = gameTypeCache.computeIfAbsent(
			Objects.hash(item.game.toLowerCase(), item.gametype.toLowerCase()),
			k -> gametypes.findGametype(Games.byName(item.game), item.gametype)
		);

		localImages(item, root.resolve(map.path).getParent());

		pages.add("map.ftl", SiteMap.Page.monthly(0.9f, item.firstIndex),
				  String.join(" / ", map.page.letter.group.game.game.bigName, SECTION, map.page.letter.group.name, item.title))
			 .put("map", map)
			 .put("gameTypeInfo", gtInfo)
			 .put("gameTypeInfoPath", gtInfo != null ? gtInfo.slugPath(root) : null)
			 .write(item.pagePath(root));
		for (ContentInfo variation : map.variations) {
			this.mapPage(pages, variation);
		}
	}

	@Override
	protected String gameSubGroup(Map item) {
		return item.gametype;
	}

}
