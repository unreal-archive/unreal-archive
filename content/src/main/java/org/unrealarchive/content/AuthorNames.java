package org.unrealarchive.content;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.unrealarchive.common.Util;

public class AuthorNames {

	public static AuthorNames instance = null;

	private static final Pattern EMAIL = Pattern.compile(
		"(-? ?)?\\(?([A-Za-z0-9.-]+@[A-Za-z0-9]+\\.[A-Za-z0-9.]+)\\)?"); // excessively simple, intentionally
	private static final Pattern URL = Pattern.compile(
		"(-? ?)?\\(?((https?://)?(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-zA-Z0-9()]{2,6}\\b([-a-zA-Z0-9()!@:%_+.~#?&/=]*))\\)?"
	);
	private static final Pattern BY = Pattern.compile("^(([Mm]ade).+)?\\s?([Bb]y\\s)");
	private static final Pattern CONVERTED = Pattern.compile("(([-A-Za-z]+?|, )[Cc]onver[^\\s]+)(\\s)?([Bb]y\\s?)?");
	private static final Pattern IMPORTED = Pattern.compile("\\s(\\*)?[Ii]mported.*(\\*)?");
	private static final Pattern MODIFIED = Pattern.compile("([Mm]odifi[^\\s]+)\\s([Bb]y\\s?)?");
	private static final Pattern EDITED = Pattern.compile("^([Ee]dit(ed)?)\\s([Bb]y\\s)?");

	private static final Pattern AKA = Pattern.compile("(.*)\\s+a\\.?k\\.?a\\.?:?\\s+?(.*)", Pattern.CASE_INSENSITIVE);
	private static final Pattern HANDLE = Pattern.compile("(.*)\\s+(['\"]([^'^\"]+)['\"])\\s+?(.*)", Pattern.CASE_INSENSITIVE);

	private static final Pattern START_END = Pattern.compile("^['\"](.*)['\"]$");

	private final Map<String, String> aliases;
	private final Set<String> nonAutoAliases;

	public AuthorNames(Path aliasPath) throws IOException {
		this.aliases = new HashMap<>();
		this.nonAutoAliases = new HashSet<>();
		try (Stream<Path> fileStream = Files.walk(aliasPath, 2, FileVisitOption.FOLLOW_LINKS)) {
			fileStream.filter(p -> !Files.isDirectory(p))
					  .filter(p -> Util.extension(p).equalsIgnoreCase("txt"))
					  .filter(p -> !Util.fileName(p).equalsIgnoreCase("exclude.txt"))
					  .forEach(path -> {
						  try {
							  List<String> names = Files.readAllLines(path);
							  for (String name : names.subList(1, names.size())) {
								  if (name.isBlank() || name.trim().startsWith("#")) continue;
								  aliases.put(name.toLowerCase().strip(), names.get(0).strip());
							  }
						  } catch (IOException e) {
							  throw new RuntimeException("Failed to process names from file " + path, e);
						  }
					  });
		}

		final Path exclude = aliasPath.resolve("no-auto-alias").resolve("exclude.txt");
		if (Files.exists(exclude)) {
			try {
				nonAutoAliases.addAll(Files.readAllLines(exclude).stream()
										   .filter(name -> !name.isBlank() && !name.trim().startsWith("#"))
										   .map(String::toLowerCase)
										   .collect(Collectors.toSet()));
			} catch (IOException e) {
				throw new RuntimeException("Failed to load non-alias names from file " + exclude, e);
			}
		}
	}

	public AuthorNames(Map<String, String> aliases, Set<String> nonAutoAliases) {
		this.aliases = aliases;
		this.nonAutoAliases = nonAutoAliases;
	}

	public int aliasCount() {
		return aliases.size();
	}

	/**
	 * Can be used to auto-generate aliases for authors, before calling
	 * {@link #cleanName(String)}.
	 */
	public void maybeAutoAlias(String author) {
		if (author.isBlank()) return;

		if (MODIFIED.matcher(author).find()) return;
		if (EDITED.matcher(author).find()) return;
		if (IMPORTED.matcher(author).find()) return;
		if (CONVERTED.matcher(author).find()) return;

		if (nonAutoAliases.contains(author.toLowerCase().strip())) return;
		if (aliases.containsKey(author.toLowerCase().strip())) return;

		String aliased = aliases.getOrDefault(author.toLowerCase().strip(), author).strip();
		if (nonAutoAliases.contains(aliased)) return;

		if (aliased.equalsIgnoreCase(author.strip())) {
			Matcher aka = AKA.matcher(author);
			if (aka.matches()) {
				// we'll record the name as well as the "aka" alias, both mapped to the pre-aka name
				aliased = aka.group(1).strip();
				if (nonAutoAliases.contains(aliased.toLowerCase())) return;
				if (aliases.containsKey(aliased.toLowerCase())) return;
				aliases.put(author.strip().toLowerCase(), aliased);
				aliases.put(aka.group(2).strip().toLowerCase(), aliased);
			} else {
				Matcher handle = HANDLE.matcher(author);
				if (handle.matches()) {
					aliased = handle.group(3).strip();
					// we'll record the full name, aliased by both the handle and real name
					if (nonAutoAliases.contains(aliased.toLowerCase())) return;
					if (aliases.containsKey(aliased.toLowerCase())) return;
					aliases.put((handle.group(1) + " " + handle.group(4)).strip().toLowerCase(), author.strip());
					aliases.put(handle.group(3).strip().toLowerCase(), author.strip());
				}
			}
		}
	}

	/**
	 * Create a presentable representation of an author name, possibly aliased to some
	 * common name, and with various elements like URLs and email addresses stripped.
	 */
	public String cleanName(String author) {
		if (author.isBlank()) return "Unknown";

		if (nonAutoAliases.contains(author.toLowerCase())) return author;

		String aliased = aliases.getOrDefault(author.toLowerCase().strip(), author).strip();
		
		String noQuote = aliased;
		if (START_END.matcher(aliased).find()) {
			noQuote = START_END.matcher(aliased).replaceFirst("$1");
		}

		String noEmail = EMAIL.matcher(noQuote).replaceAll("");
		if (noEmail.isBlank() || noEmail.length() < 3) {
			// if the entire author string seems to be an email address, use the username
			if (noQuote.indexOf('@') > 0 && noQuote.length() > 3) noEmail = noQuote.substring(0, noQuote.indexOf('@'));
			else noEmail = noQuote;
		}

		String noUrl = URL.matcher(noEmail).replaceAll("");
		if (noUrl.isBlank() || noUrl.length() < 3) noUrl = noEmail;

		String noConverted = CONVERTED.matcher(noUrl).replaceAll("");
		if (noConverted.isBlank()) noConverted = noUrl;

		String noImport = IMPORTED.matcher(noConverted).replaceAll("");
		if (noImport.isBlank()) noImport = noConverted;

		String noMadeBy = BY.matcher(noImport).replaceAll("");
		if (noMadeBy.isBlank()) noMadeBy = noImport;

		String noEditBy = EDITED.matcher(noMadeBy).replaceAll("");
		if (noEditBy.isBlank()) noEditBy = noMadeBy;

		return aliases.getOrDefault(noEditBy.toLowerCase().strip(), noEditBy).strip();
	}

	public static String nameFor(String author) {
		if (instance == null) return author;
		else return instance.cleanName(author);
	}
}
