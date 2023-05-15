package org.unrealarchive.content.wiki;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.unrealarchive.common.JSON;
import org.unrealarchive.common.Util;
import org.unrealarchive.common.YAML;

public interface WikiRepository {

	public Wiki wiki(String name);

	public Set<Wiki> all();

	public int size();

	public static class FileRepository implements WikiRepository {

		private final Map<String, WikiRepository.Wiki> wikis;

		public static void main(String[] args) throws IOException {
//			convert(Paths.get("./unreal-archive-data/wikis/beyondunreal"));
//			clean(Paths.get("./unreal-archive-data/wikis/liandri"));
		}

		public static void convert(Path wikiRoot) throws IOException {
			try (Stream<Path> files = Files.walk(wikiRoot.resolve("content"), FileVisitOption.FOLLOW_LINKS)) {
				files.filter(f -> Files.isRegularFile(f) && Util.extension(f).equalsIgnoreCase("json"))
					 .forEach(f -> {
						 try {
							 WikiPage pg = JSON.fromFile(f, WikiPage.class);
							 Files.write(f.resolveSibling(String.format("%s.yml", Util.plainName(f))), YAML.toBytes(pg));
							 System.out.println("rewrite " + f);
							 Files.deleteIfExists(f);
						 } catch (IOException e) {
							 throw new RuntimeException(e);
						 }
					 });
			}
		}

		public static void clean(Path wikiRoot) throws IOException {
			WikiRepository.Wiki wiki = YAML.fromFile(wikiRoot.resolve("wiki.yml"), WikiRepository.Wiki.class);
			try (Stream<Path> files = Files.walk(wikiRoot.resolve("content"), FileVisitOption.FOLLOW_LINKS)) {
				files.filter(f -> Files.isRegularFile(f) && Util.extension(f).equalsIgnoreCase("yml"))
					 .forEach(f -> {
						 try {
							 WikiPage pg = YAML.fromFile(f, WikiPage.class);
							 boolean delete =
								 pg.parse.categories.stream()
													.anyMatch(c -> wiki.skipCategories.stream().anyMatch(c.name::contains))
								 || pg.parse.templates.stream()
													  .anyMatch(c -> wiki.skipTemplates.stream().anyMatch(c.name::contains));

							 if (delete) {
								 System.out.println("Deleting " + f);
								 Files.deleteIfExists(f);
								 Files.deleteIfExists(f.getParent().resolve("Talk:" + Util.fileName(f)));
							 }
						 } catch (IOException e) {
							 throw new RuntimeException(e);
						 }
					 });
			}
		}

		public FileRepository(Path wikiRoot) throws IOException {
			this.wikis = new HashMap<>();

			// exit if there's no wiki content
			if (!Files.exists(wikiRoot)) return;

			try (Stream<Path> list = Files.list(wikiRoot)) {
				list.forEach(d -> {
					try {
						if (Files.exists(d.resolve("wiki.yml"))) {
							// cool it's a wiki, lets load it and its pages
							WikiRepository.Wiki wiki = YAML.fromFile(d.resolve("wiki.yml"), WikiRepository.Wiki.class);
							wiki.path = d;
							if (Files.exists(d.resolve("interwiki.yml"))) {
								wiki.interWiki = YAML.fromFile(d.resolve("interwiki.yml"), InterWikiList.class);
							}
							// create a redirect from the homepage to index
							wiki.redirects.put("Main_Page", "index");
							wiki.redirects.put("Main Page", "index");
							wikis.put(wiki.name, wiki);

							try (Stream<Path> files = Files.walk(d.resolve("content"))) {
								files.parallel()
									 .filter(f -> Files.isRegularFile(f) && Util.extension(f).equalsIgnoreCase("yml"))
									 .forEach(f -> {
										 try {
											 WikiPage pg = YAML.fromFile(f, WikiPage.class);
											 wiki.addPage(f, pg);
										 } catch (IOException e) {
											 throw new RuntimeException(e);
										 }
									 });
							}
						}
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
			}
		}

		@Override
		public WikiRepository.Wiki wiki(String name) {
			return wikis.get(name);
		}

		@Override
		public Set<WikiRepository.Wiki> all() {
			return wikis.values().stream().filter(w -> w.publish).collect(Collectors.toSet());
		}

		@Override
		public int size() {
			return wikis.values().stream().mapToInt(w -> w.pages.size()).sum();
		}

	}

	// FIXME a wiki should be a top-level content element, and the WikiPageHolder should be an implementation detail of the FileRepository
	public static final class Wiki {

		private static final Pattern REDIRECT = Pattern.compile("#REDIRECT ?\\[\\[([^]]+)]].*", Pattern.DOTALL);

		public transient Path path;
		public String name;
		public String owner;
		public String url;
		public WikiLicence licence;
		public String title;
		public String homepage;
		public String imagesPath;
		public Set<String> skipCategories;
		public Set<String> skipTemplates;
		public Set<String> deleteElements;
		public boolean publish;

		public transient InterWikiList interWiki = new InterWikiList(Set.of());

		public transient final Map<String, String> redirects = new ConcurrentHashMap<>();
		private transient final Map<String, WikiPageHolder> pages = new ConcurrentHashMap<>();

		public void addPage(Path path, WikiPage page) {
			WikiPageHolder pageHolder = new WikiPageHolder(path);
			pages.put(page.name, pageHolder);

			Matcher matcher = REDIRECT.matcher(page.parse.wikitext.text);
			if (matcher.matches()) {
				redirects.put(page.name, matcher.group(1));
				redirects.put(page.name.replaceAll(" ", "_"), matcher.group(1));
				page.isRedirect = true;
				pageHolder.isRedirect = true;
			}
		}

		public WikiPage page(String name) {
			return pages.get(name).get();
		}

		public Set<WikiPage> all() {
			return pages.values().stream()
						.filter(p -> !p.isRedirect)
						.map(WikiPageHolder::get)
						.collect(Collectors.toSet());
		}

		public static class WikiLicence {

			public String name;
			public String url;
		}

		private static class WikiPageHolder {

			private final Path path;
			private SoftReference<WikiPage> page;
			private boolean isRedirect;

			public WikiPageHolder(Path path) {
				this.path = path;
				this.page = null;
			}

			public WikiPage get() {
				WikiPage maybePage = page != null ? page.get() : null;
				if (maybePage == null) {
					try {
						maybePage = YAML.fromFile(path, WikiPage.class);
						page = new SoftReference<>(maybePage);
					} catch (IOException e) {
						throw new RuntimeException("Failed to load page from file", e);
					}
				}
				return maybePage;
			}
		}
	}
}
