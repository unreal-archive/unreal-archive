package org.unrealarchive.content;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Matcher;

import org.unrealarchive.common.Util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;
import static org.unrealarchive.content.Authors.HANDLE_AFTER;

public class AuthorsTest {

	@TempDir
	Path repoPath;

	@Test
	void testHandleAfter() {
		String name1 = "Mike Bananas (Mr. Banan)";
		String name2 = "Mike Bananas 'Mr. Banan'";

		Matcher match1 = HANDLE_AFTER.matcher(name1);
		assertTrue(match1.matches());
		assertEquals("Mr. Banan", match1.group(3));
		assertEquals("Mike Bananas", match1.group(1));

		Matcher match2 = HANDLE_AFTER.matcher(name2);
		assertTrue(match2.matches());
		assertEquals("Mr. Banan", match2.group(3));
		assertEquals("Mike Bananas", match2.group(1));
	}

	@Test
	void testIsSomeone() {
		assertFalse(Authors.isSomeone(""));
		assertFalse(Authors.isSomeone(AuthorRepository.UNKNOWN));
		assertFalse(Authors.isSomeone(AuthorRepository.VARIOUS));
		assertFalse(Authors.isSomeone("various"));
		assertFalse(Authors.isSomeone("Various"));

		Author regularAuthor = new Author("Regular Author");
		assertTrue(Authors.isSomeone(regularAuthor));
	}

	@Test
	public void akaAliasTest() throws IOException {
		AuthorRepository repo = new AuthorRepository.FileRepository(repoPath);
		Authors.setRepository(repo, repoPath);

		Authors.addToRepository("Mike Bananas A.K.A. Mr. Banan", repo);
		assertEquals(Authors.byName("Mr Banan"), Authors.byName("Mike Bananas"));
	}

	@Test
	public void noDateTest() throws IOException {
		AuthorRepository repo = new AuthorRepository.FileRepository(repoPath);
		Authors.setRepository(repo, repoPath);
		Authors.addToRepository("VoiceGuy 22/04/2024", repo);
		Authors.addToRepository("VoiceGuy 2024-04-22", repo);
		assertEquals(Authors.byName("VoiceGuy"), Authors.byName("VoiceGuy 22/04/2024"));
	}

	@Test
	public void autoAliasTest() throws IOException {
		AuthorRepository repo = new AuthorRepository.FileRepository(repoPath);
		Authors.setRepository(repo, repoPath);

		Authors.addToRepository("Mike \"Mr. Banan\" Bananas", repo);
		assertEquals(Authors.byName("Mr. Banan"), Authors.byName("Mike Bananas"));
		assertEquals(Authors.byName("Mike \"Mr. Banan\" Bananas"), Authors.byName("Mike Bananas"));

		Authors.addToRepository("M. \"Banan\" Bananas", repo);
		assertEquals(Authors.byName("Banan"), Authors.byName("M. Bananas"));
		assertEquals(Authors.byName("M. \"Banan\" Bananas"), Authors.byName("M. Bananas"));

		Authors.addToRepository("Mark `MB` Ban", repo);
		assertEquals(Authors.byName("MB"), Authors.byName("Mark Ban"));
		assertEquals(Authors.byName("Mark `MB` Ban"), Authors.byName("Mark Ban"));
	}

	@Test
	public void normalised() {
		assertEquals("Fernandez", Util.normalised("Fernández"));
	}
}
