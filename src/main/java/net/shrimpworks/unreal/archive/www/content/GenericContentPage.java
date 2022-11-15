package net.shrimpworks.unreal.archive.www.content;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.ContentManager;
import net.shrimpworks.unreal.archive.content.Games;
import net.shrimpworks.unreal.archive.www.SiteFeatures;
import net.shrimpworks.unreal.archive.www.SiteMap;
import net.shrimpworks.unreal.archive.www.Templates;

import static net.shrimpworks.unreal.archive.Util.slug;
import static net.shrimpworks.unreal.archive.www.Templates.PAGE_SIZE;

public abstract class GenericContentPage<T extends Content> extends ContentPageGenerator {

	static final DateTimeFormatter RELEASE_DATE_FORMAT = new DateTimeFormatterBuilder()
		.append(DateTimeFormatter.ofPattern("yyyy-MM"))
		.parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
		.toFormatter();
	static final LocalDate MIN_DATE = LocalDate.of(1998, 1, 1);
	static final LocalDate MAX_DATE = LocalDate.now();

	/**
	 * Create a new Page Generator instance.
	 *
	 * @param content    content manager
	 * @param siteRoot   root directory of the website output
	 * @param output     path to write this generator's output to
	 * @param staticRoot path to static content
	 * @param features   if true, download and reference local copies of remote images
	 */
	public GenericContentPage(ContentManager content, Path siteRoot, Path output, Path staticRoot, SiteFeatures features) {
		super(content, siteRoot, output, staticRoot, features);
	}

	Templates.PageSet pageSet(String resourceRoot) {
		return new Templates.PageSet(resourceRoot, features, siteRoot, staticRoot, root);
	}

	abstract String gameSubGroup(T item);

	String letterSubGroup(T item) {
		return item.subGrouping();
	}

	/**
	 * Download and store image files locally.
	 * <p>
	 * This works by replacing in-memory image attachment URLs
	 * with local paths for the purposes of outputting HTML pages
	 * linking to local copies, rather than the original remotes.
	 *
	 * @param content   content item to download images for
	 * @param localPath local output path
	 */
	void localImages(Content content, Path localPath) {
		if (!features.localImages) return;

		// find all the images
		List<Content.Attachment> images = content.attachments.stream()
															 .filter(a -> a.type == Content.AttachmentType.IMAGE)
															 .collect(Collectors.toList());

		// we're creating a sub-directory here, to create a nicer looking on-disk structure
		Path imgPath = localPath.resolve("images");
		try {
			if (!Files.exists(imgPath)) Files.createDirectories(imgPath);
		} catch (IOException e) {
			System.err.printf("\rFailed to download create output directory %s: %s%n", imgPath, e);
			return;
		}

		for (Content.Attachment img : images) {
			try {
				System.out.printf("\rDownloading image %-60s", img.name);

				// prepend filenames with the content hash, to prevent conflicts
				String hashName = String.join("_", content.hash.substring(0, 8), img.name);
				Path outPath = imgPath.resolve(Util.safeFileName(hashName));

				// only download if it doesn't already exist locally
				if (!Files.exists(outPath)) Util.downloadTo(img.url, outPath);

				// replace the actual attachment with the local copy
				content.attachments.remove(img);
				content.attachments.add(new Content.Attachment(img.type, img.name, localPath.relativize(outPath).toString()));
			} catch (Throwable t) {
				System.err.printf("\rFailed to download image %s: %s%n", img.name, t);
			}
		}
	}

	public Map<Integer, Map<Integer, Integer>> timeline(Game game) {
		Map<LocalDate, Integer> grouped = game.groups.values().stream().flatMap(g -> g.letters.values().stream()).flatMap(
												  g -> g.pages.stream()).flatMap(g -> g.items.stream())
													 .parallel()
													 .filter(c -> c.releaseDate.isPresent())
													 .filter(c -> c.releaseDate.get().isAfter(MIN_DATE)
																  && c.releaseDate.get().isBefore(MAX_DATE))
													 .collect(
														 HashMap::new,
														 (m, c) -> m.compute(c.releaseDate.get(), (k, v) -> v == null ? 1 : v + 1),
														 (a, b) -> b.forEach((k, v) -> a.compute(k, (x, y) -> y == null ? v : y + v))
													 );

		if (grouped.isEmpty()) return Map.of();

		LocalDate min = grouped.keySet().stream().min(LocalDate::compareTo).get();
		LocalDate max = grouped.keySet().stream().max(LocalDate::compareTo).get();

		Map<Integer, Map<Integer, Integer>> everything = new TreeMap<>();
		min.datesUntil(max.plusMonths(1), Period.ofMonths(1)).forEachOrdered(
			d -> everything.compute(d.getYear(), (k, v) -> {
				Map<Integer, Integer> months = v == null ? new TreeMap<>() : v;
				months.put(d.getMonthValue(), grouped.getOrDefault(d, 0));
				return months;
			})
		);

		return everything;
	}

	void generateTimeline(Templates.PageSet pages, Map<Integer, Map<Integer, Integer>> timeline, Game game, String sectionName) {
		timeline.forEach((year, months) -> {
			pages.add("months.ftl", SiteMap.Page.weekly(0.55f), String.join(" / ", sectionName, game.game.bigName, Integer.toString(year)))
				 .put("game", game)
				 .put("timeline", timeline)
				 .put("year", year)
				 .put("months", months)
				 .write(game.path.resolve("releases").resolve(Integer.toString(year)).resolve("index.html"));

			months.forEach((month, count) -> {
				LocalDate date = LocalDate.of(year, month, 1);
				pages.add("month.ftl", SiteMap.Page.weekly(0.60f),
						  String.join(" / ", sectionName, game.game.bigName, Integer.toString(year), date.getMonth().toString()))
					 .put("game", game)
					 .put("timeline", timeline)
					 .put("year", year)
					 .put("month", month)
					 .put("items", game.dated.getOrDefault(date, List.of()))
					 .write(game.path.resolve("releases")
									 .resolve(Integer.toString(year))
									 .resolve(Integer.toString(month))
									 .resolve("index.html"));
			});
		});
	}

	public class GameList {

		public final TreeMap<String, Game> games = new TreeMap<>();

		public void clear() {
			games.clear();
		}
	}

	public class Game {

		public final Games game;
		public final String name;
		public final String slug;
		public final Path path;
		public final TreeMap<String, SubGroup> groups = new TreeMap<>();
		public int count;

		public final HashMap<LocalDate, List<ContentInfo<T>>> dated;

		public Game(String name) {
			this.game = Games.byName(name);
			this.name = name;
			this.slug = slug(name);
			this.path = root.resolve(slug);
			this.count = 0;

			this.dated = new HashMap<>();
		}

		public ContentInfo<T> add(T item) {
			SubGroup gametype = groups.computeIfAbsent(gameSubGroup(item), g -> new SubGroup(this, g));
			this.count++;

			ContentInfo<T> added = gametype.add(item);

			added.releaseDate.map(r -> dated.computeIfAbsent(r, d -> new ArrayList<>()).add(added));

			return added;
		}
	}

	public class SubGroup {

		public final Game game;
		public final String name;
		public final String slug;
		public final Path path;
		public final TreeMap<String, LetterGroup> letters = new TreeMap<>();
		public int count;

		public SubGroup(Game game, String name) {
			this.game = game;
			this.name = name;
			this.slug = slug(name);
			this.path = game.path.resolve(slug);
			this.count = 0;
		}

		public ContentInfo<T> add(T item) {
			LetterGroup letter = letters.computeIfAbsent(letterSubGroup(item), l -> new LetterGroup(this, l));
			this.count++;

			return letter.add(item);
		}
	}

	public class LetterGroup {

		public final SubGroup group;
		public final String letter;
		public final Path path;
		public final List<Page> pages = new ArrayList<>();
		public int count;

		public LetterGroup(SubGroup group, String letter) {
			this.group = group;
			this.letter = letter;
			this.path = group.path.resolve(letter);
			this.count = 0;
		}

		public ContentInfo<T> add(T item) {
			if (pages.isEmpty()) pages.add(new Page(this, 1));
			Page page = pages.get(pages.size() - 1);
			if (page.items.size() == PAGE_SIZE) {
				page = new Page(this, pages.size() + 1);
				pages.add(page);
			}
			this.count++;

			return page.add(item);
		}
	}

	public class Page {

		public final LetterGroup letter;
		public final int number;
		public final Path path;
		public final List<ContentInfo<T>> items = new ArrayList<>(PAGE_SIZE);

		public Page(LetterGroup letter, int number) {
			this.letter = letter;
			this.number = number;
			this.path = letter.path.resolve(Integer.toString(number));
		}

		public ContentInfo<T> add(T item) {
			ContentInfo<T> added = new ContentInfo<>(this, item);
			this.items.add(added);
			Collections.sort(items);
			return added;
		}
	}

	public class ContentInfo<Y extends T> implements Comparable<ContentInfo<Y>> {

		public final Page page;
		public final String itemHash;
		private final String itemName;
		public final Path path;

		public final Collection<ContentInfo<Y>> variations;
		public final Map<String, Integer> alsoIn;

		public final Optional<LocalDate> releaseDate;

		@SuppressWarnings("unchecked")
		public ContentInfo(Page page, Y item) {
			this.page = page;
			this.itemHash = item.hash;
			this.itemName = item.name;
			this.path = item.slugPath(siteRoot);

			this.alsoIn = new HashMap<>();
			for (Content.ContentFile f : item.files) {
				int alsoInCount = content.containingFileCount(f.hash);
				if (alsoInCount > 1) alsoIn.put(f.hash, alsoInCount - 1);
			}

			this.variations = content.variationsOf(item.hash).stream()
									 .filter(p -> p.getClass().isAssignableFrom(item.getClass()))
									 .map(p -> new ContentInfo<>(page, (Y)p))
									 .sorted()
									 .collect(Collectors.toList());

			if (item.releaseDate() != null && !item.releaseDate().equals("Unknown")) {
				releaseDate = Optional.of(LocalDate.parse(item.releaseDate(), RELEASE_DATE_FORMAT));
			} else {
				releaseDate = Optional.empty();
			}
		}

		public Y item() {
			final Content item = content.forHash(itemHash);
//			Collections.sort(item.downloads);
//			Collections.sort(item.files);

			return (Y)item;
		}

		@Override
		public int compareTo(ContentInfo<Y> o) {
			return itemName.toLowerCase().compareTo(o.itemName.toLowerCase());
		}
	}
}
