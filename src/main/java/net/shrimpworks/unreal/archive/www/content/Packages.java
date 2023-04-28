package net.shrimpworks.unreal.archive.www.content;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.common.Util;
import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.ContentRepository;
import net.shrimpworks.unreal.archive.content.FileType;
import net.shrimpworks.unreal.archive.content.GameTypeRepository;
import net.shrimpworks.unreal.archive.content.Games;
import net.shrimpworks.unreal.archive.managed.ManagedContentRepository;
import net.shrimpworks.unreal.archive.www.SiteFeatures;
import net.shrimpworks.unreal.archive.www.SiteMap;
import net.shrimpworks.unreal.archive.www.Templates;

public class Packages extends ContentPageGenerator {

	private static final Set<String> PKG_TYPES = Arrays.stream(FileType.PACKAGES)
													   .filter(p -> p != FileType.MAP)
													   .flatMap(p -> p.ext.stream())
													   .collect(Collectors.toSet());

	private final GameTypeRepository gameTypes;
	private final ManagedContentRepository managed;

	public Packages(ContentRepository content, GameTypeRepository gameTypes, ManagedContentRepository managed, Path output, Path staticRoot,
					SiteFeatures features) {
		super(content, output, output.resolve("packages"), staticRoot, features);

		this.gameTypes = gameTypes;
		this.managed = managed;
	}

	public Map<Games, Map<String, Map<Content.ContentFile, List<Content>>>> loadContentFiles(ContentRepository content,
																							 GameTypeRepository gameTypes,
																							 ManagedContentRepository managed) {
		final Map<Games, Map<String, Map<Content.ContentFile, List<Content>>>> contentFiles = new HashMap<>();
		content.all()
			   .forEach(c -> {
				   for (Content.ContentFile f : c.files) {
					   if (PKG_TYPES.contains(Util.extension(f.name).toLowerCase())) {
						   String pkgName = Util.plainName(f.name).toLowerCase();
						   contentFiles.computeIfAbsent(Games.byName(c.game), g -> new HashMap<>())
									   .computeIfAbsent(pkgName, p -> new HashMap<>())
									   .computeIfAbsent(f, fc -> new ArrayList<>()).add(c);
					   }
				   }
			   });
		return contentFiles;
	}

	@Override
	public Set<SiteMap.Page> generate() {
		Map<Games, Map<String, Map<Content.ContentFile, List<Content>>>> contentFiles = loadContentFiles(content, gameTypes, managed);

		Templates.PageSet pages = pageSet("content/packages");

		contentFiles.entrySet().parallelStream().forEach(game -> {
			game.getValue().entrySet().parallelStream().forEach(e -> {
				Path p = root.resolve(Util.slug(game.getKey().name)).resolve(Util.authorSlug(e.getKey())).resolve("index.html");

				final Content.ContentFile[] any = new Content.ContentFile[1];
				LinkedHashMap<Content.ContentFile, List<Content>> sorted =
					e.getValue().entrySet()
					 .stream()
					 .sorted(Collections.reverseOrder(Comparator.comparingInt(a -> a.getValue().size())))
					 .peek(a -> Collections.sort(a.getValue()))
					 .peek(a -> any[0] = a.getKey())
					 .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

				pages.add("package.ftl", SiteMap.Page.monthly(0.3f), String.join(" / ", "Packages", game.getKey().name, e.getKey()))
					 .put("game", game.getKey().name)
					 .put("package", e.getKey())
					 .put("type", FileType.forFile(any[0].name))
					 .put("packageFiles", sorted)
					 .write(p);
			});
		});

		return pages.pages;
	}
}
