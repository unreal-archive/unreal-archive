package org.unrealarchive.indexing;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unrealarchive.content.Authors;

import static org.unrealarchive.content.addons.Addon.UNKNOWN;

public class AuthorNameUtils {

	/**
	 * The keyword must be a word of its own - "STANDBY", "nearby" and "Derby" are not attributions.
	 * <p>
	 * The name must also end the line (the expression is used with {@link Matcher#matches()}).
	 * That requirement is load-bearing: a real attribution ends its line, and relaxing this to
	 * allow trailing commentary floods the candidate set with mid-sentence prose whose "by"
	 * happens to sit early on the line, displacing genuine trailing attributions.
	 */
	public static final Pattern AUTHOR_MATCH = Pattern.compile("(.+)?\\b(authors?|by)\\b(\\(s\\))?([\\s:]+)?(.{4,35})(\\s+)?",
															   Pattern.CASE_INSENSITIVE);
	/**
	 * A readme lays its metadata out in columns, so padding or an ellipsis run ends the name
	 * ("Adapt            adaptadapt", "Luger...........great stuff!!").
	 * <p>
	 * Three or more spaces, since two may be a typo inside a real name ("Cheney  Lansard").
	 */
	private static final Pattern COLUMN = Pattern.compile("(?<=\\S)\\s{3,}|\\t|(?<=\\w)\\.{3,}(?=\\w)");
	/**
	 * A column-separated tail which is more attribution must survive: a contribution ("OZ   *
	 * Modified by RUSH*", "DGunman   Original UT99:  (ASC)Djinn") or a co-author, which announces
	 * itself with a conjunction ("...$M]![L3   /  ù)v(indz-$L@Y3R").
	 */
	private static final Pattern CONTRIBUTION = Pattern.compile(
		"^\\s*[/&+]|\\b(and|modified|imported|converted|conversion|original|remix(ed)?|edit(ed)?|by)\\b",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern COPYRIGHT = Pattern.compile("\\(c\\)\\s*\\d{2,4}|\u00a9\\s*\\d{2,4}", Pattern.CASE_INSENSITIVE);
	/**
	 * An unterminated HTML tag is a leaked markup fragment ("&lt;A", "&lt;font color=..."), but only
	 * for recognised tag names - "&lt;Quest-, Paul Fahss, ..." and "k&lt;doubLe" are handles.
	 */
	private static final Pattern UNTERMINATED_TAG = Pattern.compile(
		"</?(a|b|i|p|br|img|font|td|tr|table|center|span|div|body|html)\\b[^>]*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern LEADING_LABEL = Pattern.compile("^((made|created|done|compiled)\\s+)?(by|authors?)\\s*:?\\s+",
																 Pattern.CASE_INSENSITIVE);
	private static final Pattern CLAUSE = Pattern.compile(
		",\\s*(for|using|with|to|in|from|so|if|feel|please|originally|mostly|while|when|after|before|though|but)\\b.*$",
		Pattern.CASE_INSENSITIVE);
	/**
	 * Characters a stylised handle decorates itself with. Brackets are absent deliberately: those are
	 * balanced instead, so a matched pair is never mistaken for decoration ("ScorpioN &lt;No Name&gt;").
	 */
	private static final String DECORATION = "-=~*_:;!.,|/\\^?%\u00a9\u00b0\u00b1#+'\"`";
	/**
	 * The decoration a name may legitimately open with, being {@link #DECORATION} intersected with
	 * {@link #NAME_START}. A flush leading run of anything else ("*Britton Wesley", "\"Revelation")
	 * is debris, where "-GhostXC" and "=iNi=bRiaN=-" wear theirs as part of the name.
	 */
	private static final String NAME_LEADING_DECORATION = ".-=";
	/**
	 * Sentence punctuation, which never wraps a name, so it always goes ("Sam Plate." -> "Sam Plate").
	 */
	private static final String PUNCTUATION = ".,";
	/**
	 * A single letter closed by a full stop is an initial or an acronym piece ("H.O.L.", "Ryan F.").
	 */
	private static final Pattern INITIAL = Pattern.compile("(^|[\\s.])\\p{L}\\.$");
	/**
	 * Brackets, which are balanced rather than treated as decoration.
	 */
	private static final String BRACKETS = "()[]<>{}";
	/**
	 * Exactly two spaces is a typo inside a name ("Rolf  Baumann"), not the column padding
	 * {@link #COLUMN} cuts, and normalising it lets the value merge with its correctly spaced twin.
	 */
	private static final Pattern DOUBLE_SPACE = Pattern.compile("(?<=\\S) {2}(?=\\S)");
	private static final Pattern DATE_TAIL = Pattern.compile(
		"[\\s,.\\-\u00b4]+((jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*([/-][a-z]+)?[\\s,]*)?"
		+ "(\\d{1,2}[/-]\\d{2,4}|(19|20)\\d\\d)$",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern MONTH_TAIL = Pattern.compile(
		"[\\s,.\\-]+\\d{1,2}\\s+(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern QUOTED = Pattern.compile("^[\"'`](.+)[\"'`]$");
	private static final String LEADING_SEPARATORS = " \t,;:";
	private static final Pattern SOME_NAME = Pattern.compile("[\\p{L}\\p{N}]");
	private static final Pattern NAME_START = Pattern.compile("^[A-Za-z0-9(\\[<.\\-=]");

	/**
	 * A value which is entirely one of these holds no name at all.
	 */
	private static final Set<String> NOT_A_NAME = Set.of(
		"etc", "others", "other", "me", "myself", "above", "below", "default", "unknown", "various", "him", "her",
		"them", "anyone", "someone", "yourself", "author", "authors", "the", "email", "e-mail", "info", "information",
		"comments", "none", "n/a", "using", "pressing", "any", "those", "his", "skin", "skins", "name", "names",
		"credit", "credits", "mr", "mrs", "dr", "edit", "edited");

	/**
	 * A lowercase one of these opening the value means what was captured is mid-sentence prose
	 * ("of these skins", "and let me know!") rather than a name - but only when the following word
	 * is also lowercase, since real handles hide behind some of them ("the CreaTurE", "the DJ",
	 * "me: Miceal \"Blaze\" Kelley").
	 */
	private static final Set<String> PROSE_STARTERS = Set.of(
		"of", "and", "for", "or", "a", "an", "me", "us", "him", "her", "them", "my", "your", "his", "its",
		"this", "that", "these", "those", "it", "is", "was", "are", "in", "on", "at", "to", "from", "with",
		"the", "not", "about", "using", "creating", "pressing", "any", "some", "all", "one", "if", "so", "as",
		"i", "we", "you", "they", "he", "she", "but", "just", "also", "thanks", "thanx", "thx", "credit", "credits");

	/**
	 * Beyond this many characters ahead of the keyword the line is prose rather than an attribution;
	 * across the archive genuine attributions sit within the first 50 or so characters.
	 */
	private static final int MAX_AUTHOR_OFFSET = 150;

	private AuthorNameUtils() {}

	public static String findAuthor(List<String> lines) {
		String best = null;
		int bestOffset = Integer.MAX_VALUE;

		for (String s : lines) {
			// contact details trailing the name would otherwise be captured as part of it, or
			// push the name past the length the expression will match at all
			Matcher m = AUTHOR_MATCH.matcher(Authors.stripContacts(s));
			if (!m.matches() || m.group(5).isBlank()) continue;

			// the nearer the keyword sits to the start of the line, the more the line reads as an
			// attribution rather than as prose which happens to mention an author
			int offset = m.group(1) == null ? 0 : m.group(1).length();
			if (offset > MAX_AUTHOR_OFFSET || offset >= bestOffset) continue;

			String author = authorName(m.group(5));
			if (author == null) continue;

			best = author;
			bestOffset = offset;

			// a labelled line ("Author: ...") cannot be bettered
			if (offset == 0) break;
		}

		return best;
	}

	/**
	 * Reduce a matched author expression to a name, or null if what was captured cannot be one.
	 */
	private static String authorName(String captured) {
		String name = cleanAuthor(captured);
		if (name.equals(UNKNOWN) || !NAME_START.matcher(name).find()) return null;

		// the captured line still carries the "by"/"made by" lead-in and any conversion credit,
		// neither of which belongs in a name, so those come off and the result is cleaned again
		name = cleanAuthor(Authors.cleanName(name));
		return name.equals(UNKNOWN) ? null : name;
	}

	/**
	 * Reduce an author value to just the name, or {@code Unknown} when no name survives.
	 * <p>
	 * The same rules serve a freshly matched name and a value already stored in the index: contact
	 * details, column padding, copyright markers, date tails, one-sided decoration, truncated
	 * parentheticals and markup fragments all come off, and a value holding no name at all
	 * (", etc", "of these skins") reduces to {@code Unknown}.
	 * <p>
	 * Deliberately NOT applied here: {@link Authors#cleanName(String)}'s by/modified/converted
	 * stripping. "X Modified by Y" is contributor information parsed out of the stored string, and
	 * must survive - author extraction strips it from the line it captured instead.
	 */
	public static String cleanAuthor(String author) {
		String a = author.strip().replace("-->", " ");
		a = UNTERMINATED_TAG.matcher(a).replaceAll("");
		a = Authors.stripContacts(a);

		// a value which was entirely a single email address keeps its username part, which may
		// itself still carry a URL ("[www.Heldsite.de]Heldsite@gmx.net")
		if (a.replace(" ", "").length() < 3) {
			String orig = author.strip();
			if (orig.indexOf('@') > 0 && !orig.contains(" ")) a = Authors.stripContacts(orig.substring(0, orig.indexOf('@')));
		}

		// the structural cuts run once; the trims loop to a fixed point, since each can expose
		// more work for the others ("(c) 2000 - Gene Ostrowski -" sheds its marker, then its dash)
		a = CLAUSE.matcher(a).replaceFirst("");
		String[] columns = COLUMN.split(a.strip(), 2);
		if (columns.length < 2 || !CONTRIBUTION.matcher(columns[1]).find()) a = columns[0];
		a = DOUBLE_SPACE.matcher(a).replaceAll(" ");
		a = COPYRIGHT.matcher(a).replaceAll("");

		String prev = null;
		while (!a.equals(prev)) {
			prev = a;
			a = a.strip();
			a = trimLeadingSeparators(a);
			a = LEADING_LABEL.matcher(a).replaceFirst("");
			a = trimLeadingDecoration(a);
			a = trimBracketDebris(a);
			a = trimUnbalancedClosers(a);
			// a truncated trailing parenthetical is cut; a leading clan "(" is part of the name
			if (count(a, '(') > count(a, ')') && a.lastIndexOf('(') > 0) a = a.substring(0, a.lastIndexOf('(')).strip();
			a = DATE_TAIL.matcher(a).replaceFirst("");
			a = MONTH_TAIL.matcher(a).replaceFirst("");
			a = trimTrailingDecoration(a);

			Matcher quoted = QUOTED.matcher(a);
			if (quoted.matches()) a = quoted.group(1).strip();
		}

		if (isNameless(a) || NOT_A_NAME.contains(a.toLowerCase())) return UNKNOWN;

		String[] words = a.split("[\\s:,.;]+", 3);
		if (PROSE_STARTERS.contains(words[0])  // lowercase members only, so this is a case-sensitive test
			&& (words.length < 2 || words[1].isEmpty() || !Character.isUpperCase(words[1].codePointAt(0)))) {
			return UNKNOWN;
		}

		return a;
	}

	/**
	 * True when a value holds no name at all, being decoration all the way down (".:..:", "???").
	 * <p>
	 * Unreadable, but for a value already in the index it is what the author called themselves,
	 * so the caller may prefer to leave it exactly as stored rather than reduce it to unknown.
	 */
	public static boolean isNameless(String value) {
		return SOME_NAME.matcher(value).results().count() < 2;
	}

	/**
	 * Drop trailing decoration, unless it pairs with something earlier in the value.
	 * <p>
	 * A stylised handle wraps itself: "?3rror?", "^~[Lohc]~Opa~^", "=iNi=bRiaN=-", "*NoReMoRsE***",
	 * "... CTF4:-=Musc@t=-". In each the closing decoration also appears further back, so it is part
	 * of the name. Where it does not - "Fira?", "Peter Muller!", "TR==" - it is emphasis or debris.
	 * Whitespace is not considered: it is stripped separately, so "*NoReMoRsE* " cannot masquerade
	 * as a two-character run and take the star with it.
	 */
	private static String trimTrailingDecoration(String a) {
		int end = a.length();
		while (end > 0 && DECORATION.indexOf(a.charAt(end - 1)) >= 0) end--;

		// nothing decorative at the end, or nothing but decoration in the whole value
		if (end == a.length() || end == 0) return a;

		// a full stop closing an initial or an acronym is part of the name, not sentence punctuation
		if (a.endsWith(".") && INITIAL.matcher(a).find()) return a;

		// only a lone full stop or comma is sentence punctuation; a run of them is decoration
		// ("Failsuicide... / Nobody..."), so it answers to the pairing test like anything else
		boolean lone = a.length() - end == 1;

		String head = a.substring(0, end);
		for (int i = end; i < a.length(); i++) {
			char c = a.charAt(i);
			if (lone && PUNCTUATION.indexOf(c) >= 0) continue;
			if (head.indexOf(c) >= 0 || head.indexOf(mirror(c)) >= 0) return a;
		}
		return head.strip();
	}

	/**
	 * Drop leading separators, unless one recurs later and so opens a tag (":SOLO:[EL]Darkchlor1",
	 * ":LoH:Lord McNeiL"). A lone leading separator is capture debris (", etc").
	 */
	private static String trimLeadingSeparators(String a) {
		int start = 0;
		while (start < a.length() && LEADING_SEPARATORS.indexOf(a.charAt(start)) >= 0) start++;
		if (start == 0 || start == a.length()) return a;

		String tail = a.substring(start);
		for (int i = 0; i < start; i++) {
			char c = a.charAt(i);
			if (!Character.isWhitespace(c) && tail.indexOf(c) >= 0) return a;
		}
		return tail;
	}

	/**
	 * Drop a leading decoration run ("- Gene Ostrowski", "= [ IndecisioN ]", "*Britton Wesley"),
	 * unless it recurs later and so belongs to the name ("*NoReMoRsE***").
	 * <p>
	 * Whitespace after the run marks it as a bullet, and only a run of one repeated character is
	 * one: a mixed run is a clan tag (".:..: Remix/Add by gopostal", "-=AoR- ..."), and the tag is
	 * the author. A run flush against the name is instead read through {@link
	 * #NAME_LEADING_DECORATION}, since "-GhostXC" and "=iNi=bRiaN=-" wear theirs legitimately while
	 * a quote or star cannot open a name.
	 */
	private static String trimLeadingDecoration(String a) {
		int start = 0;
		while (start < a.length() && DECORATION.indexOf(a.charAt(start)) >= 0) start++;
		if (start == 0 || start >= a.length()) return a;

		// only a run of one repeated character is decoration; a mixed run is a clan tag, and a
		// stylised letter ("\\\/ertigo" for Vertigo) would lose its first stroke
		if (a.chars().limit(start).distinct().count() > 1) return a;
		// flush against the name, only characters no name opens with can be debris
		if (!Character.isWhitespace(a.charAt(start)) && NAME_LEADING_DECORATION.indexOf(a.charAt(0)) >= 0) return a;

		String tail = a.substring(start).strip();
		for (int i = 0; i < start; i++) {
			char c = a.charAt(i);
			if (tail.indexOf(c) >= 0 || tail.indexOf(mirror(c)) >= 0) return a;
		}
		return tail;
	}

	/**
	 * The mirror of a stroke, which pairs with it the way a bracket does: "\V/ictima" draws one
	 * letter, so the leading stroke is no more debris than a matched bracket is.
	 */
	private static char mirror(char c) {
		return switch (c) {
			case '\\' -> '/';
			case '/' -> '\\';
			default -> c;
		};
	}

	private static String trimUnbalancedClosers(String a) {
		while (!a.isEmpty()) {
			char last = a.charAt(a.length() - 1);
			char open = last == ')' ? '(' : last == ']' ? '[' : last == '>' ? '<' : 0;
			if (open == 0 || count(a, last) <= count(a, open)) break;
			a = a.substring(0, a.length() - 1).strip();
		}
		return a;
	}

	/**
	 * Drop the largest bracket run at either end which leaves the brackets properly nested. Runs
	 * holding no name character enclose nothing, so a mirrored or empty one is debris the readme
	 * laid around the name ("Squacky &lt;&gt;", "&gt;&gt; [BIG]-Bastardo® &lt;&lt;", "(edit"), while a bracket
	 * the name owns is nested with its partner and no cut including it can survive the test
	 * ("ScorpioN &lt;No Name&gt;", "[BIG]-Bastardo®", "&lt;GF&gt;-REX!!").
	 * <p>
	 * Nesting, not counting: "&gt;&gt; ... &lt;&lt;" holds two of each yet opens with a closer. Interior
	 * brackets are never cut, so the "()" in "-=G|-|()ST=-" still spells an O. A value which is
	 * one whole group is left alone rather than unwrapped: "[FBI]" and "&lt;((Vampyre))&gt;" are how
	 * those authors write their names, and a clan tag is a name.
	 */
	private static String trimBracketDebris(String a) {
		if (properlyNested(a) && wrapsWholeValue(a)) return a;

		int lead = 0;
		while (lead < a.length() && bracketOrSpace(a.charAt(lead))) lead++;
		int trail = 0;
		while (trail < a.length() - lead && bracketOrSpace(a.charAt(a.length() - 1 - trail))) trail++;

		// the runs hold no name character, so the largest cut which nests properly loses nothing
		for (int l = lead; l >= 0; l--) {
			for (int t = trail; t >= 0; t--) {
				if (l == 0 && t == 0) return a;
				String cut = a.substring(l, a.length() - t).strip();
				if (properlyNested(cut)) return cut;
			}
		}
		return a;
	}

	private static boolean bracketOrSpace(char c) {
		return BRACKETS.indexOf(c) >= 0 || Character.isWhitespace(c);
	}

	/**
	 * True where the value is a single bracket group enclosing everything else.
	 */
	private static boolean wrapsWholeValue(String a) {
		int first = a.isEmpty() ? -1 : BRACKETS.indexOf(a.charAt(0));
		if (first < 0 || first % 2 != 0) return false;

		int depth = 0;
		for (int i = 0; i < a.length(); i++) {
			int bracket = BRACKETS.indexOf(a.charAt(i));
			if (bracket < 0) continue;
			if (bracket % 2 == 0) depth++;
			else if (--depth == 0) return i == a.length() - 1;
		}
		return false;
	}

	private static boolean properlyNested(String a) {
		StringBuilder open = new StringBuilder();
		for (int i = 0; i < a.length(); i++) {
			int bracket = BRACKETS.indexOf(a.charAt(i));
			if (bracket < 0) continue;
			if (bracket % 2 == 0) open.append(a.charAt(i));
			else if (open.isEmpty() || open.charAt(open.length() - 1) != BRACKETS.charAt(bracket - 1)) return false;
			else open.setLength(open.length() - 1);
		}
		return open.isEmpty();
	}

	private static int count(String s, char c) {
		int n = 0;
		for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++;
		return n;
	}
}
