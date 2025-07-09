package org.unrealarchive.www.content;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.unrealarchive.common.Util;
import org.unrealarchive.content.ContentEntity;
import org.unrealarchive.content.Games;
import org.unrealarchive.content.RepositoryManager;
import org.unrealarchive.www.SiteFeatures;
import org.unrealarchive.www.SiteMap;
import org.unrealarchive.www.Templates;

public class Latest extends ContentPageGenerator {

	private final Path sectionRoot;

	public Latest(RepositoryManager repos, Path root,
				  Path staticRoot, SiteFeatures features) {
		super(repos, root, staticRoot, features);

		this.sectionRoot = root.resolve("latest");
	}

	private Set<ContentEntity<?>> loadAllContent() {
		return Stream.concat(Stream.concat(
								 repos.addons().all(false).stream(),
								 repos.gameTypes().all().stream()),
							 repos.managed().all().stream().filter(d -> d.published))
					 .collect(Collectors.toSet());
	}

	@Override
	public Set<SiteMap.Page> generate() {
		final Set<ContentEntity<?>> allContent = loadAllContent();

		TreeMap<LocalDate, List<ContentEntity<?>>> contentFiles = new TreeMap<>(
			allContent.stream()
					  .sorted((a, b) -> -a.addedDate().compareTo(b.addedDate()))
					  .collect(Collectors.groupingBy(c -> c.addedDate().toLocalDate()))
		);

		Templates.PageSet pages = pageSet("content/latest");
		pages.add("index.ftl", SiteMap.Page.weekly(0.97f), "Latest Content Additions")
			 .put("latest", contentFiles.descendingMap())
			 .write(sectionRoot.resolve("index.html"));

		// all games RSS
		pages.add("feed.ftl", SiteMap.Page.weekly(0f), "Latest Content Additions")
			 .put("latest", contentFiles.descendingMap())
			 .write(sectionRoot.resolve("feed.xml"));

		// per game RSS
		for (Games game : Games.values()) {
			TreeMap<LocalDate, List<ContentEntity<?>>> gameContent = new TreeMap<>(
				allContent.stream().filter(c -> c.game().equals(game.name))
						  .sorted((a, b) -> -a.addedDate().compareTo(b.addedDate()))
						  .collect(Collectors.groupingBy(c -> c.addedDate().toLocalDate()))
			);
			pages.add("feed.ftl", SiteMap.Page.weekly(0f), "Latest " + game.name + " Additions")
				 .put("latest", gameContent.descendingMap())
				 .write(sectionRoot.resolve(Util.slug(game.name) + ".xml"));
		}

		return pages.pages;
	}

}
