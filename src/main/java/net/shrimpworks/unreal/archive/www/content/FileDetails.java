package net.shrimpworks.unreal.archive.www.content;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.ContentManager;
import net.shrimpworks.unreal.archive.www.SiteMap;
import net.shrimpworks.unreal.archive.www.Templates;

public class FileDetails extends ContentPageGenerator {

	private final Map<Content.ContentFile, List<Content>> contentFiles;
	private final Path siteRoot;

	public FileDetails(ContentManager content, Path output, Path staticRoot, boolean localImages) {
		super(content, output.resolve("files"), staticRoot, localImages);
		this.siteRoot = output;

		this.contentFiles = new HashMap<>();
		content.search(null, null, null, null)
			   .forEach(c -> {
				   for (Content.ContentFile f : c.files) {
					   Collection<Content> contents = contentFiles.computeIfAbsent(f, h -> new ArrayList<>());
					   contents.add(c);
				   }
			   });
	}

	@Override
	public Set<SiteMap.Page> generate() {
		Templates.PageSet pages = new Templates.PageSet("content/files", siteRoot, staticRoot, root);
		contentFiles.entrySet().parallelStream().forEach(e -> {
			// we're only interested in multi-use files
			if (e.getValue().size() < 2) return;

			Path p = root.resolve(e.getKey().hash.substring(0, 2)).resolve(e.getKey().hash + ".html");

			e.getValue().sort(Comparator.comparing(a -> a.name));

			pages.add("file.ftl", SiteMap.Page.monthly(0.25f), String.join(" / ", "Files", e.getKey().name))
				 .put("file", e.getKey())
				 .put("packages", e.getValue())
				 .write(p);
		});

		return pages.pages;
	}

}
