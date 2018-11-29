package net.shrimpworks.unreal.archive.www;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.github.rjeschke.txtmark.Configuration;
import com.github.rjeschke.txtmark.Processor;

import net.shrimpworks.unreal.archive.docs.Document;
import net.shrimpworks.unreal.archive.docs.DocumentManager;

import static net.shrimpworks.unreal.archive.www.Templates.slug;

public class Documents {

	private static final String SECTION = "Articles";

	private final DocumentManager documents;
	private final Path root;
	private final Path staticRoot;

	private final Map<String, DocumentGroup> groups;

	public Documents(DocumentManager documents, Path root, Path staticRoot) {
		this.documents = documents;
		this.root = root.resolve("documents");
		this.staticRoot = staticRoot;

		this.groups = new HashMap<>();

		documents.all().stream()
				 .filter(d -> d.published)
				 .forEach(d -> {
					 DocumentGroup group = groups.computeIfAbsent(d.game, g -> new DocumentGroup(null, g));
					 group.add(d);
				 });
	}

	/**
	 * Generate one or more HTML pages of output.
	 *
	 * @return number of individual pages created
	 */
	public int generate() {
		int count = 0;
		try {
			// create the root landing page, for reasons
			DocumentGroup rootGroup = new DocumentGroup(null, "");
			rootGroup.groups.putAll(groups);
			count += generateGroup(rootGroup);

			for (DocumentGroup group : groups.values()) {
				count += generateGroup(group);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to render page", e);
		}

		return count;
	}

	private int generateGroup(DocumentGroup group) throws IOException {
		int count = 0;

		Templates.template("docs/group.ftl")
				 .put("static", root.resolve(group.path).relativize(staticRoot))
				 .put("title", String.join(" / ", SECTION, String.join(" / ", group.pPath.split("/"))))
				 .put("group", group)
				 .put("siteRoot", root.resolve(group.path).relativize(root))
				 .write(root.resolve(group.path).resolve("index.html"));

		count++;

		for (DocumentGroup g : group.groups.values()) {
			count += generateGroup(g);
		}

		for (DocumentInfo d : group.documents) {
			count += generateDocument(d);
		}

		return count;
	}

	private int generateDocument(DocumentInfo doc) throws IOException {

		try (ReadableByteChannel docChan = documents.document(doc.document)) {

			final Path path = Files.createDirectories(root.resolve(doc.path));

			final Path docRoot = documents.documentRoot(doc.document);
			Files.walk(docRoot, FileVisitOption.FOLLOW_LINKS)
				 .forEach(p -> {
					 if (Files.isRegularFile(p)) {
						 Path relPath = docRoot.relativize(p);
						 Path copyPath = path.resolve(relPath);

						 try {
							 if (!Files.isDirectory(copyPath.getParent())) Files.createDirectories(copyPath.getParent());
							 Files.copy(p, copyPath, StandardCopyOption.REPLACE_EXISTING);
						 } catch (IOException e) {
							 e.printStackTrace();
						 }
					 }
				 });

			final Configuration config = Configuration.builder()
													  .forceExtentedProfile()
													  .setEncoding(StandardCharsets.UTF_8.name())
													  .build();

			final String content = Processor.process(Channels.newInputStream(docChan), config);

			Templates.template("docs/document.ftl")
					 .put("static", path.relativize(staticRoot))
					 .put("title", String.join(" / ", SECTION, doc.document.game, String.join(" / ", doc.document.path.split("/")),
											   doc.document.title))
					 .put("document", doc)
					 .put("content", content)
					 .put("siteRoot", path.relativize(root))
					 .write(path.resolve("index.html"));
		}

		return 1;
	}

	public class DocumentGroup {

		private final String pPath;

		public final String name;
		public final String slug;
		public final String path;
		public final DocumentGroup parent;

		public final TreeMap<String, DocumentGroup> groups = new TreeMap<>();
		public final List<DocumentInfo> documents = new ArrayList<>();

		public int docs;

		public DocumentGroup(DocumentGroup parent, String name) {
			this.pPath = parent != null ? parent.pPath.isEmpty() ? name : String.join("/", parent.pPath, name) : "";

			this.parent = parent;

			this.name = name;
			this.slug = slug(name);
			this.path = parent != null ? String.join("/", parent.path, slug) : slug;
			this.docs = 0;
		}

		public void add(Document d) {
			if (d.path.equals(pPath)) {
				documents.add(new DocumentInfo(d, this));
			} else {
				String[] next = (pPath.isEmpty() ? d.path : d.path.replaceFirst(pPath + "/", "")).split("/");
				String nextName = (next.length > 0 && !next[0].isEmpty()) ? next[0] : "";

				DocumentGroup group = groups.computeIfAbsent(nextName, g -> new DocumentGroup(this, g));
				group.add(d);
			}
			this.docs++;
		}
	}

	public class DocumentInfo {

		public final Document document;
		public final DocumentGroup group;

		public final String slug;
		public final String path;

		public DocumentInfo(Document document, DocumentGroup group) {
			this.document = document;
			this.group = group;

			this.slug = slug(document.title);
			this.path = group != null ? String.join("/", group.path, slug) : slug;
		}
	}

}
