package net.shrimpworks.unreal.archive.www;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface SiteMap extends PageGenerator {

	public Page DEFAULT_PAGE = Page.of(Page.DEFAULT_PRIORITY, Page.DEFAULT_LAST_MOD, Page.DEFAULT_FREQ);

	enum ChangeFrequency {
		always,
		hourly,
		daily,
		weekly,
		monthly,
		yearly,
		never
	}

	static SiteMap siteMap(String rootUrl, Path root, Set<Page> pages, int pageLimit) {
		return new SiteMapImpl(rootUrl, root, pages, pageLimit);
	}

	class SiteMapImpl implements SiteMap {

		private final String rootUrl;
		private final Path root;
		private final List<Page> pages;

		public SiteMapImpl(String rootUrl, Path root, Set<Page> pages, int pageLimit) {
			this.rootUrl = rootUrl;
			this.root = root;
			this.pages = pages.stream()
							  .sorted(Collections.reverseOrder())
							  .limit(pageLimit)
							  .collect(Collectors.toList());
		}

		@Override
		public Set<Page> generate() {
			Set<Page> genPages = new HashSet<>();

			try {
				genPages.add(Templates.template("sitemap.ftl")
									  .put("rootUrl", rootUrl)
									  .put("pages", pages)
									  .put("siteRoot", root)
									  .write(root.resolve("sitemap.xml")));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			return genPages;
		}
	}

	public class Page implements Comparable<Page> {

		public static final LocalDate DEFAULT_LAST_MOD = LocalDate.now();
		public static final float DEFAULT_PRIORITY = 0.5f;
		public static final ChangeFrequency DEFAULT_FREQ = ChangeFrequency.monthly;

		public final Path path;
		public final float priority;
		public final LocalDate lastMod;
		public final ChangeFrequency changeFreq;

		public static Page of(Path path, float priority, LocalDate lastMod, ChangeFrequency changeFreq) {
			return new Page(path, priority, lastMod, changeFreq);
		}

		public static Page of(float priority, LocalDate lastMod, ChangeFrequency changeFreq) {
			return new Page(null, priority, lastMod, changeFreq);
		}

		public static Page of(float priority, ChangeFrequency changeFreq) {
			return new Page(null, priority, DEFAULT_LAST_MOD, changeFreq);
		}

		public static Page weekly(float priority) {
			return new Page(null, priority, DEFAULT_LAST_MOD, ChangeFrequency.weekly);
		}

		public static Page weekly(float priority, LocalDate lastMod) {
			return new Page(null, priority, lastMod, ChangeFrequency.weekly);
		}

		public static Page weekly(float priority, LocalDateTime lastMod) {
			return new Page(null, priority, lastMod.toLocalDate(), ChangeFrequency.weekly);
		}

		public static Page monthly(float priority) {
			return new Page(null, priority, DEFAULT_LAST_MOD, ChangeFrequency.weekly);
		}

		public static Page monthly(float priority, LocalDate lastMod) {
			return new Page(null, priority, lastMod, ChangeFrequency.weekly);
		}

		public static Page monthly(float priority, LocalDateTime lastMod) {
			return new Page(null, priority, lastMod.toLocalDate(), ChangeFrequency.weekly);
		}

		private Page(Path path, float priority, LocalDate lastMod, ChangeFrequency changeFreq) {
			this.path = path;
			this.priority = priority;
			this.lastMod = lastMod;
			this.changeFreq = changeFreq;
		}

		public Page withPath(Path path) {
			return new Page(path, priority, lastMod, changeFreq);
		}

		@Override
		public int compareTo(Page other) {
			return Float.compare(priority, other.priority);
		}
	}
}
