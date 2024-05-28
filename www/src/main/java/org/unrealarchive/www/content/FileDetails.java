package org.unrealarchive.www.content;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.unrealarchive.common.Util;
import org.unrealarchive.content.FileType;
import org.unrealarchive.content.addons.Addon;
import org.unrealarchive.content.addons.SimpleAddonRepository;
import org.unrealarchive.www.SiteFeatures;
import org.unrealarchive.www.SiteMap;
import org.unrealarchive.www.Templates;

public class FileDetails extends ContentPageGenerator {

	public FileDetails(SimpleAddonRepository content, Path output, Path staticRoot, SiteFeatures features) {
		super(content, output, output, staticRoot, features);
	}

	private Map<Addon.ContentFile, List<Addon>> loadContentFiles(SimpleAddonRepository content) {
		final Map<Addon.ContentFile, List<Addon>> contentFiles = new HashMap<>();
		content.all()
			   .forEach(c -> {
				   for (Addon.ContentFile f : c.files) {
					   Collection<Addon> contents = contentFiles.computeIfAbsent(f, h -> new ArrayList<>());
					   contents.add(c);
				   }
			   });
		return contentFiles;
	}

	@Override
	public Set<SiteMap.Page> generate() {
		final Map<Addon.ContentFile, List<Addon>> contentFiles = loadContentFiles(content);

		Templates.PageSet pages = pageSet("content/files");
		contentFiles.entrySet().parallelStream().forEach(e -> {
			// we're only interested in multi-use files
			if (e.getValue().size() < 2) return;

			final String game = e.getValue().get(0).game();
			Addon.ContentFile file = e.getKey();

			Path p = root.resolve(Util.slug(game)).resolve("files")
						 .resolve(file.hash.substring(0, 2)).resolve(file.hash + ".html");

			e.getValue().sort(Comparator.comparing(a -> a.name));

			pages.add("file.ftl", SiteMap.Page.monthly(0.25f), String.join(" / ", game, "Files", file.name))
				 .put("game", game)
				 .put("file", file)
				 .put("type", FileType.forFile(file.name))
				 .put("packages", e.getValue())
				 .write(p);
		});

		return pages.pages;
	}

}
