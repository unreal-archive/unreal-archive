package org.unrealarchive.www.content;

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

import org.unrealarchive.common.Util;
import org.unrealarchive.content.ContentEntity;
import org.unrealarchive.content.FileType;
import org.unrealarchive.content.Games;
import org.unrealarchive.content.addons.Addon;
import org.unrealarchive.content.addons.GameTypeRepository;
import org.unrealarchive.content.addons.SimpleAddonRepository;
import org.unrealarchive.content.managed.ManagedContentRepository;
import org.unrealarchive.www.SiteFeatures;
import org.unrealarchive.www.SiteMap;
import org.unrealarchive.www.Templates;

public class Packages extends ContentPageGenerator {

	private static final Set<String> PKG_TYPES = Arrays.stream(FileType.PACKAGES)
													   .filter(p -> p != FileType.MAP)
													   .flatMap(p -> p.ext.stream())
													   .collect(Collectors.toSet());

	private final GameTypeRepository gameTypes;
	private final ManagedContentRepository managed;

	public Packages(SimpleAddonRepository content, GameTypeRepository gameTypes, ManagedContentRepository managed,
					Path root, Path staticRoot, SiteFeatures features) {
		super(content, root, staticRoot, features);

		this.gameTypes = gameTypes;
		this.managed = managed;
	}

	public static Map<Games, Map<String, Map<Addon.ContentFile, List<ContentEntity<?>>>>> loadContentFiles(
		SimpleAddonRepository content,
		GameTypeRepository gameTypes
	) {
		// TODO include ManagedContentRepository managed
		final Map<Games, Map<String, Map<Addon.ContentFile, List<ContentEntity<?>>>>> contentFiles = new HashMap<>();

		content.all()
			   .forEach(c -> {
				   for (Addon.ContentFile f : c.files) {
					   if (PKG_TYPES.contains(Util.extension(f.name).toLowerCase())) {
						   String pkgName = Util.plainName(f.name).toLowerCase();
						   contentFiles.computeIfAbsent(Games.byName(c.game), g -> new HashMap<>())
									   .computeIfAbsent(pkgName, p -> new HashMap<>())
									   .computeIfAbsent(f, fc -> new ArrayList<>()).add(c);
					   }
				   }
			   });

		// hoooly shit
		gameTypes.all()
				 .forEach(g -> g.releases.forEach(r -> r.files.forEach(c -> {
					 for (Addon.ContentFile f : c.files) {
						 if (PKG_TYPES.contains(Util.extension(f.name).toLowerCase())) {
							 String pkgName = Util.plainName(f.name).toLowerCase();
							 contentFiles.computeIfAbsent(Games.byName(g.game), n -> new HashMap<>())
										 .computeIfAbsent(pkgName, p -> new HashMap<>())
										 .computeIfAbsent(f, fc -> new ArrayList<>()).add(g);
						 }
					 }
				 })));

		return contentFiles;
	}

	@Override
	public Set<SiteMap.Page> generate() {
		Map<Games, Map<String, Map<Addon.ContentFile, List<ContentEntity<?>>>>> contentFiles = loadContentFiles(content, gameTypes);

		Templates.PageSet pages = pageSet("content/packages");

		contentFiles.entrySet().parallelStream().forEach(game -> {
			game.getValue().entrySet().parallelStream().forEach(e -> {
				Path p = root.resolve(Util.slug(game.getKey().name))
							 .resolve("packages")
							 .resolve(Util.authorSlug(e.getKey()))
							 .resolve("index.html");

				LinkedHashMap<Addon.ContentFile, List<ContentEntity<?>>> sorted =
					e.getValue().entrySet()
					 .stream()
					 .sorted(Collections.reverseOrder(Comparator.comparingInt(a -> a.getValue().size())))
					 .peek(a -> Collections.sort(a.getValue()))
					 .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

				pages.add("package.ftl", SiteMap.Page.monthly(0.3f), String.join(" / ", game.getKey().name, "Packages", e.getKey()))
					 .put("game", game.getKey().name)
					 .put("package", e.getKey())
					 .put("packageFiles", sorted)
					 .write(p);
			});
		});

		return pages.pages;
	}
}
