package net.shrimpworks.unreal.archive.wiki;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.JSON;
import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.YAML;

public class WikiManager {

	private static final Pattern REDIRECT = Pattern.compile("#REDIRECT ?\\[\\[(.*)]]");

	private final Map<String, Wiki> wikis;

	public static void main(String[] args) throws IOException {
		convert(Paths.get("./unreal-archive-data/wikis/liandri"));
	}

	public static void convert(Path wikiRoot) throws IOException {
		Files.walk(wikiRoot.resolve("content"), FileVisitOption.FOLLOW_LINKS)
			 .filter(f -> Files.isRegularFile(f) && Util.extension(f).equalsIgnoreCase("json"))
			 .forEach(f -> {
				 try {
					 WikiPage pg = JSON.fromFile(f, WikiPage.class);
					 Files.write(f.resolveSibling(String.format("%s.yml", Util.plainName(f))), YAML.toBytes(pg));
					 Files.deleteIfExists(f);
				 } catch (IOException e) {
					 throw new RuntimeException(e);
				 }
			 });
	}

	public WikiManager(Path wikiRoot) throws IOException {
		this.wikis = new HashMap<>();

		// exit if there's no wiki content
		if (!Files.exists(wikiRoot)) return;

		Files.list(wikiRoot).forEach(d -> {
			try {
				if (Files.exists(d.resolve("wiki.yml"))) {
					// cool it's a wiki, lets load it and its pages
					Wiki wiki = YAML.fromFile(d.resolve("wiki.yml"), Wiki.class);
					wiki.path = d;
					if (Files.exists(d.resolve("interwiki.yml"))) {
						wiki.interWiki = YAML.fromFile(d.resolve("interwiki.yml"), InterWikiList.class);
					}
					// create a redirect from the homepage to index
					wiki.redirects.put("Main_Page", "index");
					wiki.redirects.put("Main Page", "index");
					wikis.put(wiki.name, wiki);

					Files.walk(d.resolve("content"))
						 .parallel()
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
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	public Wiki wiki(String name) {
		return wikis.get(name);
	}

	public Set<Wiki> all() {
		return wikis.values().stream().filter(w -> w.publish).collect(Collectors.toSet());
	}

	public int size() {
		return wikis.values().stream().mapToInt(w -> w.pages.size()).sum();
	}

	public static final class Wiki {

		public Path path;
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

		public InterWikiList interWiki = new InterWikiList(Set.of());

		public final Map<String, String> redirects = new ConcurrentHashMap<>();
		private final Map<String, WikiPageHolder> pages = new ConcurrentHashMap<>();

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
