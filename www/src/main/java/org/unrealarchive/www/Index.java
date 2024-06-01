package org.unrealarchive.www;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.unrealarchive.common.Util;
import org.unrealarchive.content.Games;
import org.unrealarchive.content.addons.GameType;
import org.unrealarchive.content.addons.GameTypeRepository;
import org.unrealarchive.content.addons.SimpleAddonRepository;
import org.unrealarchive.content.docs.DocumentRepository;
import org.unrealarchive.content.managed.Managed;
import org.unrealarchive.content.managed.ManagedContentRepository;

public class Index implements PageGenerator {

	private final DocumentRepository documents;
	private final ManagedContentRepository managed;
	private final SimpleAddonRepository content;
	private final GameTypeRepository gametypes;
	private final Path root;
	private final Path staticRoot;
	private final SiteFeatures features;

	public Index(SimpleAddonRepository content, GameTypeRepository gametypes, DocumentRepository documents,
				 ManagedContentRepository managed, Path output, Path staticRoot, SiteFeatures features) {
		this.content = content;
		this.gametypes = gametypes;
		this.documents = documents;
		this.managed = managed;

		this.root = output;
		this.staticRoot = staticRoot;
		this.features = features;
	}

	@Override
	public Set<SiteMap.Page> generate() {
		Map<String, Long> contentCount = new HashMap<>();

		Map<Games, Long> games = new LinkedHashMap<>();

		Map<String, Long> contentByGame = content.countByGame();
		Map<String, Long> gametypesByGame = gametypes.all().stream().collect(Collectors.groupingBy(GameType::game, Collectors.counting()));
		Map<String, Long> managedByGame = managed.all().stream().collect(Collectors.groupingBy(Managed::game, Collectors.counting()));
		Map<String, Long> docsByGame = documents.all().stream().collect(Collectors.groupingBy(d -> d.game, Collectors.counting()));

		Arrays.stream(Games.values()).forEach(g -> {
			long c = contentByGame.getOrDefault(g.name, 0L)
					 + gametypesByGame.getOrDefault(g.name, 0L)
					 + managedByGame.getOrDefault(g.name, 0L)
					 + docsByGame.getOrDefault(g.name, 0L);

			if (c > 0) games.put(g, c);
		});

		Templates.PageSet pages = new Templates.PageSet("", features, root, staticRoot, root);

		pages.add("index.ftl", SiteMap.Page.of(1f, SiteMap.ChangeFrequency.weekly), "Home")
			 .put("games", games)
			 .put("count", contentCount)
			 .write(root.resolve("index.html"));

		// generate game landing pages
		games.keySet().forEach(g -> generateGame(pages, g));

		pages.add("misc/index.ftl", SiteMap.Page.of(1f, SiteMap.ChangeFrequency.monthly), "Miscellaneous")
			 .write(root.resolve("misc").resolve("index.html"));

		pages.add("404.ftl", SiteMap.Page.of(0f, SiteMap.ChangeFrequency.never), "Not Found")
			 .write(root.resolve("404.html"));

		return pages.pages;
	}

	private void generateGame(Templates.PageSet pages, Games game) {
		Map<String, Long> contentCount = new HashMap<>();

		content.countByType(game.name).forEach((k, v) -> contentCount.put(k.getSimpleName(), v));
		contentCount.put("Documents", documents.all().stream().filter(d -> d.published && d.game.equals(game.name)).count());
		contentCount.put("GameTypes", gametypes.all().stream().filter(d -> !d.deleted && d.game.equals(game.name)).count());

		Map<String, Long> managedCount = managed.all()
												.stream()
												.filter(d -> d.published && d.game.equals(game.name))
												.collect(Collectors.groupingBy(m -> m.group, Collectors.counting()));

		contentCount.put("Updates", managed.all().stream().filter(d -> d.published && d.game.equals(game.name)).count());

		pages.add("index-game.ftl", SiteMap.Page.of(1f, SiteMap.ChangeFrequency.monthly), game.name)
			 .put("game", game)
			 .put("managed", managedCount)
			 .put("count", contentCount)
			 .write(root.resolve(Util.slug(game.name)).resolve("index.html"));
	}
}
