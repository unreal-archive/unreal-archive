package net.shrimpworks.unreal.archive.www;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.shrimpworks.unreal.archive.indexer.Content;
import net.shrimpworks.unreal.archive.indexer.ContentManager;

public class FileDetails {

	private final ContentManager content;
	private final Path root;
	private final Path staticRoot;

	private final Map<Content.ContentFile, List<Content>> contentFiles;

	public FileDetails(ContentManager content, Path output, Path staticRoot) {
		this.content = content;
		this.root = output.resolve("files");
		this.staticRoot = staticRoot;

		this.contentFiles = new HashMap<>();
		content.search(null, null, null, null)
			   .forEach(c -> {
				   for (Content.ContentFile f : c.files) {
					   Collection<Content> contents = contentFiles.computeIfAbsent(f, h -> new ArrayList<>());
					   contents.add(c);
				   }
			   });
	}

	public int generate() {
		int count = 0;
		try {

			for (Map.Entry<Content.ContentFile, List<Content>> e : contentFiles.entrySet()) {
				// we're only interested in multi-use files
				if (e.getValue().size() < 2) continue;

				Path p = root.resolve(e.getKey().hash.substring(0, 2));

				e.getValue().sort(Comparator.comparing(a -> a.name));

				Templates.template("files/file.ftl")
						 .put("static", p.relativize(staticRoot))
						 .put("title", String.join(" / ", "Files", e.getKey().name))
						 .put("file", e.getKey())
						 .put("packages", e.getValue())
						 .put("siteRoot", root.resolve("files").relativize(root))
						 .write(p.resolve(e.getKey().hash + ".html"));
				count++;
			}

		} catch (
				IOException e) {
			throw new RuntimeException("Failed to render page", e);
		}

		return count;
	}

}
