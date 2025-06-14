package org.unrealarchive.content;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.unrealarchive.common.Util;
import org.unrealarchive.content.addons.GameTypeRepository;
import org.unrealarchive.content.addons.SimpleAddonRepository;
import org.unrealarchive.content.managed.ManagedContentRepository;

import static org.unrealarchive.content.AuthorRepository.authorKey;

public class Authors {

	// -- begin repository and lookup caches

	private static final Map<String, Author> LOOKUP_CACHE = new ConcurrentHashMap<>();
	private static final Set<String> NON_AUTO_ALIASES = ConcurrentHashMap.newKeySet();

	private static AuthorRepository repository = null;

	// -- end repository and lookup caches

	// -- begin name cleaning expressions

	private static final Pattern EMAIL = Pattern.compile(
		"((e)?mail(to)?\\s?:)?(-? ?)?\\(?<?([A-Za-z0-9_.-]+@[A-Za-z0-9]+\\.[A-Za-z0-9.]+)>?\\)?",
		Pattern.CASE_INSENSITIVE); // excessively simple, intentionally
	private static final Pattern URL = Pattern.compile(
		"(-? ?)?\\(?((https?://)?(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-zA-Z0-9()]{2,6}\\b([-a-zA-Z0-9()!@:%_+.~#?&/=]*))\\)?",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern DATE = Pattern.compile("\\d{1,4}[/-]\\d{1,2}[/-]\\d{1,4}");
	private static final Pattern BY = Pattern.compile("^((made|created|done).+)?\\s?(by\\s?:?\\s)",
													  Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern SOMETHING_BY = Pattern.compile("(\\s+([-*( ]+)?([A-Z0-9\\s]+)?conv(^\\s+)\\sby:?\\s+)",
																Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern CONVERTED = Pattern.compile("(([-A-Za-z(]+?|, )conv[^\\s]+)(\\s)?(by:?\\s?)?",
															 Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern IMPORTED = Pattern.compile("\\s(\\*)?imported.*(\\*)?", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern MODIFIED = Pattern.compile("((port|mod)([^\\s]+)?)\\s(by:?\\s?)?",
															Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern EDITED = Pattern.compile("((edit|fix)([^\\s]+)?)\\s(by:?\\s)?",
														  Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern ORIGINAL = Pattern.compile("(origina([^\\s]+)?)\\s(made\\s)?(by:?\\s)?",
															Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_END = Pattern.compile("^[`'\"](.*)[`'\"]$");

	// -- end name cleaning expressions

	static final Pattern AKA = Pattern.compile("(.*)\\s+a\\.?k\\.?a\\.?:?\\s+?(.*)", Pattern.CASE_INSENSITIVE);
	static final Pattern HANDLE = Pattern.compile("(.*)\\s+([`'(\"]([^`^'^)^\"]+)[`')\"])\\s+?(.*)", Pattern.CASE_INSENSITIVE);
	static final Pattern HANDLE_AFTER = Pattern.compile("^(.*)\\s+([`'(\"]([^`^'^)^\"]+)[`')\"])$", Pattern.CASE_INSENSITIVE);

	private Authors() {}

	public static void setRepository(AuthorRepository repo, Path authorsPath) {
		if (repo == null) throw new IllegalArgumentException("Author repository cannot be null");

		repository = repo;

		/*
		 Also populate the auto-alias blocklist
		 */
		final Path exclude = authorsPath.resolve("no-auto-alias.txt");
		if (Files.exists(exclude)) {
			try {
				NON_AUTO_ALIASES.addAll(Files.readAllLines(exclude).stream()
											 .filter(name -> !name.isBlank() && !name.trim().startsWith("#"))
											 .map(Util::normalised)
											 .map(String::toLowerCase)
											 .collect(Collectors.toSet()));
			} catch (IOException e) {
				throw new RuntimeException("Failed to load non-alias names from file " + exclude, e);
			}
		}
	}

	public static Author byName(String name) {
		if (name == null || name.isBlank() || name.equalsIgnoreCase(AuthorRepository.UNKNOWN.name)) return AuthorRepository.UNKNOWN;
		if (name.equalsIgnoreCase(AuthorRepository.VARIOUS.name)) return AuthorRepository.VARIOUS;

		return LOOKUP_CACHE.computeIfAbsent(authorKey(name), n -> {
			Author maybe = repository.byName(name);
			if (maybe != null) return maybe;

			String cleanName = cleanName(name);
			return repository.byName(cleanName);
		});
	}

	public static Contributors contributors(String name) {
		if (name == null || name.isBlank() || name.equalsIgnoreCase(AuthorRepository.UNKNOWN.name)) return null;
		if (name.equalsIgnoreCase(AuthorRepository.VARIOUS.name)) return null;
		if (noAlias(name)) return null;

		Contributors contributors = new Contributors(name);
		if (contributors.modifiedBy.isEmpty() && contributors.contributors.isEmpty()) return null;

		return contributors;
	}

	/**
	 * Checks if an author is a real person (not null, not UNKNOWN, not VARIOUS).
	 */
	public static boolean isSomeone(Author author) {
		return author != null
			   && author != AuthorRepository.UNKNOWN
			   && author != AuthorRepository.VARIOUS;
	}

	public static boolean isSomeone(String name) {
		return name != null
			   && !name.isBlank()
			   && !name.strip().equalsIgnoreCase(AuthorRepository.UNKNOWN.name)
			   && !name.strip().equalsIgnoreCase(AuthorRepository.VARIOUS.name);
	}

	public static boolean noAlias(String name) {
		return NON_AUTO_ALIASES.contains(Util.normalised(name).toLowerCase());
	}

	/**
	 * Populates the provided AuthorRepository with authors extracted from the provided repositories.
	 */
	public static void autoPopRepository(AuthorRepository authorRepo, SimpleAddonRepository content, GameTypeRepository gameTypes,
										 ManagedContentRepository managed) {
		// TODO - map pack content authors, gametype credits
		Stream.concat(Stream.concat(content.all(false).stream(),
									gameTypes.all().stream()), managed.all().stream())
			  .map(ContentEntity::author)
			  .filter(Objects::nonNull)
			  .filter(Authors::isSomeone)
			  .distinct()
			  .sorted()
			  .sorted(Comparator.comparingInt(String::length).reversed())
			  .flatMap(name -> Contributors.names(name).stream()).distinct()
			  .forEach(a -> addToRepository(a, authorRepo));
	}

	public static void addToRepository(String name, AuthorRepository repo) {
		// unknown or various authors, nothing to do
		if (name == null || name.isBlank()) return;
		if (name.equalsIgnoreCase(AuthorRepository.VARIOUS.slug())) return;

		// already known, nothing to do
		if (byName(name) != null) return;

		String normalised = Util.normalised(name).replaceAll("\\s+?", " ").strip();

		if (MODIFIED.matcher(normalised).find()) return;
		if (EDITED.matcher(normalised).find()) return;
		if (ORIGINAL.matcher(normalised).find()) return;
		if (IMPORTED.matcher(normalised).find()) return;
		if (CONVERTED.matcher(normalised).find()) return;
		if (SOMETHING_BY.matcher(normalised).find()) return;

		// skip things with possibly multiple authors
		if (normalised.contains(",") || normalised.contains("&") || normalised.contains(" + ") ||
			normalised.toLowerCase().contains(" and ")) return;

		// FIXME this author should still be recorded
		if (noAlias(normalised)) return;

		Author putAuthor;

		Matcher aka = AKA.matcher(name);
		Matcher handle = HANDLE.matcher(name);
		Matcher handleAfter = HANDLE_AFTER.matcher(name);

		if (aka.matches()) {
			// we'll record the name as well as the "aka" alias, both mapped to the pre-aka name
			String aliased = aka.group(1).strip();
			String realname = aka.group(2).strip();

			// FIXME these authors still need to fall through to final else condition
			if (noAlias(aliased)) return;
			if (noAlias(realname)) return;

			if (byName(aliased) != null) return;
			Author maybeAuthor = byName(realname);
			if (maybeAuthor != null) {
				maybeAuthor.aliases.addAll(Set.of(aliased, realname, name.strip()));
				putAuthor = maybeAuthor;
			} else {
				putAuthor = new Author(cleanName(name), aliased, realname, name.strip());
			}

			return;
		} else if (handle.matches()) {
			String aliased = handle.group(3).strip();
			String realname = handle.group(1).strip() + " " + handle.group(4).strip();

			if (noAlias(aliased)) return;
			if (noAlias(realname)) return;

			if (byName(aliased) != null) return;
			Author maybeAuthor = byName(realname);
			if (maybeAuthor != null) {
				maybeAuthor.aliases.addAll(Set.of(aliased, realname, name.strip()));
				putAuthor = maybeAuthor;
			} else {
				putAuthor = new Author(cleanName(name), aliased, realname, name.strip());
			}
		} else if (handleAfter.matches()) {
			String aliased = handleAfter.group(3).strip();
			String realname = handleAfter.group(1).strip();

			if (noAlias(aliased)) return;
			if (noAlias(realname)) return;

			if (byName(aliased) != null) return;
			Author maybeAuthor = byName(realname);
			if (maybeAuthor != null) {
				maybeAuthor.aliases.addAll(Set.of(aliased, realname, name.strip()));
				putAuthor = maybeAuthor;
			} else {
				putAuthor = new Author(cleanName(name), aliased, realname, name.strip());
			}
		} else {
			String cleanName = cleanName(name);
			if (cleanName.isBlank() || !cleanName.matches(".*[a-zA-Z].*")) return;

			putAuthor = new Author(cleanName);
			if (!cleanName.equalsIgnoreCase(name.strip())) putAuthor.aliases.add(name.strip());
		}

		try {
			repo.put(putAuthor, true);
		} catch (IOException e) {
			throw new RuntimeException("Failed to add author to repository: " + name, e);
		}
	}

	/**
	 * Create a presentable representation of an author name, with various
	 * elements like URLs and email addresses stripped.
	 */
	private static String cleanName(String author) {
		if (author.isBlank()) return "Unknown";

		String noFullstop = author.replaceAll("(\\.)$", "");

		String noDate = DATE.matcher(noFullstop).replaceAll("");

		String noQuote = noDate;
		if (START_END.matcher(noDate).find()) {
			noQuote = START_END.matcher(noDate).replaceFirst("$1");
		}

		String noEmail = EMAIL.matcher(noQuote).replaceAll("");
		if (noEmail.isBlank() || noEmail.length() < 3) {
			// if the entire author string seems to be an email address, use the username component
			if (noQuote.indexOf('@') > 0 && noQuote.length() > 3) noEmail = noQuote.substring(0, noQuote.indexOf('@'));
			else noEmail = noQuote;
		}

		String noUrl = URL.matcher(noEmail).replaceAll("");
		if (noUrl.isBlank() || noUrl.length() < 3) noUrl = noEmail;

		String noConverted = CONVERTED.matcher(noUrl).replaceAll("");
		if (noConverted.isBlank()) noConverted = noUrl;

		String noImport = IMPORTED.matcher(noConverted).replaceAll("");
		if (noImport.isBlank()) noImport = noConverted;

		String noModified = MODIFIED.matcher(noImport).replaceAll("");
		if (noModified.isBlank()) noModified = noImport;

		String noMadeBy = BY.matcher(noModified).replaceAll("");
		if (noMadeBy.isBlank()) noMadeBy = noModified;

		String noEditBy = EDITED.matcher(noMadeBy).replaceAll("");
		if (noEditBy.isBlank()) noEditBy = noMadeBy;

		return noEditBy.strip().replaceAll("[.,]+$", "").strip();
	}

}
