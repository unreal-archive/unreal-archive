package net.shrimpworks.unreal.archive.www.content;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.shrimpworks.unreal.archive.content.ContentEntity;
import net.shrimpworks.unreal.archive.common.Util;
import net.shrimpworks.unreal.archive.content.addons.SimpleAddonRepository;
import net.shrimpworks.unreal.archive.content.addons.GameTypeRepository;
import net.shrimpworks.unreal.archive.content.Games;
import net.shrimpworks.unreal.archive.content.managed.ManagedContentRepository;
import net.shrimpworks.unreal.archive.www.SiteFeatures;
import net.shrimpworks.unreal.archive.www.SiteMap;
import net.shrimpworks.unreal.archive.www.Templates;

public class Latest extends ContentPageGenerator {

	private final GameTypeRepository gameTypes;
	private final ManagedContentRepository managed;

	public Latest(SimpleAddonRepository content, GameTypeRepository gameTypes, ManagedContentRepository managed, Path output, Path staticRoot,
				  SiteFeatures features) {
		super(content, output, output.resolve("latest"), staticRoot, features);

		this.gameTypes = gameTypes;
		this.managed = managed;
	}

	private Set<ContentEntity<?>> loadAllContent(SimpleAddonRepository content, GameTypeRepository gameTypes, ManagedContentRepository managed) {
		return Stream.concat(Stream.concat(
								 content.all(false).stream(),
								 gameTypes.all().stream()),
							 managed.all().stream())
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
