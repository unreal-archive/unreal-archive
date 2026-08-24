package org.unrealarchive.indexing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IndexUtilsTest {

	@Test
	public void findAuthor() throws IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("TestReadMe.txt")))) {
			assertEquals("Thåt Guy", IndexUtils.findAuthor(br.lines().toList()));
		}
	}

	@Test
	public void findAuthorWithContactDetails() {
		// contact details trailing the name are not part of it
		assertEquals("Maxar", IndexUtils.findAuthor(List.of("Team Skin mod by Maxar email (maxfuller5@gmail.com)")));
		assertEquals("Bob", IndexUtils.findAuthor(List.of("Author: Bob <bob@example.com>")));
		assertEquals("Bob", IndexUtils.findAuthor(List.of("Author: Bob - http://www.example.com/bob")));
		assertEquals("Bob", IndexUtils.findAuthor(List.of("Author: Bob E-mail: bob@example.com")));

		// ... and they must not push the name beyond the length the expression matches,
		// which previously left the author entirely undetected
		assertEquals("DeathChild", IndexUtils.findAuthor(List.of("Cyborg pack By DeathChild email (rumachado@clix.pt)")));

		// a line carrying nothing but contact details is not an author
		assertEquals(null, IndexUtils.findAuthor(List.of("Author: bob@example.com")));
	}

	@Test
	public void findAuthorKeywordIsAWord() {
		// "by" inside another word is not an attribution - this is a column heading
		assertEquals(null, IndexUtils.findAuthor(List.of("  POSSIBLE      STANDBY      DEFINATE                NOt")));
		assertEquals(null, IndexUtils.findAuthor(List.of("There is a health pack nearby the lift, go grab it")));

		// but the plural and the "(s)" form still are
		assertEquals("Bob", IndexUtils.findAuthor(List.of("Authors: Bob")));
		assertEquals("Bob", IndexUtils.findAuthor(List.of("Author(s): Bob")));
	}

	@Test
	public void findAuthorEndsAtColumnPadding() {
		// readme metadata is laid out in columns, so padding ends the name
		assertEquals("Adapt", IndexUtils.findAuthor(List.of("adaptadapt        Adapt Skin, by Adapt            adaptadapt")));
		assertEquals("MATTIAS EKH", IndexUtils.findAuthor(List.of("Author: MATTIAS EKH   Asmdminigun")));

		// an ellipsis run between words does the same, and must survive the URL cleanup which
		// used to read "Luger...........great" as a hostname and drop it
		assertEquals("Luger", IndexUtils.findAuthor(List.of("skins for Katana and Mudfish created by Luger...........great stuff!!")));

		// a single space is not a separator
		assertEquals("Thåt Guy", IndexUtils.findAuthor(List.of("Author: Thåt Guy")));
	}

	@Test
	public void findAuthorTrimsDecoration() {
		// one-sided decoration is trimmed, and a bracket it closes is kept
		assertEquals("Holy Embrace[UNW]", IndexUtils.findAuthor(List.of(" (:---By Holy Embrace[UNW]---:)")));

		// decoration at both ends belongs to the handle
		assertEquals("-=Musc@t=-", IndexUtils.findAuthor(List.of("Author: -=Musc@t=-")));
		assertEquals("...AndRelax aka baddavie", IndexUtils.findAuthor(List.of("Author: ...AndRelax aka baddavie")));

		// a copyright marker is not part of the name
		assertEquals("J. Martin", IndexUtils.findAuthor(List.of("Male2TheMoon skin For Ureal by J. Martin (c)2005")));
	}

	@Test
	public void findAuthorRejectsProse() {
		// the keyword appears in prose far into a line of instructions, and what follows is no name
		assertEquals(null, IndexUtils.findAuthor(
			List.of("F1 on your keyboard will bring up your score, map name, author, etc.")));

		// a labelled line is preferred over an earlier mention buried in prose
		assertEquals("Bob", IndexUtils.findAuthor(List.of(
			"This skin was originally created by somebody else entirely, adapted here for Unreal",
			"Author: Bob"
		)));
	}

	@Test
	public void findAuthorKeepsClanTags() {
		// tags are stripped upstream, per file, only from HTML readmes - so an angle bracketed
		// clan tag in a plain text readme is a name, and must survive
		assertEquals("<NDP>BOZO", IndexUtils.findAuthor(List.of("Author: <NDP>BOZO")));
		assertEquals("Palsied <twat>", IndexUtils.findAuthor(List.of("Skin by Palsied <twat>")));
	}

	@Test
	public void cleanString() {
		// plain strings are left alone, other than whitespace trimming
		assertEquals("Thåt Guy", IndexUtils.cleanString("  Thåt Guy\n"));

		// colour markup is an ESC followed by an RGB triplet, anywhere in the string
		assertEquals("Hello", IndexUtils.cleanString("\u001bÿ\u0001\u0001Hello"));
		assertEquals("ab", IndexUtils.cleanString("a\u001bÿ\u0001\u0001b"));

		// a real UT2004 author, one colour code per character
		assertEquals("snare", IndexUtils.cleanString(
			"\u001b\u040e\u040e\u040es\u001b\u201a\u044a<n\u001b\u040e\u040e\u040ea\u001b\u201a\u044a<r\u001b\u040e\u040e\u040ee\u001b\u0440\u0440\u0440"));

		// truncated markup at the end of a string must not overrun it
		assertEquals("done", IndexUtils.cleanString("done\u001b"));
		assertEquals("done", IndexUtils.cleanString("done\u001bÿ"));

		// stray control characters go too, but newlines and tabs are content
		assertEquals("a\tb\nc", IndexUtils.cleanString("a\tb\u0000\u0007\nc"));
	}
}
