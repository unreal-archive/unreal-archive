package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthorNames {

	public static Optional<AuthorNames> instance = Optional.empty();

	private static final Pattern EMAIL = Pattern.compile(
		"(-? ?)?\\(?([A-Za-z0-9.-]+@[^.]+\\.[A-Za-z]+)\\)?"); // excessively simple, intentionally
	private static final Pattern URL = Pattern.compile(
		"(-? ?)?\\(?((https?://)?(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-zA-Z0-9()]{2,6}\\b([-a-zA-Z0-9()!@:%_+.~#?&/=]*))\\)?"
	);
	private static final Pattern BY = Pattern.compile("(([Mm]ade).+)?\\s?([Bb]y)");
	private static final Pattern CONVERTED = Pattern.compile("([-A-Za-z]+?[Cc]onver[^\\s]+)\\s([Bb]y)?");
	private static final Pattern IMPORTED = Pattern.compile("\\s(\\*)?[Ii]mported.*(\\*)?");

	private static final Pattern AKA = Pattern.compile("(.*)\\s+a\\.?k\\.?a\\.?:?\\s+?(.*)", Pattern.CASE_INSENSITIVE);
	private static final Pattern HANDLE = Pattern.compile("(.*)\\s+(['\"]([^'^\"]+)['\"])\\s+?(.*)", Pattern.CASE_INSENSITIVE);

	private final Map<String, String> aliases;

	public AuthorNames(Path aliasPath) throws IOException {
		this.aliases = new HashMap<>();
		Files.walk(aliasPath, FileVisitOption.FOLLOW_LINKS)
			 .filter(p -> !Files.isDirectory(p))
			 .filter(p -> Util.extension(p).equalsIgnoreCase("txt"))
			 .forEach(path -> {
				 try {
					 List<String> names = Files.readAllLines(path);
					 for (String name : names.subList(1, names.size())) {
						 aliases.put(name.toLowerCase(), names.get(0));
					 }
				 } catch (IOException e) {
					 throw new RuntimeException("Failed to process names from file " + path, e);
				 }
			 });
	}

	public AuthorNames(Map<String, String> aliases) {
		this.aliases = aliases;
	}

	public String cleanName(String author) {
		String aliased = aliases.getOrDefault(author.toLowerCase().strip(), author).strip();

		if (aliased.equalsIgnoreCase(author.strip())) {
			Matcher aka = AKA.matcher(author);
			if (aka.matches()) {
				// we'll record the name as well as the "aka" alias, both mapped to the pre-aka name
				aliases.put(author.strip().toLowerCase(), aka.group(1));
				aliases.put(aka.group(2).strip().toLowerCase(), aka.group(1));
				aliased = aka.group(1).strip();
			} else {
				Matcher handle = HANDLE.matcher(author);
				if (handle.matches()) {
					// we'll record the full name, aliased by both the handle and real name
					aliases.put((handle.group(1) + " " + handle.group(4)).strip().toLowerCase(), author.strip());
					aliases.put(handle.group(3).strip().toLowerCase(), author.strip());
				}
			}
		}

		String noEmail = EMAIL.matcher(aliased).replaceAll("");
		if (noEmail.isBlank() || noEmail.length() < 3) {
			if (aliased.indexOf('@') > 0 && aliased.length() > 3) noEmail = aliased.substring(0, aliased.indexOf('@'));
			else noEmail = aliased;
		}

		String noUrl = URL.matcher(noEmail).replaceAll("");
		if (noUrl.isBlank() || noUrl.length() < 3) noUrl = noEmail;

		String noMadeBy = BY.matcher(noUrl).replaceAll("");
		if (noMadeBy.isBlank()) noMadeBy = noUrl;

		String noConverted = CONVERTED.matcher(noMadeBy).replaceAll("");
		if (noConverted.isBlank()) noConverted = noMadeBy;

		String noImport = IMPORTED.matcher(noConverted).replaceAll("");
		if (noImport.isBlank()) noImport = noConverted;

		return aliases.getOrDefault(noImport.toLowerCase().strip(), noImport).strip();
	}

	public static String nameFor(String name) {
		return instance.map(e -> e.cleanName(name)).orElse(name);
	}
}
