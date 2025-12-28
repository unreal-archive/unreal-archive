package org.unrealarchive.indexing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.unrealarchive.content.CollectionsRepository;
import org.unrealarchive.content.ContentCollection;
import org.unrealarchive.storage.DataStore;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

public class CollectionsManagerTest {

	@TempDir
	Path tempDir;

	@Test
	public void syncTest() throws IOException {
		Path collectionsPath = tempDir.resolve("collections");
		Files.createDirectories(collectionsPath);
		CollectionsRepository repo = new CollectionsRepository.FileRepository(collectionsPath);

		CollectionsManager manager = new CollectionsManager(null, repo, new DataStore.NopStore());

		ContentCollection collection = new ContentCollection();
		collection.title = "Test Collection";
		collection.createdDate = LocalDate.now();

		Path localFile = tempDir.resolve("test.zip");
		Files.writeString(localFile, "test content");

		ContentCollection.CollectionArchive archive = new ContentCollection.CollectionArchive();
		archive.title = "Test Archive";
		archive.localFile = localFile.toString();
		archive.synced = false;

		collection.items.clear();
		collection.archives.add(archive);

		manager.sync(collection);

		ContentCollection synced = repo.find("Test Collection");
		assertNotNull(synced);
		assertEquals(1, synced.archives.size());
		assertTrue(synced.archives.get(0).synced);
		assertFalse(synced.archives.get(0).downloads.isEmpty());
		assertEquals("nop://collections/test-collection/test.zip", synced.archives.get(0).downloads.get(0).url);
		assertEquals(Files.size(localFile), synced.archives.get(0).fileSize);
		assertNotNull(synced.archives.get(0).hash);

		// test that it doesn't sync again if already synced
		manager.sync(synced);
		assertEquals(1, synced.archives.get(0).downloads.size()); // should still be 1

		// test that it doesn't sync if deleted
		ContentCollection.CollectionArchive archive2 = new ContentCollection.CollectionArchive();
		archive2.title = "Test Archive 2";
		archive2.localFile = localFile.toString();
		archive2.synced = false;
		archive2.deleted = true;
		synced.archives.add(archive2);
		manager.checkin(synced);

		manager.sync(synced);
		ContentCollection synced2 = repo.find("Test Collection");
		assertFalse(synced2.archives.get(1).synced);
		assertTrue(synced2.archives.get(1).downloads.isEmpty());
	}
}
