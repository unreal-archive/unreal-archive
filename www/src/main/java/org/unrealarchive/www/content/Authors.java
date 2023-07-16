package org.unrealarchive.www.content;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.unrealarchive.content.AuthorNames;
import org.unrealarchive.content.ContentEntity;
import org.unrealarchive.content.addons.GameTypeRepository;
import org.unrealarchive.content.addons.SimpleAddonRepository;
import org.unrealarchive.content.managed.ManagedContentRepository;
import org.unrealarchive.www.SiteFeatures;
import org.unrealarchive.www.SiteMap;
import org.unrealarchive.www.Templates;

import static org.unrealarchive.common.Util.authorSlug;

public class Authors extends ContentPageGenerator {

	private static final String SECTION = "Authors";
	private final AuthorNames names;
	private final GameTypeRepository gameTypes;
	private final ManagedContentRepository managed;

	public Authors(AuthorNames names, SimpleAddonRepository content, GameTypeRepository gameTypes, ManagedContentRepository managed,
				   Path output, Path staticRoot, SiteFeatures features) {
		super(content, output, output.resolve("authors"), staticRoot, features);

		this.names = names;
		this.gameTypes = gameTypes;
		this.managed = managed;

	}

	private TreeMap<String, LetterGroup> loadLetters(AuthorNames names, SimpleAddonRepository content, GameTypeRepository gameTypes,
													 ManagedContentRepository managed) {
		final TreeMap<String, LetterGroup> letters = new TreeMap<>();

		Stream.concat(Stream.concat(content.all(false).stream(),
									gameTypes.all().stream()), managed.all().stream())
			  .filter(c -> c.author().length() > 2)
			  .filter(c -> !c.author().equalsIgnoreCase("Unknown"))
			  .filter(c -> !c.author().equalsIgnoreCase("Various"))
			  .collect(Collectors.groupingBy(
				  c -> names.cleanName(c.author()).toLowerCase().replaceAll("[\"`()\\[\\]<>{}=*-]", "'"))
			  ).entrySet().stream()
			  .sorted(Map.Entry.comparingByKey())
			  .forEach(e -> {
				  String authorName = names.cleanName(e.getValue().get(0).author());
				  String pageLetter = pageSelection(authorName);
				  if (pageLetter != null) {
					  LetterGroup letter = letters.computeIfAbsent(pageLetter, LetterGroup::new);
					  letter.add(authorName, e.getValue());
				  }
			  });

		return letters;
	}

	private String pageSelection(String author) {
		char first;

		String normalised = Normalizer.normalize(author.toUpperCase(Locale.ENGLISH), Normalizer.Form.NFKD).toUpperCase();

		if (normalised.startsWith("\"")) normalised = normalised.substring(1);
		if (normalised.startsWith("'")) normalised = normalised.substring(1);

		// skip names which are nothing but unprintable characters
		if (normalised.replaceAll("([^A-Za-z0-9])", "").trim().isBlank()) return null;

		else if (Character.isDigit(normalised.charAt(0))) first = '0';
		else if (Character.isAlphabetic(normalised.charAt(0))) first = normalised.charAt(0);
		else first = '_';

		return Character.toString(first);
	}

	@Override
	public Set<SiteMap.Page> generate() {
		final TreeMap<String, LetterGroup> letters = loadLetters(names, content, gameTypes, managed);

		Templates.PageSet pages = pageSet("content/authors");

		letters.entrySet().parallelStream().forEach(l -> {
			l.getValue().pages.parallelStream().forEach(p -> {
				pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION))
					 .put("letters", letters)
					 .put("page", p)
					 .write(p.path.resolve("index.html"));

				p.authors.parallelStream().forEach(author -> authorPage(pages, author));
			});

			// output first letter/page combo, with appropriate relative links
			pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION))
				 .put("letters", letters)
				 .put("page", l.getValue().pages.get(0))
				 .write(l.getValue().path.resolve("index.html"));
		});

		// output first letter/page combo, with appropriate relative links
		pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION))
			 .put("letters", letters)
			 .put("page", letters.firstEntry().getValue().pages.get(0))
			 .write(root.resolve("index.html"));

		return pages.pages;
	}

	private void authorPage(Templates.PageSet pages, AuthorInfo author) {
		pages.add("author.ftl", SiteMap.Page.monthly(author.count > 2 ? 0.92f : 0.67f), String.join(" / ", SECTION, author.author))
			 .put("author", author)
			 .write(Paths.get(author.path.toString() + ".html"));
	}

	public class LetterGroup {

		public final String letter;
		public final Path path;
		public final List<Page> pages = new ArrayList<>();
		public int count = 0;

		public LetterGroup(String letter) {
			this.letter = letter;
			this.path = root.resolve(letter);
		}

		public void add(String author, List<ContentEntity<?>> contents) {
			if (pages.isEmpty()) pages.add(new Page(this, 1));
			Page page = pages.get(pages.size() - 1);
			if (page.authors.size() == Templates.PAGE_SIZE) {
				page = new Page(this, pages.size() + 1);
				pages.add(page);
			}

			page.add(author, contents);

			if (page.count > 0) count++;
		}
	}

	public class Page {

		public final LetterGroup letter;
		public final int number;
		public final Path path;
		public final List<AuthorInfo> authors = new ArrayList<>();

		public int count;

		public Page(LetterGroup letter, int number) {
			this.letter = letter;
			this.number = number;
			this.path = letter.path.resolve(Integer.toString(number));
		}

		public void add(String author, List<ContentEntity<?>> contents) {
			this.authors.add(new AuthorInfo(this, author, contents));
			Collections.sort(authors);

			if (contents.size() > 1) this.count++;
		}
	}

	public class AuthorInfo implements Comparable<AuthorInfo> {

		public final Page page;
		public final String author;
		public final Map<String, List<ContentEntity<?>>> contents;
		public final String slug;
		public final Path path;

		public final int count;
		public final String leadImage;

		public AuthorInfo(Page page, String author, List<ContentEntity<?>> contents) {
			this.page = page;
			this.author = author;
			this.contents = new HashMap<>(contents.stream().sorted().collect(Collectors.groupingBy(ContentEntity::friendlyType)));
			this.slug = authorSlug(author);

			this.path = root.resolve(slug);

			this.count = contents.size();

			List<ContentEntity<?>> shuffled = new ArrayList<>(contents);
			Collections.shuffle(shuffled);
			leadImage = shuffled.stream()
								.filter(e -> e.leadImage() != null && !e.leadImage().isBlank())
								.map(ContentEntity::leadImage)
								.findAny()
								.map(i -> {
									// a remote URL of some sort
									if (i.contains("://")) return i;

									// a local path - make it relative
									return path.relativize(siteRoot.resolve(i)).toString();
								})
								.orElse(null);
		}

		@Override
		public int compareTo(AuthorInfo o) {
			return author.toLowerCase().compareTo(o.author.toLowerCase());
		}
	}
}
