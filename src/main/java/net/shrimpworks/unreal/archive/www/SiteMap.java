package net.shrimpworks.unreal.archive.www;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface SiteMap extends PageGenerator {

	final Page DEFAULT_PAGE = Page.of(Page.DEFAULT_PRIORITY, Page.DEFAULT_LAST_MOD, Page.DEFAULT_FREQ);
	final String SITE_ROOT = System.getenv().getOrDefault("SITE_URL", "");

	enum ChangeFrequency {
		always,
		hourly,
		daily,
		weekly,
		monthly,
		yearly,
		never
	}

	static SiteMap siteMap(String rootUrl, Path root, Set<Page> pages, int pageLimit, SiteFeatures features) {
		return new SiteMapImpl(rootUrl, root, pages, pageLimit, features);
	}

	class SiteMapImpl implements SiteMap {

		private final String rootUrl;
		private final Path root;
		private final List<Page> pages;
		private final SiteFeatures features;

		public SiteMapImpl(String rootUrl, Path root, Set<Page> pages, int pageLimit, SiteFeatures features) {
			this.features = features;
			if (!rootUrl.endsWith("/")) this.rootUrl = rootUrl + "/";
			else this.rootUrl = rootUrl;
			this.root = root;
			this.pages = pages.stream()
							  .sorted(Collections.reverseOrder())
							  .limit(pageLimit)
							  .collect(Collectors.toList());
		}

		@Override
		public Set<Page> generate() {
			Templates.PageSet genPages = new Templates.PageSet("", features, root, root, root);

			pages.stream().filter(p -> p.path == null).forEach(System.out::println);

			genPages.add("sitemap.ftl", Page.monthly(0), "Sitemap")
					.put("rootUrl", rootUrl)
					.put("pages", pages)
					.write(root.resolve("sitemap.xml"));

			genPages.add("robots.ftl", Page.monthly(0), "Robots")
					.put("rootUrl", rootUrl)
					.write(root.resolve("robots.txt"));

			return genPages.pages;
		}
	}

	public class Page implements Comparable<Page> {

		public static final LocalDate DEFAULT_LAST_MOD = LocalDate.now();
		public static final float DEFAULT_PRIORITY = 0.5f;
		public static final ChangeFrequency DEFAULT_FREQ = ChangeFrequency.monthly;

		public Path path;
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
			return new Page(null, priority, DEFAULT_LAST_MOD, ChangeFrequency.monthly);
		}

		public static Page monthly(float priority, LocalDate lastMod) {
			return new Page(null, priority, lastMod, ChangeFrequency.monthly);
		}

		public static Page monthly(float priority, LocalDateTime lastMod) {
			return new Page(null, priority, lastMod.toLocalDate(), ChangeFrequency.monthly);
		}

		private Page(Path path, float priority, LocalDate lastMod, ChangeFrequency changeFreq) {
			this.path = path;
			this.priority = priority;
			this.lastMod = lastMod;
			this.changeFreq = changeFreq;
		}

		public Page withPath(Path path) {
			this.path = path;
			return this;
		}

		@Override
		public int compareTo(Page other) {
			return Float.compare(priority, other.priority);
		}
	}
}
