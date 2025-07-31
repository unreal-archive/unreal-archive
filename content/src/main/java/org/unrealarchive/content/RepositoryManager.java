package org.unrealarchive.content;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

import org.unrealarchive.common.ArchiveUtil;
import org.unrealarchive.common.CLI;
import org.unrealarchive.common.Util;
import org.unrealarchive.content.addons.GameTypeRepository;
import org.unrealarchive.content.addons.SimpleAddonRepository;
import org.unrealarchive.content.docs.DocumentRepository;
import org.unrealarchive.content.managed.ManagedContentRepository;
import org.unrealarchive.content.wiki.WikiRepository;

/**
 * Repository manager that holds weak references to all repositories and initializes them lazily.
 * This provides a central access point for all repository instances while allowing them to be
 * garbage collected when not in use.
 */
public class RepositoryManager {

	// Constants moved from RepoFactory
	private static final String CONTENT_DIR = "content";
	private static final String DOCUMENTS_DIR = "documents";
	private static final String GAMETYPES_DIR = "gametypes";
	private static final String MANAGED_DIR = "managed";
	private static final String AUTHORS_DIR = "authors";
	private static final String WIKIS_DIR = "wikis";

	private static final Path TMP = Paths.get(System.getProperty("java.io.tmpdir"));
	private static final String CONTENT_URL = System.getenv().getOrDefault("UA_CONTENT_URL",
																		   "https://github.com/unreal-archive/unreal-archive-data/archive/master.zip");

	private final CLI cli;

	private volatile SimpleAddonRepository simpleAddonRepository;
	private volatile ManagedContentRepository managedContentRepository;
	private volatile DocumentRepository documentRepository;
	private volatile AuthorRepository authorRepository;
	private volatile WikiRepository wikiRepository;
	private volatile GameTypeRepository gameTypeRepository;

	/**
	 * Create a new RepositoryManager instance.
	 *
	 * @param cli CLI instance used for repository configuration
	 */
	public RepositoryManager(CLI cli) {
		this.cli = cli;
	}

	/**
	 * Get the SimpleAddonRepository instance, creating it if necessary.
	 *
	 * @return SimpleAddonRepository instance
	 */
	public SimpleAddonRepository addons() {
		if (simpleAddonRepository == null) {
			synchronized (CONTENT_DIR) {
				if (simpleAddonRepository == null) simpleAddonRepository = createContentRepo();
			}
		}
		return simpleAddonRepository;
	}

	/**
	 * Get the GameTypeRepository instance, creating it if necessary.
	 *
	 * @return GameTypeRepository instance
	 */
	public GameTypeRepository gameTypes() {
		if (gameTypeRepository == null) {
			synchronized (GAMETYPES_DIR) {
				if (gameTypeRepository == null) gameTypeRepository = createGameTypeRepo();
			}
		}
		return gameTypeRepository;
	}

	/**
	 * Get the ManagedContentRepository instance, creating it if necessary.
	 *
	 * @return ManagedContentRepository instance
	 */
	public ManagedContentRepository managed() {
		if (managedContentRepository == null) {
			synchronized (MANAGED_DIR) {
				if (managedContentRepository == null) managedContentRepository = createManagedRepo();
			}
		}
		return managedContentRepository;
	}

	/**
	 * Get the DocumentRepository instance, creating it if necessary.
	 *
	 * @return DocumentRepository instance
	 */
	public DocumentRepository docs() {
		if (documentRepository == null) {
			synchronized (DOCUMENTS_DIR) {
				if (documentRepository == null) documentRepository = createDocumentRepo();
			}
		}
		return documentRepository;
	}

	/**
	 * Get the AuthorRepository instance, creating it if necessary.
	 *
	 * @return AuthorRepository instance
	 */
	public AuthorRepository authors() {
		if (authorRepository == null) {
			synchronized (AUTHORS_DIR) {
				if (authorRepository == null) authorRepository = createAuthorRepo();
			}
		}
		return authorRepository;
	}

	/**
	 * Get the WikiRepository instance, creating it if necessary.
	 *
	 * @return WikiRepository instance
	 */
	public WikiRepository wikis() {
		if (wikiRepository == null) {
			synchronized (WIKIS_DIR) {
				if (wikiRepository == null) wikiRepository = createWikiRepo();
			}
		}
		return wikiRepository;
	}

	// Private methods moved from RepoFactory

	private SimpleAddonRepository createContentRepo() {
		try {
			Path contentPath = getContentPath();

			final long start = System.currentTimeMillis();
			final SimpleAddonRepository repo = new SimpleAddonRepository.FileRepository(contentPath.resolve(CONTENT_DIR));
			final double gigs = (repo.fileSize() / 1024d / 1024d / 1024d);
			System.err.printf("Loaded content index with %d items (%.2fGB) in %.2fs%n",
							  repo.size(), gigs, (System.currentTimeMillis() - start) / 1000f);

			return repo;
		} catch (IOException e) {
			throw new RuntimeException("Failed to initialize content repository", e);
		}
	}

	private GameTypeRepository createGameTypeRepo() {
		try {
			Path contentPath = getContentPath();
			final long start = System.currentTimeMillis();
			final GameTypeRepository repo = new GameTypeRepository.FileRepository(contentPath.resolve(GAMETYPES_DIR));
			System.err.printf("Loaded gametypes index with %d items in %.2fs%n",
							  repo.size(), (System.currentTimeMillis() - start) / 1000f);
			return repo;
		} catch (IOException e) {
			throw new RuntimeException("Failed to initialize gametype repository", e);
		}
	}

	private DocumentRepository createDocumentRepo() {
		try {
			Path contentPath = getContentPath();

			final long start = System.currentTimeMillis();
			final DocumentRepository repo = new DocumentRepository.FileRepository(contentPath.resolve(DOCUMENTS_DIR));
			System.err.printf("Loaded document index with %d items in %.2fs%n",
							  repo.size(), (System.currentTimeMillis() - start) / 1000f);

			return repo;
		} catch (IOException e) {
			throw new RuntimeException("Failed to initialize document repository", e);
		}
	}

	private ManagedContentRepository createManagedRepo() {
		try {
			Path contentPath = getContentPath();

			final long start = System.currentTimeMillis();
			ManagedContentRepository managedContentManager = new ManagedContentRepository.FileRepository(contentPath.resolve(MANAGED_DIR));
			System.err.printf("Loaded managed content index with %d items in %.2fs%n",
							  managedContentManager.size(), (System.currentTimeMillis() - start) / 1000f);

			return managedContentManager;
		} catch (IOException e) {
			throw new RuntimeException("Failed to initialize managed content repository", e);
		}
	}

	private AuthorRepository createAuthorRepo() {
		try {
			Path contentPath = getContentPath();

			final long start = System.currentTimeMillis();
			AuthorRepository authorRepo = new AuthorRepository.FileRepository(contentPath.resolve(AUTHORS_DIR));
			System.err.printf("Loaded authors repository with %d entries in %.2fs%n",
							  authorRepo.size(), (System.currentTimeMillis() - start) / 1000f);

			Authors.setRepository(authorRepo, contentPath.resolve(AUTHORS_DIR));

			Authors.autoPopRepository(authorRepo, addons(), gameTypes(), managed());
			System.err.printf("Populated authors repository with %d entries in %.2fs%n",
							  authorRepo.size(), (System.currentTimeMillis() - start) / 1000f);

			return authorRepo;
		} catch (IOException e) {
			throw new RuntimeException("Failed to initialize author repository", e);
		}
	}

	private WikiRepository createWikiRepo() {
		try {
			Path contentPath = getContentPath();

			final long start = System.currentTimeMillis();
			final WikiRepository wikis = new WikiRepository.FileRepository(contentPath.resolve(WIKIS_DIR));
			System.err.printf("Loaded wikis index with %d pages in %.2fs%n",
							  wikis.size(), (System.currentTimeMillis() - start) / 1000f);

			return wikis;
		} catch (IOException e) {
			throw new RuntimeException("Failed to initialize wiki repository", e);
		}
	}

	private Path getContentPath() throws IOException {
		if (cli.option("content-path", null) == null) {
			if (cli.flag("content-download")) {
				Path tmpPath = TMP.resolve("ua-tmp");
				if (!Files.exists(tmpPath) || !Files.isDirectory(tmpPath)) {
					Files.createDirectories(tmpPath);
					Path tmpFile = tmpPath.resolve("archive.zip");
					System.out.printf("Downloading archive from %s... ", CONTENT_URL);
					Util.downloadTo(Util.toUriString(CONTENT_URL), tmpFile);
					System.out.println("Done");
					try {
						System.out.printf("Extracting archive from %s to %s... ", tmpFile, tmpPath);
						ArchiveUtil.extract(tmpFile, tmpPath, Duration.ofMinutes(10));
						System.out.println("Done");
					} catch (Throwable e) {
						// make sure to clean out the path, so we can try again next time
						ArchiveUtil.cleanPath(tmpPath);
						throw new IOException("Failed to extract downloaded content archive", e);
					}
				}

				// find the content directory within the extracted stuff
				try (Stream<Path> pathStream = Files.walk(tmpPath, 3, FileVisitOption.FOLLOW_LINKS)) {
					Optional<Path> contentParent = pathStream
						.filter(p -> Files.isDirectory(p) && p.getFileName().toString().equals(CONTENT_DIR))
						.map(Path::getParent)
						.findFirst();

					cli.putOption("content-path", contentParent.orElseThrow(IllegalArgumentException::new).toString());
				}
			}
		}

		if (cli.option("content-path", null) == null && !cli.flag("content-download")) {
			System.err.println("content-path or content-download must be set");
			System.exit(3);
		}

		Path contentPath = Paths.get(cli.option("content-path", null));
		if (!Files.isDirectory(contentPath)) {
			System.err.println("content-path must be a directory!");
			System.exit(3);
		}

		return contentPath.toAbsolutePath();
	}
}