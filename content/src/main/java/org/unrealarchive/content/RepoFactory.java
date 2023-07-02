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

public class RepoFactory {

	public static final String CONTENT_DIR = "content";
	public static final String DOCUMENTS_DIR = "documents";
	public static final String GAMETYPES_DIR = "gametypes";
	public static final String MANAGED_DIR = "managed";
	public static final String AUTHORS_DIR = "authors";
	public static final String WIKIS_DIR = "wikis";

	private static final Path TMP = Paths.get(System.getProperty("java.io.tmpdir"));
	private static final String CONTENT_URL = System.getenv().getOrDefault("UA_CONTENT_URL",
																		   "https://github.com/unreal-archive/unreal-archive-data/archive/master.zip");

	public static SimpleAddonRepository contentRepo(CLI cli) throws IOException {
		Path contentPath = contentPathHelper(cli);

		final long start = System.currentTimeMillis();
		final SimpleAddonRepository repo = new SimpleAddonRepository.FileRepository(contentPath.resolve(CONTENT_DIR));
		final double gigs = (repo.fileSize() / 1024d / 1024d / 1024d);
		System.err.printf("Loaded content index with %d items (%.2fGB) in %.2fs%n",
						  repo.size(), gigs, (System.currentTimeMillis() - start) / 1000f);

		return repo;
	}

	public static GameTypeRepository gameTypeRepo(CLI cli) throws IOException {
		Path contentPath = contentPathHelper(cli);
		final long start = System.currentTimeMillis();
		final GameTypeRepository repo = new GameTypeRepository.FileRepository(contentPath.resolve(GAMETYPES_DIR));
		System.err.printf("Loaded gametypes index with %d items in %.2fs%n",
						  repo.size(), (System.currentTimeMillis() - start) / 1000f);
		return repo;
	}

	public static DocumentRepository documentRepo(CLI cli) throws IOException {
		Path contentPath = contentPathHelper(cli);

		final long start = System.currentTimeMillis();
		final DocumentRepository repo = new DocumentRepository.FileRepository(contentPath.resolve(DOCUMENTS_DIR));
		System.err.printf("Loaded document index with %d items in %.2fs%n",
						  repo.size(), (System.currentTimeMillis() - start) / 1000f);

		return repo;
	}

	public static ManagedContentRepository managedRepo(CLI cli) throws IOException {
		Path contentPath = contentPathHelper(cli);

		final long start = System.currentTimeMillis();
		ManagedContentRepository managedContentManager = new ManagedContentRepository.FileRepository(contentPath.resolve(MANAGED_DIR));
		System.err.printf("Loaded managed content index with %d items in %.2fs%n",
						  managedContentManager.size(), (System.currentTimeMillis() - start) / 1000f);

		return managedContentManager;
	}

	public static WikiRepository wikiRepo(CLI cli) throws IOException {
		Path contentPath = contentPathHelper(cli);

		final long start = System.currentTimeMillis();
		final WikiRepository wikis = new WikiRepository.FileRepository(contentPath.resolve(WIKIS_DIR));
		System.err.printf("Loaded wikis index with %d pages in %.2fs%n",
						  wikis.size(), (System.currentTimeMillis() - start) / 1000f);

		return wikis;
	}

	public static Path contentPathHelper(CLI cli) throws IOException {
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
