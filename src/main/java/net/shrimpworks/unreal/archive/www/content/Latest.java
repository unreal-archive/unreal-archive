package net.shrimpworks.unreal.archive.www.content;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.ContentManager;
import net.shrimpworks.unreal.archive.content.Games;
import net.shrimpworks.unreal.archive.www.SiteFeatures;
import net.shrimpworks.unreal.archive.www.SiteMap;
import net.shrimpworks.unreal.archive.www.Templates;

public class Latest extends ContentPageGenerator {

	private final Set<Content> allContent;

	public Latest(ContentManager content, Path output, Path staticRoot, SiteFeatures features) {
		super(content, output, output.resolve("latest"), staticRoot, features);

		this.allContent = content.search(null, null, null, null).stream()
								 .filter(c -> !c.deleted)
								 .filter(c -> c.variationOf == null).collect(Collectors.toSet());
	}

	@Override
	public Set<SiteMap.Page> generate() {
		TreeMap<LocalDate, List<Content>> contentFiles = new TreeMap<>(
				allContent.stream().collect(Collectors.groupingBy(c -> c.firstIndex.toLocalDate()))
		);

		Templates.PageSet pages = pageSet("content/latest");
		pages.add("index.ftl", SiteMap.Page.weekly(0.97f), "Latest Content Additions")
			 .put("latest", contentFiles.descendingMap())
			 .write(root.resolve("index.html"));

		// all games RSS
		pages.add("feed.ftl", SiteMap.Page.weekly(0f), "Latest Content Additions")
			 .put("latest", contentFiles.descendingMap())
			 .write(root.resolve("feed.xml"));

		// per game RSS
		for (Games game : Games.values()) {
			TreeMap<LocalDate, List<Content>> gameContent = new TreeMap<>(
					allContent.stream().filter(c -> c.game.equals(game.name))
							  .collect(Collectors.groupingBy(c -> c.firstIndex.toLocalDate()))
			);
			pages.add("feed.ftl", SiteMap.Page.weekly(0f), "Latest " + game.name + " Additions")
				 .put("latest", gameContent.descendingMap())
				 .write(root.resolve(Util.slug(game.name) + ".xml"));
		}

		return pages.pages;
	}

}
