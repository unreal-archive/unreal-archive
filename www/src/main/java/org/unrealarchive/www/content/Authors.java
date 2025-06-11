package org.unrealarchive.www.content;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.unrealarchive.common.Util;
import org.unrealarchive.content.Author;
import org.unrealarchive.content.AuthorInfo;
import org.unrealarchive.content.AuthorRepository;
import org.unrealarchive.content.ContentEntity;
import org.unrealarchive.content.Contributors;
import org.unrealarchive.content.addons.GameType;
import org.unrealarchive.content.addons.GameTypeRepository;
import org.unrealarchive.content.addons.MapPack;
import org.unrealarchive.content.addons.SimpleAddonRepository;
import org.unrealarchive.content.managed.ManagedContentRepository;
import org.unrealarchive.www.SiteFeatures;
import org.unrealarchive.www.SiteMap;
import org.unrealarchive.www.Templates;

import static org.unrealarchive.content.Authors.isSomeone;

public class Authors extends ContentPageGenerator {

	private static final String SECTION = "Authors";
	private final AuthorRepository authors;
	private final GameTypeRepository gameTypes;
	private final ManagedContentRepository managed;
	private final Path sectionPath;

	public Authors(AuthorRepository authors, SimpleAddonRepository content, GameTypeRepository gameTypes, ManagedContentRepository managed,
				   Path root, Path staticRoot, SiteFeatures features) {
		super(content, root, staticRoot, features);

		this.authors = authors;
		this.gameTypes = gameTypes;
		this.managed = managed;
		this.sectionPath = root.resolve("authors");
	}

	private TreeMap<String, LetterGroup> loadLetters() {
		final TreeMap<String, LetterGroup> letters = new TreeMap<>();

		Stream.concat(Stream.concat(content.all(false).stream(),
									gameTypes.all().stream()), managed.all().stream())
			  .forEach(c -> {
				  AuthorInfo a = c.authorInfo();
				  Contributors contribs = a.contributors();
				  if (contribs != null) {
					  if (!contribs.contributors.isEmpty()) {
						  contribs
							  .contributors
							  .stream().filter(org.unrealarchive.content.Authors::isSomeone)
							  .forEach(cc -> {
								  String pageLetter = pageSelection(cc.name);
								  if (pageLetter != null) {
									  LetterGroup letter = letters.computeIfAbsent(pageLetter, LetterGroup::new);
									  letter.addContributed(cc, c);
								  }
							  });
					  } else if (!contribs.modifiedBy.isEmpty()) {
						  contribs.modifiedBy
							  .stream().filter(org.unrealarchive.content.Authors::isSomeone)
							  .forEach(mc -> {
								  String pageLetter = pageSelection(mc.name);
								  if (pageLetter != null) {
									  LetterGroup letter = letters.computeIfAbsent(pageLetter, LetterGroup::new);
									  letter.addModified(mc, c);
								  }
							  });
						  if (isSomeone(contribs.originalAuthor)) {
							  String pageLetter = pageSelection(contribs.originalAuthor.name);
							  if (pageLetter != null) {
								  LetterGroup letter = letters.computeIfAbsent(pageLetter, LetterGroup::new);
								  letter.add(contribs.originalAuthor, c);
							  }
						  }
					  }
				  } else if (isSomeone(a.author())) {
					  String pageLetter = pageSelection(a.author().name);
					  if (pageLetter != null) {
						  LetterGroup letter = letters.computeIfAbsent(pageLetter, LetterGroup::new);
						  letter.add(a.author(), c);
					  }
				  } else {
					  if (c instanceof MapPack mp) {
						  mp.maps.stream().filter(m -> isSomeone(m.author)).forEach(m -> {
							  AuthorInfo ma = m.authorInfo();
							  if (isSomeone(ma.author()) && !ma.equals(a)) {
								  String pageLetter = pageSelection(ma.author().name);
								  if (pageLetter != null) {
									  LetterGroup letter = letters.computeIfAbsent(pageLetter, LetterGroup::new);
									  letter.addContributed(ma.author(), c);
								  }
							  }
						  });
					  } else if (c instanceof GameType gt) {
						  gt.credits.values().stream().flatMap(Collection::stream).forEach(credit -> {
							  AuthorInfo ga = new AuthorInfo(credit);
							  if (isSomeone(ga.author()) && !ga.equals(a)) {
								  String pageLetter = pageSelection(ga.author().name);
								  if (pageLetter != null) {
									  LetterGroup letter = letters.computeIfAbsent(pageLetter, LetterGroup::new);
									  letter.addContributed(ga.author(), c);
								  }
							  }
						  });
					  }
				  }
			  });

		return letters;
	}

	private String pageSelection(String author) {
		char first;

		String normalised = Util.normalised(author).toUpperCase();

		if (normalised.startsWith("\"")) normalised = normalised.substring(1);
		if (normalised.startsWith("'")) normalised = normalised.substring(1);

		// skip names which are nothing but unprintable characters
		if (normalised.replaceAll("([^A-Za-z0-9])", "").trim().isBlank()) return null;

		// special handling hacks
		else if (normalised.charAt(0) == 'Ð') first = 'D';
		else if (normalised.charAt(0) == 'º') first = '§';

		else if (Character.isDigit(normalised.charAt(0))) first = '0';
		else if (Character.isAlphabetic(normalised.charAt(0))) first = normalised.charAt(0);

		else first = '§';

		return Character.toString(first);
	}

	@Override
	public Set<SiteMap.Page> generate() {
		final TreeMap<String, LetterGroup> letters = loadLetters();

		Templates.PageSet pages = pageSet("content/authors");

		letters.entrySet().parallelStream().forEach(l -> {
			List<Page> letterPages = l.getValue().pages();
			for (int i = 0; i < letterPages.size(); i++) {
				Page p = letterPages.get(i);
				pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION))
					 .put("letters", letters)
					 .put("page", p)
					 .put("authorsPath", sectionPath)
					 .write(
						 l.getValue().path.resolve(Integer.toString(i + 1)).resolve("index.html"),
						 i == 0 ? l.getValue().path.resolve("index.html") : null
					 );

				p.authors.parallelStream().forEach(author -> {
					try {
						authorPage(pages, author);
					} catch (IOException e) {
						throw new RuntimeException("Error generating author page", e);
					}
				});
			}
		});

		// output first letter/page combo, with appropriate relative links
		pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION))
			 .put("letters", letters)
			 .put("page", letters.firstEntry().getValue().pages().getFirst())
			 .put("authorsPath", sectionPath)
			 .write(sectionPath.resolve("index.html"));

		return pages.pages;
	}

	private void authorPage(Templates.PageSet pages, AuthorInfoHolder author) throws IOException {
		final Path outPath = Files.isDirectory(author.path) ? author.path : Files.createDirectories(author.path);

		this.authors.writeContent(author.author, outPath);

		pages.add("author.ftl", SiteMap.Page.monthly(author.count() > 2 ? 0.92f : 0.67f),
				  String.join(" / ", SECTION, author.author.name))
			 .put("author", author)
			 .put("authorsPath", sectionPath)
			 .write(outPath.resolve("index.html"));
	}

	public class LetterGroup {

		public final String letter;
		public final Path path;
		public final Set<AuthorInfoHolder> authors = new TreeSet<>();

		public LetterGroup(String letter) {
			this.letter = letter;
			this.path = sectionPath.resolve(letter);
		}

		private synchronized AuthorInfoHolder findAuthor(Author author) {
			return authors.stream().filter(a -> a.author.equals(author))
						  .findAny()
						  .orElseGet(() -> {
							  AuthorInfoHolder holder = new AuthorInfoHolder(author);
							  authors.add(holder);
							  return holder;
						  });
		}

		public synchronized void add(Author author, ContentEntity<?> content) {
			findAuthor(author).add(content);
		}

		public void addContributed(Author author, ContentEntity<?> content) {
			findAuthor(author).addContributed(content);
		}

		public void addModified(Author author, ContentEntity<?> content) {
			findAuthor(author).addModified(content);
		}

		public List<Page> pages() {
			List<Page> pages = new ArrayList<>();
			Page currentPage = new Page(this, 1);
			pages.add(currentPage);

			for (AuthorInfoHolder author : authors) {
				if (currentPage.authors.size() >= Templates.PAGE_SIZE) {
					currentPage = new Page(this, pages.size() + 1);
					pages.add(currentPage);
				}
				currentPage.authors.add(author);
			}

			return pages;
		}

		public long count() {
			return authors.stream().filter(a -> a.count() > 1).count();
		}
	}

	public class Page {

		public final LetterGroup letter;
		public final int number;
		public final Path path;
		public final Set<AuthorInfoHolder> authors = new TreeSet<>();

		public Page(LetterGroup letter, int number) {
			this.letter = letter;
			this.number = number;
			this.path = letter.path.resolve(Integer.toString(number));
		}
	}

	public class AuthorInfoHolder implements Comparable<AuthorInfoHolder> {

		public final Author author;
		public final String slug;
		public final Path path;

		public final Map<String, TreeSet<ContentEntity<?>>> created = new ConcurrentHashMap<>();
		public final Map<String, TreeSet<ContentEntity<?>>> contributed = new ConcurrentHashMap<>();
		public final Map<String, TreeSet<ContentEntity<?>>> modified = new ConcurrentHashMap<>();

		private String leadImage = null;

		public AuthorInfoHolder(Author author) {
			this.author = author;
			this.slug = author.slug();

			this.path = author.pagePath(root);
		}

		public synchronized void add(ContentEntity<?> content) {
			created.computeIfAbsent(content.friendlyType(), t -> new TreeSet<>()).add(content);
		}

		public void addContributed(ContentEntity<?> content) {
			contributed.computeIfAbsent(content.friendlyType(), t -> new TreeSet<>()).add(content);
		}

		public void addModified(ContentEntity<?> content) {
			modified.computeIfAbsent(content.friendlyType(), t -> new TreeSet<>()).add(content);
		}

		public long count() {
			return created.values().stream().mapToLong(Set::size).sum()
				   + contributed.values().stream().mapToLong(Set::size).sum()
				   + modified.values().stream().mapToLong(Set::size).sum();
		}

		public String leadImage() {
			if (this.leadImage != null) return leadImage;

			List<ContentEntity<?>> shuffled = Stream.concat(
														Stream.concat(created.values().stream(), contributed.values().stream()),
														modified.values().stream()
													)
													.flatMap(Set::stream).collect(Collectors.toList());
			Collections.shuffle(shuffled);
			this.leadImage = shuffled.stream()
									 .filter(e -> e.leadImage() != null && !e.leadImage().isBlank())
									 .map(ContentEntity::leadImage)
									 .findAny()
									 .map(i -> {
										 // a remote URL of some sort
										 if (i.contains("://")) return i;

										 // a local path - make it relative
										 return path.relativize(root.resolve(i)).toString();
									 })
									 .orElse(null);

			return leadImage;
		}

		@Override
		public int compareTo(AuthorInfoHolder o) {
			return author.compareTo(o.author);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o instanceof Author && author.equals(o)) return true;
			if (o == null || getClass() != o.getClass()) return false;
			AuthorInfoHolder that = (AuthorInfoHolder)o;
			return author.equals(that.author);
		}

		@Override
		public int hashCode() {
			return author.hashCode();
		}
	}
}
