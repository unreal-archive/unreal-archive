package net.shrimpworks.unreal.archive.www.content;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.shrimpworks.unreal.archive.ContentEntity;
import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.content.ContentManager;
import net.shrimpworks.unreal.archive.content.GameTypeManager;
import net.shrimpworks.unreal.archive.content.Games;
import net.shrimpworks.unreal.archive.managed.ManagedContentManager;
import net.shrimpworks.unreal.archive.www.SiteFeatures;
import net.shrimpworks.unreal.archive.www.SiteMap;
import net.shrimpworks.unreal.archive.www.Templates;

public class Latest extends ContentPageGenerator {

	private final GameTypeManager gameTypes;
	private final ManagedContentManager managed;

	public Latest(ContentManager content, GameTypeManager gameTypes, ManagedContentManager managed, Path output, Path staticRoot,
				  SiteFeatures features) {
		super(content, output, output.resolve("latest"), staticRoot, features);

		this.gameTypes = gameTypes;
		this.managed = managed;
	}

	private Set<ContentEntity<?>> loadAllContent(ContentManager content, GameTypeManager gameTypes, ManagedContentManager managed) {
		Set<String> variations = content.all().stream().map(c -> c.variationOf).filter(Objects::nonNull).collect(Collectors.toSet());

		return Stream.concat(Stream.concat(
								 // exclude content which are variations of existing things
								 content.all().stream().filter(c -> !variations.contains(c.hash)),
								 gameTypes.all().stream()),
							 managed.all().stream())
					 .filter(c -> !c.deleted())
					 .filter(c -> !c.isVariation())
					 .collect(Collectors.toSet());
	}

	@Override
	public Set<SiteMap.Page> generate() {
		final Set<ContentEntity<?>> allContent = loadAllContent(content, gameTypes, managed);

		TreeMap<LocalDate, List<ContentEntity<?>>> contentFiles = new TreeMap<>(
			allContent.stream()
					  .sorted((a, b) -> -a.addedDate().compareTo(b.addedDate()))
					  .collect(Collectors.groupingBy(c -> c.addedDate().toLocalDate()))
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
			TreeMap<LocalDate, List<ContentEntity<?>>> gameContent = new TreeMap<>(
				allContent.stream().filter(c -> c.game().equals(game.name))
						  .sorted((a, b) -> -a.addedDate().compareTo(b.addedDate()))
						  .collect(Collectors.groupingBy(c -> c.addedDate().toLocalDate()))
			);
			pages.add("feed.ftl", SiteMap.Page.weekly(0f), "Latest " + game.name + " Additions")
				 .put("latest", gameContent.descendingMap())
				 .write(root.resolve(Util.slug(game.name) + ".xml"));
		}

		return pages.pages;
	}

}
