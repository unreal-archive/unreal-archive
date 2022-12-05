package net.shrimpworks.unreal.archive.wiki;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.YAML;

public class WikiManager {

	private static final Pattern REDIRECT = Pattern.compile("#REDIRECT \\[\\[(.*)]]");

	private final Map<String, Wiki> wikis;

	public WikiManager(Path wikiRoot) throws IOException {
		this.wikis = new HashMap<>();

		// exit if there's no wiki content
		if (!Files.exists(wikiRoot)) return;

		Files.list(wikiRoot).forEach(d -> {
			try {
				if (Files.exists(d.resolve("wiki.yml"))) {
					// cool it's a wiki, lets load it and its pages
					Wiki wiki = YAML.fromFile(d.resolve("wiki.yml"), Wiki.class);
					wikis.put(wiki.name, wiki);
					Files.walk(d.resolve("content"), FileVisitOption.FOLLOW_LINKS)
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

	public int size() {
		return wikis.values().stream().mapToInt(w -> w.pages.size()).sum();
	}

	public static final class Wiki {

		public String name;
		public String host;
		public String url;

		public final Map<String, String> redirects = new HashMap<>();
		private final Map<String, WikiPageHolder> pages = new HashMap<>();

		public void addPage(Path path, WikiPage page) {
			WikiPageHolder pageHolder = new WikiPageHolder(path, page);
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

	private static class WikiPageHolder {

		private final Path path;
		private SoftReference<WikiPage> page;
		private boolean isRedirect;

		public WikiPageHolder(Path path, WikiPage page) {
			this.path = path;
			this.page = new SoftReference<>(page);
		}

		public WikiPage get() {
			WikiPage maybePage = page.get();
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
