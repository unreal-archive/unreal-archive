package net.shrimpworks.unreal.archive.www.content;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.shrimpworks.unreal.archive.content.ContentRepository;
import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.FileType;
import net.shrimpworks.unreal.archive.www.SiteFeatures;
import net.shrimpworks.unreal.archive.www.SiteMap;
import net.shrimpworks.unreal.archive.www.Templates;

public class FileDetails extends ContentPageGenerator {

	public FileDetails(ContentRepository content, Path output, Path staticRoot, SiteFeatures features) {
		super(content, output, output.resolve("files"), staticRoot, features);
	}

	private Map<Content.ContentFile, List<Content>> loadContentFiles(ContentRepository content) {
		final Map<Content.ContentFile, List<Content>> contentFiles = new HashMap<>();
		content.all()
			   .forEach(c -> {
				   for (Content.ContentFile f : c.files) {
					   Collection<Content> contents = contentFiles.computeIfAbsent(f, h -> new ArrayList<>());
					   contents.add(c);
				   }
			   });
		return contentFiles;
	}

	@Override
	public Set<SiteMap.Page> generate() {
		final Map<Content.ContentFile, List<Content>> contentFiles = loadContentFiles(content);

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
