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
import org.unrealarchive.content.RepositoryManager;
import org.unrealarchive.content.addons.GameType;
import org.unrealarchive.content.managed.Managed;

public class Index implements PageGenerator {

	private final RepositoryManager repos;
	private final Path root;
	private final Path staticRoot;
	private final SiteFeatures features;

	public Index(RepositoryManager repos, Path output, Path staticRoot, SiteFeatures features) {
		this.repos = repos;

		this.root = output;
		this.staticRoot = staticRoot;
		this.features = features;
	}

	@Override
	public Set<SiteMap.Page> generate() {
		Map<String, Long> contentCount = new HashMap<>();

		Map<Games, Long> games = new LinkedHashMap<>();

		Map<String, Long> contentByGame = repos.addons().countByGame();
		Map<String, Long> gametypesByGame = repos.gameTypes().all().stream().collect(Collectors.groupingBy(GameType::game, Collectors.counting()));
		Map<String, Long> managedByGame = repos.managed().all().stream().collect(Collectors.groupingBy(Managed::game, Collectors.counting()));
		Map<String, Long> docsByGame = repos.docs().all().stream().collect(Collectors.groupingBy(d -> d.game, Collectors.counting()));

		Arrays.stream(Games.values()).forEach(g -> {
			long c = contentByGame.getOrDefault(g.name, 0L)
					 + gametypesByGame.getOrDefault(g.name, 0L)
					 + managedByGame.getOrDefault(g.name, 0L)
					 + docsByGame.getOrDefault(g.name, 0L);

			if (c > 0) games.put(g, c);
		});

		Templates.PageSet pages = new Templates.PageSet("", features, root, staticRoot);

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

		repos.addons().countByType(game.name).forEach((k, v) -> contentCount.put(k.getSimpleName(), v));
		contentCount.put("Documents", repos.docs().all().stream().filter(d -> d.published && d.game.equals(game.name)).count());
		contentCount.put("GameTypes", repos.gameTypes().all().stream().filter(d -> !d.deleted && d.game.equals(game.name)).count());

		Map<String, Long> managedCount = repos.managed().all()
											  .stream()
											  .filter(d -> d.published && d.game.equals(game.name))
											  .collect(Collectors.groupingBy(m -> m.group, Collectors.counting()));

		contentCount.put("Updates", repos.managed().all().stream().filter(d -> d.published && d.game.equals(game.name)).count());

		pages.add("index-game.ftl", SiteMap.Page.of(1f, SiteMap.ChangeFrequency.monthly), game.name)
			 .put("game", game)
			 .put("managed", managedCount)
			 .put("count", contentCount)
			 .write(root.resolve(Util.slug(game.name)).resolve("index.html"));
	}
}
