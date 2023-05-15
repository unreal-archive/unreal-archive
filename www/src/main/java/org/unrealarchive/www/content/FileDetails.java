package org.unrealarchive.www.content;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.unrealarchive.content.addons.SimpleAddonRepository;
import org.unrealarchive.content.addons.Addon;
import org.unrealarchive.content.FileType;
import org.unrealarchive.www.SiteFeatures;
import org.unrealarchive.www.SiteMap;
import org.unrealarchive.www.Templates;

public class FileDetails extends ContentPageGenerator {

	public FileDetails(SimpleAddonRepository content, Path output, Path staticRoot, SiteFeatures features) {
		super(content, output, output.resolve("files"), staticRoot, features);
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

			Path p = root.resolve(e.getKey().hash.substring(0, 2)).resolve(e.getKey().hash + ".html");

			e.getValue().sort(Comparator.comparing(a -> a.name));

			pages.add("file.ftl", SiteMap.Page.monthly(0.25f), String.join(" / ", "Files", e.getKey().name))
				 .put("file", e.getKey())
				 .put("type", FileType.forFile(e.getKey().name))
				 .put("packages", e.getValue())
				 .write(p);
		});

		return pages.pages;
	}

}
