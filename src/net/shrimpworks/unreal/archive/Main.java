package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import net.shrimpworks.unreal.archive.indexer.Classifier;
import net.shrimpworks.unreal.archive.indexer.Content;
import net.shrimpworks.unreal.archive.indexer.ContentManager;
import net.shrimpworks.unreal.archive.indexer.ContentType;
import net.shrimpworks.unreal.archive.indexer.Incoming;
import net.shrimpworks.unreal.archive.indexer.IndexLog;
import net.shrimpworks.unreal.archive.indexer.Submission;
import net.shrimpworks.unreal.archive.indexer.SubmissionOverride;
import net.shrimpworks.unreal.archive.scraper.AutoIndexPHPScraper;
import net.shrimpworks.unreal.archive.scraper.Downloader;
import net.shrimpworks.unreal.packages.Umod;

public class Main {

	public static void main(String[] args) throws IOException {
		final CLI cli = CLI.parse(Collections.emptyMap(), args);

		if (cli.commands().length == 0) {
			usage();
			System.exit(1);
		}

		// TODO probably only load the content for specific commands

		if (cli.option("content-path", null) == null) {
			System.err.println("content-path must be specified!");
			System.exit(2);
		}

		Path contentPath = Paths.get(cli.option("content-path", null));
		if (!Files.isDirectory(contentPath)) {
			System.err.println("content-path must be a directory!");
			System.exit(3);
		}

		final long start = System.currentTimeMillis();
		final ContentManager contentManager = new ContentManager(contentPath);
		System.err.printf("Loaded content index with %d items in %.2fs%n",
						  contentManager.size(), (System.currentTimeMillis() - start) / 1000f);

		switch (cli.commands()[0].toLowerCase()) {
			case "index":
				index(contentManager, cli);
				break;
			case "summary":
				summary(contentManager);
				break;
			case "ls":
				list(contentManager, cli);
				break;
			case "show":
				show(contentManager, cli);
				break;
			case "scrape":
				scrape(cli);
				break;
			case "download":
				download(cli);
				break;
			case "unpack":
				unpack(cli);
				break;
			default:
				System.out.printf("Command \"%s\" has not been implemented!", cli.commands()[0]);
		}

	}

	private static void index(ContentManager contentManager, CLI cli) throws IOException {
		if (cli.option("input-path", null) == null) {
			System.err.println("input-path must be specified!");
			System.exit(2);
		}

		Path inputPath = Paths.get(cli.option("input-path", null));
		if (!Files.exists(inputPath)) {
			System.err.println("input-path does not exist!");
			System.exit(4);
		}

		final List<IndexLog> indexLogs = new ArrayList<>();

		// go through all the files in the input path and index them if new
		if (Files.isDirectory(inputPath)) {

			List<Submission> all = new ArrayList<>();
			Files.walkFileTree(inputPath, new SimpleFileVisitor<Path>() {

				SubmissionOverride override = null;

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (!Util.extension(file).equalsIgnoreCase("yml")) {
						Submission sub;
						if (Files.exists(Paths.get(file.toString() + ".yml"))) {
							sub = YAML.fromFile(Paths.get(file.toString() + ".yml"), Submission.class);
							sub.filePath = file;
						} else {
							sub = new Submission(file);
						}

						if (override != null) sub.override = override;
						all.add(sub);
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					if (Files.exists(dir.resolve("_override.yml"))) {
						override = YAML.fromFile(dir.resolve("_override.yml"), SubmissionOverride.class);
					}
					return super.preVisitDirectory(dir, attrs);
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					override = null;
					return super.postVisitDirectory(dir, exc);
				}
			});

			System.out.printf("Found %d maps to index in %s%n", all.size(), inputPath);

			AtomicInteger done = new AtomicInteger();

			all.stream().sorted().forEach(sub -> {
				IndexLog log = new IndexLog(sub);
				indexLogs.add(log);

				indexFile(sub, contentManager, log, c -> {
					for (IndexLog.LogEntry l : log.log) {
						System.out.printf("[%s] %s: %s%n", l.type, Util.fileName(c.filePath.getFileName()), l.message);
						if (l.exception != null
							&& (cli.option("verbose", "").equalsIgnoreCase("true") || cli.option("verbose", "").equalsIgnoreCase("1"))) {
							l.exception.printStackTrace(System.out);
						}
					}
					System.out.printf("Completed %d of %d\r", done.incrementAndGet(), all.size());
				});
			});
		} else {
			Submission sub = new Submission(inputPath);
			IndexLog log = new IndexLog(sub);
			indexLogs.add(log);

			indexFile(sub, contentManager, log, c -> {
				for (IndexLog.LogEntry l : log.log) {
					System.out.printf("[%s] %s: %s%n", l.type, Util.fileName(c.filePath.getFileName()), l.message);
					if (l.exception != null
						&& (cli.option("verbose", "").equalsIgnoreCase("true") || cli.option("verbose", "").equalsIgnoreCase("1"))) {
						l.exception.printStackTrace(System.out);
					}
				}
			});
		}

		int err = 0;

		for (IndexLog l : indexLogs) {
			if (!l.ok()) err++;
		}

		System.out.printf("%nCompleted indexing %d files, with %d errors%n", indexLogs.size(), err);
	}

	private static void indexFile(Submission sub, ContentManager contentManager, IndexLog log, Consumer<Submission> done) {
		try (Incoming incoming = new Incoming(sub, log)) {
			Content content = contentManager.checkout(incoming.hash);

			if (content != null) {
				// lets not support re-index yet, but we can update with urls if there are any
//				if (sub.sourceUrls != null && sub.sourceUrls.length > 0) {
//					for (String url : sub.sourceUrls) {
//						content.downloads.add(new Content.Download(url, LocalDate.now(), false));
//					}
//				}
				return;
			}

			incoming.prepare();

			ContentType type = Classifier.classify(incoming, log);

			content = type.newContent(incoming);

			if (type != ContentType.UNKNOWN) { // TODO later support a generic dumping ground for unknown content

				type.indexer.get().index(incoming, content, log, c -> {
					try {
						c.content.lastIndex = LocalDateTime.now();
						if (sub.sourceUrls != null && sub.sourceUrls.length > 0) {
							for (String url : sub.sourceUrls) {
								c.content.downloads.add(new Content.Download(url, LocalDate.now(), false));
							}
						}

						// TODO upload file to our storage, and add to downloads url set

//							Path repack = incoming.getRepack(c.name);

						if (c.content.name.isEmpty()) {
							throw new IllegalStateException("Name cannot be blank for " + incoming.submission.filePath);
						}

						contentManager.checkin(c);
					} catch (IOException e) {
						log.log(IndexLog.EntryType.FATAL, "Failed to store content file data for " + sub.filePath.toString());
					}
				});

			}
		} catch (Throwable e) {
			log.log(IndexLog.EntryType.FATAL, e.getMessage(), e);
		} finally {
			done.accept(sub);
		}
	}

	private static void summary(ContentManager contentManager) {
		Map<Class<? extends Content>, Long> byType = contentManager.countByType();
		if (byType.size() > 0) {
			System.out.println("Current content by Type:");
			byType.forEach((type, count) -> System.out.printf(" > %s: %d%n", type.getSimpleName(), count));

			System.out.println("Current content by Game:");
			contentManager.countByGame().forEach((game, count) -> System.out.printf(" > %s: %d%n", game, count));
		} else {
			System.out.println("No content stored yet");
		}
	}

	private static void list(ContentManager contentManager, CLI cli) throws IOException {
		String game = cli.option("game", null);
		String type = cli.option("type", null);
		String author = cli.option("author", null);
		String name = cli.option("name", null);

		if (null == game && type == null && author == null && name == null) {
			System.err.println("Options to search by game, type, author or name are expected");
			System.exit(255);
		}

		Set<Content> results = new HashSet<>(contentManager.search(game, type, name, author));

		if (results.isEmpty()) {
			System.out.println("No results found");
		} else {
			System.out.printf("%-22s | %-10s | %-30s | %-20s | %s%n", "Game", "Type", "Name", "Author", "Hash");
			for (Content result : results) {
				System.out.printf("%-22s | %-10s | %-30s | %-20s | %s%n",
								  result.game, result.contentType,
								  result.name.substring(0, Math.min(20, result.name.length())),
								  result.author.substring(0, Math.min(20, result.author.length())),
								  result.hash);
			}
		}
	}

	private static void show(ContentManager contentManager, CLI cli) throws IOException {
		if (cli.commands().length < 2) {
			System.err.println("List of content hashes or names expected");
			System.exit(255);
		}

		Set<Content> results = new HashSet<>();

		String[] terms = Arrays.copyOfRange(cli.commands(), 1, cli.commands().length);
		for (String term : terms) {
			if (term.matches("[a-f0-9]{40}")) {
				Content found = contentManager.forHash(term);
				if (found != null) results.add(found);
			} else {
				results.addAll(contentManager.forName(term));
			}
		}

		if (results.isEmpty()) {
			System.out.printf("No results for terms %s found%n", Arrays.toString(terms));
		} else {
			for (Content result : results) {
				System.out.println(YAML.toString(result));
			}
		}
	}

	private static void scrape(CLI cli) throws IOException {
		if (cli.commands().length < 3) {
			System.err.println("A scraper type and base URL are required");
			System.exit(255);
		}

		switch (cli.commands()[1]) {
			case "autoindexphp":
				AutoIndexPHPScraper.index(cli);
				break;
			default:
				throw new UnsupportedOperationException("Scraper not supported: " + cli.commands()[1]);
		}

	}

	private static void download(CLI cli) throws IOException {
		if (cli.commands().length < 3) {
			System.err.println("An input file and output directory are required");
			System.exit(255);
		}

		Downloader.download(cli);
	}

	private static void unpack(CLI cli) throws IOException {
		if (cli.commands().length < 3) {
			System.err.println("A Umod file and destination directory are required!");
			System.exit(2);
		}

		Path umodFile = Paths.get(cli.commands()[1]);
		if (!Files.exists(umodFile)) {
			System.err.println("Umod file does not exist!");
			System.exit(4);
		}

		Path dest = Paths.get(cli.commands()[2]);
		if (!Files.isDirectory(dest)) {
			System.err.println("Destination directory does not exist!");
			System.exit(4);
		}

		Umod umod = new Umod(umodFile);
		ByteBuffer buffer = ByteBuffer.allocate(1024 * 8);
		for (Umod.UmodFile f : umod.files) {
			if (f.name.startsWith("System\\Manifest")) continue;

			System.out.printf("Unpacking %s ", f.name);
			Path out = dest.resolve(Util.filePath(f.name));

			if (!Files.exists(out)) Files.createDirectories(out);

			out = out.resolve(Util.fileName(f.name));

			System.out.printf("to %s%n", out);

			try (FileChannel fileChannel = FileChannel.open(out, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
															StandardOpenOption.TRUNCATE_EXISTING);
				 SeekableByteChannel fileData = f.read()) {

				while (fileData.read(buffer) > 0) {
					fileData.read(buffer);
					buffer.flip();
					fileChannel.write(buffer);
					buffer.clear();
				}
			}
		}
	}

	private static void usage() {
		System.out.println("Unreal Archive");
		System.out.println("Usage: unreal-archive.jar <command> [options]");
		System.out.println();
		System.out.println("Commands:");
		System.out.println("  index --content-path=<path> --input-path=<path>");
		System.out.println("    Index the contents of <input-path>, writing the results to <content-path>");
		System.out.println("  refresh --content-path=<path>");
		System.out.println("    Perform a liveliness check of all download URLs");
		System.out.println("  mirror --content-path=<path> --output-path=<path>");
		System.out.println("    Download all content in the index to <output-path>");
		System.out.println("  summary --content-path=<path>");
		System.out.println("    Show stats and counters for the content index in <content-path>");
		System.out.println("  ls [--game=<game>] [--type=<type>] [--author=<author>] --content-path=<path>");
		System.out.println("    List indexed content in <content-path>, filtered by game, type or author");
		System.out.println("  show [name ...] [hash ...] --content-path=<path>");
		System.out.println("    Show data for the content items specified");
		System.out.println("  unpack <umod-file> <destination> --content-path=<path>");
		System.out.println("    Unpack the contents of <umod-file> to directory <destination>");
		System.out.println("  scrape <type> <start-url> --style-prefix=<prefix> [--slowdown=<millis>] --content-path=<path>");
		System.out.println("    Scrape file listings from the provided URL, <type> is the type of scraper ");
		System.out.println("    to use ('autoindexphp' supported), and <style-prefix> is the prefix used in ");
		System.out.println("    styles on Autoindex PHP links. [slowdown] will cause the scraper to pause");
		System.out.println("    between page loads, defaults to 2000ms.");
		System.out.println("  download <file-list> <output-path> [--slowdown=<millis>] --content-path=<path>");
		System.out.println("    Download previously-scraped files defined in the file <file-list>, and write");
		System.out.println("    them out to <output-path>, along with a YML file containing the original URL.");
		System.out.println("    [slowdown] will cause the downloader to pause between downloads, defaults to 2000ms.");
	}
}
