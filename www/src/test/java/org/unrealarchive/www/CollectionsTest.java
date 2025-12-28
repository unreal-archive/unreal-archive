package org.unrealarchive.www;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import org.unrealarchive.common.CLI;
import org.unrealarchive.content.CollectionsRepository;
import org.unrealarchive.content.ContentCollection;
import org.unrealarchive.content.ContentEntity;
import org.unrealarchive.content.Games;
import org.unrealarchive.content.RepositoryManager;
import org.unrealarchive.content.managed.Managed;
import org.unrealarchive.content.managed.ManagedContentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class CollectionsTest {

	@TempDir
	Path temp;

	@Test
	public void collectionsTest() throws IOException {
		Path wwwRoot = temp.resolve("www");
		Path staticRoot = temp.resolve("static");

		ContentCollection collection = new ContentCollection();
		collection.title = "Test Collection";
		collection.published = true;
		collection.createdDate = java.time.LocalDate.now();

		Managed managed = new Managed();
		managed.game = "General";
		managed.group = "Test";
		managed.subGroup = "Sub";
		managed.title = "Test Managed";
		managed.author = "Author";

		ContentCollection.CollectionItem item = new ContentCollection.CollectionItem();
		item.id = managed.id().toString();
		item.title = "Managed Item";
		collection.items.add(item);

		RepositoryManager repos = new RepositoryManager(CLI.parse("")) {
			@Override
			public CollectionsRepository collections() {
				return new CollectionsRepository() {
					@Override public int size() { return 1; }
					@Override public Collection<ContentCollection> all() { return Set.of(collection); }
					@Override public ContentCollection find(String title) { return collection.title.equalsIgnoreCase(title) ? collection : null; }
					@Override public void put(ContentCollection collection) {}
					@Override public void writeContent(ContentCollection collection, Path outPath) {}
				};
			}

			@Override
			public ManagedContentRepository managed() {
				return new ManagedContentRepository() {
					@Override public int size() { return 1; }
					@Override public Collection<Managed> all() { return Set.of(managed); }
					@Override public Managed forId(String id) { return managed; }
					@Override public void put(Managed managed) {}
					@Override public void create(Games game, String group, String path, String title, Consumer<Managed> initialised) {}
					@Override public ReadableByteChannel document(Managed managed) { return null; }
					@Override public void writeContent(Managed managed, Path outPath) {}
					@Override public Managed findManaged(Games game, String group, String path, String title) { return null; }
				};
			}

			@Override
			public ContentEntity<?> forId(String id) {
				if (id.equals(managed.id().toString())) return managed;
				return null;
			}
		};

		Collections collections = new Collections(repos, wwwRoot, staticRoot, SiteFeatures.ALL);
		Set<SiteMap.Page> pages = collections.generate();

		assertFalse(pages.isEmpty());
	}
}
