package org.unrealarchive.indexing;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthorNameUtilsTest {

	@Test
	public void findAuthorWithContactDetails() {
		// contact details trailing the name are not part of it
		assertEquals("Maxar", AuthorNameUtils.findAuthor(List.of("Team Skin mod by Maxar email (maxfuller5@gmail.com)")));
		assertEquals("Bob", AuthorNameUtils.findAuthor(List.of("Author: Bob <bob@example.com>")));
		assertEquals("Bob", AuthorNameUtils.findAuthor(List.of("Author: Bob - http://www.example.com/bob")));
		assertEquals("Bob", AuthorNameUtils.findAuthor(List.of("Author: Bob E-mail: bob@example.com")));

		// ... and they must not push the name beyond the length the expression matches,
		// which previously left the author entirely undetected
		assertEquals("DeathChild", AuthorNameUtils.findAuthor(List.of("Cyborg pack By DeathChild email (rumachado@clix.pt)")));

		// a line carrying nothing but contact details is not an author
		assertEquals(null, AuthorNameUtils.findAuthor(List.of("Author: bob@example.com")));
	}

	@Test
	public void findAuthorKeywordIsAWord() {
		// "by" inside another word is not an attribution - this is a column heading
		assertEquals(null, AuthorNameUtils.findAuthor(List.of("  POSSIBLE      STANDBY      DEFINATE                NOt")));
		assertEquals(null, AuthorNameUtils.findAuthor(List.of("There is a health pack nearby the lift, go grab it")));

		// but the plural and the "(s)" form still are
		assertEquals("Bob", AuthorNameUtils.findAuthor(List.of("Authors: Bob")));
		assertEquals("Bob", AuthorNameUtils.findAuthor(List.of("Author(s): Bob")));
	}

	@Test
	public void findAuthorEndsAtColumnPadding() {
		// readme metadata is laid out in columns, so padding ends the name
		assertEquals("Adapt", AuthorNameUtils.findAuthor(List.of("adaptadapt        Adapt Skin, by Adapt            adaptadapt")));
		assertEquals("MATTIAS EKH", AuthorNameUtils.findAuthor(List.of("Author: MATTIAS EKH   Asmdminigun")));

		// an ellipsis run between words does the same, and must survive the URL cleanup which
		// used to read "Luger...........great" as a hostname and drop it
		assertEquals("Luger", AuthorNameUtils.findAuthor(List.of("skins for Katana and Mudfish created by Luger...........great stuff!!")));

		// a single space is not a separator
		assertEquals("Thåt Guy", AuthorNameUtils.findAuthor(List.of("Author: Thåt Guy")));
	}

	@Test
	public void findAuthorTrimsDecoration() {
		// one-sided decoration is trimmed, and a bracket it closes is kept
		assertEquals("Holy Embrace[UNW]", AuthorNameUtils.findAuthor(List.of(" (:---By Holy Embrace[UNW]---:)")));

		// decoration at both ends belongs to the handle
		assertEquals("-=Musc@t=-", AuthorNameUtils.findAuthor(List.of("Author: -=Musc@t=-")));
		assertEquals("...AndRelax aka baddavie", AuthorNameUtils.findAuthor(List.of("Author: ...AndRelax aka baddavie")));

		// a copyright marker is not part of the name
		assertEquals("J. Martin", AuthorNameUtils.findAuthor(List.of("Male2TheMoon skin For Ureal by J. Martin (c)2005")));
	}

	@Test
	public void findAuthorRejectsProse() {
		// the keyword appears in prose far into a line of instructions, and what follows is no name
		assertEquals(null, AuthorNameUtils.findAuthor(
			List.of("F1 on your keyboard will bring up your score, map name, author, etc.")));

		// a labelled line is preferred over an earlier mention buried in prose
		assertEquals("Bob", AuthorNameUtils.findAuthor(List.of(
			"This skin was originally created by somebody else entirely, adapted here for Unreal",
			"Author: Bob"
		)));

		// mid-sentence prose gives itself away with its first word, and a lowercase word after it.
		// Stored value cleanup rejected these while extraction accepted them, until they shared rules
		assertEquals(null, AuthorNameUtils.findAuthor(List.of("If you like the skin, stop by and let me know!")));
		assertEquals(null, AuthorNameUtils.findAuthor(List.of("intended by the author of these skins")));

		// a word which is never a name, however it is labelled
		assertEquals(null, AuthorNameUtils.findAuthor(List.of("Author: info")));
	}

	@Test
	public void findAuthorKeepsClanTags() {
		// tags are stripped upstream, per file, only from HTML readmes - so an angle bracketed
		// clan tag in a plain text readme is a name, and must survive
		assertEquals("<NDP>BOZO", AuthorNameUtils.findAuthor(List.of("Author: <NDP>BOZO")));
		assertEquals("Palsied <twat>", AuthorNameUtils.findAuthor(List.of("Skin by Palsied <twat>")));
	}

	/**
	 * Author extraction and stored value cleanup are one rule set, so what one keeps the other
	 * keeps. These cases come from values found in the index rather than from readme lines.
	 */
	@Test
	public void cleanAuthorSharesRulesWithExtraction() {
		// decoration which recurs earlier in the value belongs to a stylised handle
		assertEquals("?3rror?", AuthorNameUtils.cleanAuthor("?3rror?"));
		assertEquals(":SOLO:[EL]Darkchlor1", AuthorNameUtils.cleanAuthor(":SOLO:[EL]Darkchlor1"));
		assertEquals("Raen Gregory AkA [WFU]*NoReMoRsE***", AuthorNameUtils.cleanAuthor("Raen Gregory AkA [WFU]*NoReMoRsE***"));

		// a full stop closing an initial or an acronym is part of the name; sentence punctuation is not
		assertEquals("H.O.L.", AuthorNameUtils.cleanAuthor("H.O.L."));
		assertEquals("Ryan F.", AuthorNameUtils.cleanAuthor("Ryan F."));
		assertEquals("Sam Plate", AuthorNameUtils.cleanAuthor("Sam Plate."));

		// a date tail is not part of the name
		assertEquals("Rick 'Krow' Stirling", AuthorNameUtils.cleanAuthor("Rick 'Krow' Stirling May/June 2000"));

		// a value holding no name reduces to unknown, but reports as nameless, so a caller holding
		// a stored value can leave it exactly as the author wrote it rather than discard it
		assertEquals("Unknown", AuthorNameUtils.cleanAuthor("of these skins"));
		assertTrue(AuthorNameUtils.isNameless(".:..:"));
		assertFalse(AuthorNameUtils.isNameless("-GhostXC"));
	}

	/**
	 * Bracket debris and flush leading punctuation both split an author across two entries, since
	 * {@code AuthorRepository.authorKey()} does not normalise them away. Every case here was found
	 * written into the index by an earlier sweep.
	 */
	@Test
	public void cleanAuthorTrimsBracketDebrisAndFlushPunctuation() {
		// a bracket run the value balances without is debris the readme laid around the name
		assertEquals("Squacky", AuthorNameUtils.cleanAuthor("Squacky <>"));
		assertEquals("Squacky", AuthorNameUtils.cleanAuthor("Squacky >><<"));
		assertEquals("Vincent 'Stab_joe' JOYAU", AuthorNameUtils.cleanAuthor("Vincent 'Stab_joe' JOYAU <<>>"));
		assertEquals("[BIG]-Bastardo\u00ae", AuthorNameUtils.cleanAuthor(">> [BIG]-Bastardo\u00ae <<"));

		// a bracket the name owns has its partner inside the value, so it stays
		assertEquals("ScorpioN <No Name>", AuthorNameUtils.cleanAuthor("ScorpioN <No Name>"));
		assertEquals("[BIG]-Bastardo\u00ae", AuthorNameUtils.cleanAuthor("[BIG]-Bastardo\u00ae"));
		// the leading bracket survives; losing the "!!" is trailing decoration's pre-existing call
		assertEquals("<GF>-REX", AuthorNameUtils.cleanAuthor("<GF>-REX!!"));

		// a value which is one whole bracket group is a clan tag, and a clan tag is a name
		assertEquals("[FBI]", AuthorNameUtils.cleanAuthor("[FBI]"));
		assertEquals("(BT5-ELITE)", AuthorNameUtils.cleanAuthor("(BT5-ELITE)"));
		assertEquals("<((Vampyre))>", AuthorNameUtils.cleanAuthor("<((Vampyre))>"));
		assertEquals("<=<Gen.ZERO>=>", AuthorNameUtils.cleanAuthor("<=<Gen.ZERO>=>"));
		// mismatched ends are not a group, so the debris still goes
		assertEquals("KiNg[Of]DeAtH", AuthorNameUtils.cleanAuthor("[KiNg[Of]DeAtH}"));
		assertEquals("SAU|SPARTAN-001", AuthorNameUtils.cleanAuthor(">SAU|SPARTAN-001"));

		// a mixed flush run is a stylised letter, which would lose its first stroke
		assertEquals("\\\\\\/ertigo", AuthorNameUtils.cleanAuthor("\\\\\\/ertigo"));
		assertEquals("\\V/ictima", AuthorNameUtils.cleanAuthor("\\V/ictima"));
		assertEquals("-=G|-|()ST=-", AuthorNameUtils.cleanAuthor("-=G|-|()ST=-"));

		// an unmatched leading bracket is a truncated parenthetical
		assertEquals("Claude Meyer aka Mier", AuthorNameUtils.cleanAuthor("(Claude Meyer aka Mier"));

		// a quote or star cannot open a name, so flush against one it is debris
		assertEquals("Revelation", AuthorNameUtils.cleanAuthor("\"Revelation"));
		assertEquals("Steve Nabors", AuthorNameUtils.cleanAuthor("'Steve Nabors"));
		assertEquals("Britton Wesley", AuthorNameUtils.cleanAuthor("*Britton Wesley"));
		assertEquals("Muttley", AuthorNameUtils.cleanAuthor("'Muttley >>><<<"));

		// decoration a name does open with stays, flush or not
		assertEquals("-GhostXC", AuthorNameUtils.cleanAuthor("-GhostXC"));
		assertEquals("=iNi=bRiaN=-", AuthorNameUtils.cleanAuthor("=iNi=bRiaN=-"));
		assertEquals("-=Musc@t=-", AuthorNameUtils.cleanAuthor("-=Musc@t=-"));

		// exactly two spaces is a typo, not the column padding which ends a name
		assertEquals("Rolf Baumann", AuthorNameUtils.cleanAuthor("Rolf  Baumann"));
		assertEquals("Adapt", AuthorNameUtils.cleanAuthor("Adapt            adaptadapt"));
	}
}
