package org.unrealarchive.content;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Contributors {

	public final String authorString;
	public final Author originalAuthor;
	public final Set<Author> contributors = new HashSet<>();
	public final Set<Author> modifiedBy = new HashSet<>();

	/**
	 * Used to strip dates from input if possible.
	 */
	private static final Pattern DATE = Pattern.compile("\\d{1,4}[/-]\\d{1,2}[/-]\\d{1,4}");

	/**
	 * Inputs:
	 * - Bob and Joe
	 * - Bob & Joe
	 * - Bob, Joe
	 */
	private static final Pattern AUTHOR_SPLIT = Pattern.compile("((\\s+(and|&))\\s+|\\s+\\+\\s+|,\\s?|\\s?/\\s?)",
																Pattern.CASE_INSENSITIVE);

	/**
	 * Inputs:
	 * - Bob, modified by Joe
	 * - Bob, edit by Joe
	 * - Bob, cool version by Joe
	 * - Bob -- cool conversion by Joe
	 * - Bob - my conv by Joe Soap
	 * - Bob -- another version: Joe
	 * - Bob - remade by Joe
	 * - Bob - Rad Edit by Joe
	 */
	private static final Pattern MODIFIED_SPLIT = Pattern.compile(
		"(,?\\s+|/)((-+|[^A-Za-z\\s])\\s+)?((\\S+\\s+)?(edit(ed)?|mod(ified|ifications|ded|s))|((\\S+\\s+)?((conv(er(sion|ted))?)|(version)|(remix(ed)?)))|(rema([kd])e)|(revamp(ed)?))[\\s->:]?\\s+(by:?\\s+)?",
		Pattern.CASE_INSENSITIVE
	);

	/**
	 * Inputs:
	 * - Bob (Original), Joe (Edited)
	 * - Bob (original), Joe (cool version)
	 * Out:
	 * - Group 1: original
	 * - Group 2: editor
	 */
	private static final Pattern MODIFIED_MATCH_1 = Pattern.compile("(.+)\\s+\\(original\\),?\\s+(.+)\\(.+\\)",
																	Pattern.CASE_INSENSITIVE);

	/**
	 * Inputs:
	 * - Bob (original Joe)
	 * - Bob(originally by Joe)
	 * Out:
	 * - Group 6: original
	 * - Group 1: editor
	 */
	private static final Pattern MODIFIED_MATCH_2 = Pattern.compile("(.+)(\\s+)?\\(origin(al(ly)?)?\\s+(by:?\\s+)?(.+)\\)",
																	Pattern.CASE_INSENSITIVE);

	/**
	 * Inputs:
	 * - Bob, original by Joe
	 * - Bob original concept by Joe
	 * Out:
	 * - Group 5: original
	 * - Group 1: editor
	 */
	private static final Pattern MODIFIED_MATCH_3 = Pattern.compile("(.+?),?\\s+?original(ly)?\\s+((.+\\s)?by:?\\s+)(.+)",
																	Pattern.CASE_INSENSITIVE);

	/**
	 * Inputs:
	 * - Bob/ported to UT by Joe
	 * - Bob imported by Joe
	 * - Bob - port by Joe
	 * - Bob *Imported to UT by Joe*
	 * Out:
	 * - Group 1: original
	 * - Group 9: editor
	 */
	private static final Pattern MODIFIED_MATCH_7 = Pattern.compile(
		"(.+?)(((\\s+[/-]+)\\s+)|(/))?\\*?((im)?port(ed)?).+?by:?\\s+?([^*]+)\\*?",
		Pattern.CASE_INSENSITIVE);

	/**
	 * Inputs:
	 * - Bob, (Cool Version By Joe)
	 * - Bob (converted by Joe)
	 * - Bob *converted from A to B by Joe*
	 * Out:
	 * - Group 1: original
	 * - Group 2: editor
	 */
	private static final Pattern MODIFIED_MATCH_4 = Pattern.compile("(.+)[(\\[*].+by:?\\s+(.+)[)\\]*]",
																	Pattern.CASE_INSENSITIVE);

	/**
	 * Inputs: Bob made · Joe edited
	 * Out:
	 * - Group 1: original
	 * - Group 2: editor
	 */
	private static final Pattern MODIFIED_MATCH_5 = Pattern.compile("^(.+)\\s+made\\s+[^A-Z](.+)\\s(edited|modified)$",
																	Pattern.CASE_INSENSITIVE);

	/**
	 * Inputs:
	 * - Bob, Changed by Joe
	 * - Bob, (edited by Joe)
	 * Out:
	 * - Group 1: original
	 * - Group 4: editor
	 */
	private static final Pattern MODIFIED_MATCH_6 = Pattern.compile("([^,]+)(,?\\s+)(-+\\s+)?[(|\\[]?.+by:?\\s+([^)]+)[)|\\]]?",
																	Pattern.CASE_INSENSITIVE);

	public Contributors(String authorString) {
		this.authorString = authorString;

		String cleaned = DATE.matcher(authorString).replaceAll("");

		ModBy modBy = parseEditors(cleaned);
		if (modBy != null) {
			Set<Author> originals = parseAuthors(modBy.original());
			if (originals.size() > 1) {
				this.contributors.addAll(originals);
				this.originalAuthor = null;
			} else this.originalAuthor = originals.stream().findFirst().orElse(null);
			this.modifiedBy.addAll(parseAuthors(modBy.editor()));
		} else {
			this.originalAuthor = null;

			Set<Author> authors = parseAuthors(cleaned);
			if (authors.size() > 1) this.contributors.addAll(authors);
		}
	}

	public static Set<String> names(String authorString) {
		if (Authors.noAlias(authorString)) return Set.of(authorString);

		Set<String> names = new HashSet<>();

		String[] s = MODIFIED_SPLIT.split(authorString);
		if (s.length > 1) {
			names.addAll(Stream.of(s).filter(n -> !n.isBlank()).collect(Collectors.toSet()));
		} else {
			Matcher m;
			if ((m = MODIFIED_MATCH_1.matcher(authorString)).find()) {
				names.add(m.group(1));
				names.add(m.group(2));
			} else if ((m = MODIFIED_MATCH_2.matcher(authorString)).find()) {
				names.add(m.group(6));
				names.add(m.group(1));
			} else if ((m = MODIFIED_MATCH_3.matcher(authorString)).find()) {
				names.add(m.group(5));
				names.add(m.group(1));
			} else if ((m = MODIFIED_MATCH_7.matcher(authorString)).find()) {
				names.add(m.group(1));
				names.add(m.group(9));
			} else if ((m = MODIFIED_MATCH_4.matcher(authorString)).find()) {
				names.add(m.group(1));
				names.add(m.group(2));
			} else if ((m = MODIFIED_MATCH_5.matcher(authorString)).find()) {
				names.add(m.group(1));
				names.add(m.group(2));
			} else if ((m = MODIFIED_MATCH_6.matcher(authorString)).find()) {
				names.add(m.group(1));
				names.add(m.group(4));
			}
		}

		if (names.isEmpty()) names.add(authorString);

		final Set<String> finalNames = new HashSet<>();

		names
			.forEach(a -> {
				String[] b = AUTHOR_SPLIT.split(a);
				if (b.length == 1) {
					finalNames.add(a);
				} else {
					finalNames.addAll(Stream.of(b).filter(n -> !n.isBlank()).collect(Collectors.toSet()));
				}
			});

		return finalNames.stream()
						 .map(String::strip)
						 .map(Contributors::cleanup)
						 .filter(n -> !n.isBlank())
						 .collect(Collectors.toSet());
	}

	private Set<Author> parseAuthors(String a) {
		if (Authors.noAlias(a)) return Set.of(author(a));

		String[] s = AUTHOR_SPLIT.split(a);
		if (s.length == 1) return Set.of(author(a));

		Set<Author> authors = new HashSet<>();
		for (String string : s) if (!string.isBlank()) authors.add(author(string));

		return authors;
	}

	private ModBy parseEditors(String a) {
		String[] s = MODIFIED_SPLIT.split(a);
		if (s.length > 1) return new ModBy(s[0], s[1]);

		Matcher m = MODIFIED_MATCH_1.matcher(a);
		if (m.find()) return new ModBy(m.group(1), m.group(2));
		Matcher m2 = MODIFIED_MATCH_2.matcher(a);
		if (m2.find()) return new ModBy(m2.group(6), m2.group(1));
		Matcher m3 = MODIFIED_MATCH_3.matcher(a);
		if (m3.find()) return new ModBy(m3.group(5), m3.group(1));
		Matcher m7 = MODIFIED_MATCH_7.matcher(a);
		if (m7.find()) return new ModBy(m7.group(1), m7.group(9));
		Matcher m4 = MODIFIED_MATCH_4.matcher(a);
		if (m4.find()) return new ModBy(m4.group(1), m4.group(2));
		Matcher m5 = MODIFIED_MATCH_5.matcher(a);
		if (m5.find()) return new ModBy(m5.group(1), m5.group(2));
		Matcher m6 = MODIFIED_MATCH_6.matcher(a);
		if (m6.find()) return new ModBy(m6.group(1), m6.group(4));

		return null;
	}

	private static String cleanup(String name) {
		return name.strip()
				   .replaceAll("(?i)^(conv(er(sion|ted))?|original(ly)?|made|map|done|port(ed)?|edit(ed)?|remix(ed)?|modified|and|by)(\\s+by)?:?", "")
				   .replaceAll("(?i)(conv(er(sion|ted))?|original(ly)?|made|map|done|port(ed)?|edit(ed)?|remix(ed)?|modified|and|by)(\\s+by)?:?$", "")
				   .strip();
	}

	private Author author(String name) {
		String s = cleanup(name);
		Author author = Authors.byName(s);
		if (author == null) author = new Author(s);
		return author;
	}

	@Override
	public String toString() {
		return String.format("Contributors [authorString=%s, originalAuthor=%s, contributors=[%s], modifiedBy=[%s]]",
							 authorString, originalAuthor == null ? null : originalAuthor.name,
							 contributors.stream().map(a -> a.name).collect(Collectors.joining(" | ")),
							 modifiedBy.stream().map(a -> a.name).collect(Collectors.joining(" | ")));
	}

	private record ModBy(String original, String editor) {}
}
