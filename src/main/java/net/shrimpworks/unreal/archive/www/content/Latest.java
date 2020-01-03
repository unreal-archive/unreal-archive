package net.shrimpworks.unreal.archive.www.content;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.ContentManager;
import net.shrimpworks.unreal.archive.www.SiteFeatures;
import net.shrimpworks.unreal.archive.www.SiteMap;
import net.shrimpworks.unreal.archive.www.Templates;

public class Latest extends ContentPageGenerator {

	private final TreeMap<LocalDate, List<Content>> contentFiles;

	public Latest(ContentManager content, Path output, Path staticRoot, SiteFeatures features) {
		super(content, output, output.resolve("latest"), staticRoot, features);

		this.contentFiles = new TreeMap<>();
		this.contentFiles.putAll(content.search(null, null, null, null).stream()
										.filter(c -> !c.deleted)
										.filter(c -> c.variationOf == null)
										.collect(Collectors.groupingBy(c -> c.firstIndex.toLocalDate())));
	}

	@Override
	public Set<SiteMap.Page> generate() {
		Templates.PageSet pages = pageSet("content/latest");
		pages.add("index.ftl", SiteMap.Page.weekly(0.97f), "Latest Content Additions")
			 .put("latest", contentFiles.descendingMap())
			 .write(root.resolve("index.html"));

		return pages.pages;
	}

}
