package org.unrealarchive.content;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthorRepositoryTest {

	private AuthorRepository.FileRepository repository;

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() throws IOException {
		repository = new AuthorRepository.FileRepository(tempDir);
	}

	// Utility method to create an Author
	private Author createAuthor(String name, Set<String> aliases, String backgroundImage, String about, boolean deleted) {
		Author author = new Author();
		author.name = name;
		author.aliases = aliases;
		author.bgImage = backgroundImage;
		author.about = about;
		author.deleted = deleted;
		return author;
	}

	@Test
	void testInitialSizeIsZero() {
		assertEquals(0, repository.size(), "Initial repository size should be zero");
	}

	@Test
	void testPutAndSize() throws IOException {
		// Arrange
		Author author = createAuthor("Author One", Set.of("Alias1"), "bg.jpg", "About Author One", false);

		// Act
		repository.put(author, false);

		// Assert
		assertEquals(1, repository.size(), "Repository size should be 1 after adding an author");
	}

	@Test
	void testPutAndRetrieveBySlug() throws IOException {
		// Arrange
		Author author = createAuthor("Author Slug", Set.of("Alias"), null, "", false);

		// Act
		repository.put(author, false);
		Author retrieved = repository.byName(author.name);

		// Assert
		assertNotNull(retrieved, "Author should be retrievable by slug");
		assertEquals(author.name, retrieved.name, "Retrieved author's name should match");
		assertEquals(author.aliases, retrieved.aliases, "Retrieved author's aliases should match");
	}

	@Test
	void testPutOverwritesExistingAuthor() throws IOException {
		// Arrange
		Author initialAuthor = createAuthor("Author Name", Set.of("Alias1"), null, "Old about", false);
		Author updatedAuthor = createAuthor("Author Name", Set.of("Alias1", "Alias2"), "new_bg.jpg", "Updated about", false);

		// Act
		repository.put(initialAuthor, false);
		repository.put(updatedAuthor, false);
		Author retrieved = repository.byName(initialAuthor.name);

		// Assert
		assertEquals(1, repository.size(), "Repository size should remain 1 after overwriting an author");
		assertEquals("Updated about", retrieved.about, "Author 'about' field should be updated");
		assertEquals("new_bg.jpg", retrieved.bgImage, "Background image should be updated");
	}

	@Test
	void testAllFiltersDeletedAuthors() throws IOException {
		// Arrange
		Author activeAuthor = createAuthor("Active Author", Set.of(), null, "", false);
		Author deletedAuthor = createAuthor("Deleted Author", Set.of(), null, "", true);

		// Act
		repository.put(activeAuthor, false);
		repository.put(deletedAuthor, false);
		Collection<Author> authors = repository.all();

		// Assert
		assertEquals(1, authors.size(), "Only active authors should be returned");
		assertEquals("Active Author", authors.iterator().next().name, "Returned author should be the active one");
	}

	@Test
	void testInitializationLoadsExistingData() throws IOException {
		// Arrange
		Path authorFile = tempDir.resolve("existing_author.yml");
		Files.writeString(authorFile, """
			name: "Existing Author"
			aliases:
			  - "Alias1"
			  - "Alias2"
			links:
			  Twitter: "@ExistingAuthor"
			backgroundImage: "bg.jpg"
			about: "A previously stored author"
			deleted: false
			""", StandardOpenOption.CREATE_NEW);

		repository = new AuthorRepository.FileRepository(tempDir); // Reload repository

		// Act
		Collection<Author> authors = repository.all();

		// Assert
		assertEquals(1, repository.size(), "Repository size should reflect loaded data");
		Author retrieved = authors.iterator().next();
		assertEquals("Existing Author", retrieved.name, "Loaded author's name should match");
		assertEquals(Set.of("Alias1", "Alias2"), retrieved.aliases, "Loaded author's aliases should match");
		assertEquals(Map.of("Twitter", "@ExistingAuthor"), retrieved.links, "Loaded author's links should match");
	}

}