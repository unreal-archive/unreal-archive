package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
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

public class Main {

	public static void main(String[] args) throws IOException {
		final CLI cli = CLI.parse(Collections.emptyMap(), args);

		if (cli.commands().length == 0) {
			usage();
			System.exit(1);
		}

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
		System.out.printf("Loaded content index with %d items in %.2fs%n",
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

			List<Path> all = new ArrayList<>();
			Files.walkFileTree(inputPath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					if (!Util.extension(file).equalsIgnoreCase("yml")) all.add(file);
					return FileVisitResult.CONTINUE;
				}
			});

			System.out.printf("Found %d maps to index in %s%n", all.size(), inputPath);

			AtomicInteger done = new AtomicInteger();

			all.stream().sorted().forEach(f -> {
				ContentSubmission sub = new ContentSubmission(f);
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
			ContentSubmission sub = new ContentSubmission(inputPath);
			IndexLog log = new IndexLog(sub);
			indexLogs.add(log);

			indexFile(sub, contentManager, log, c -> {
			});
		}

		int err = 0;

		for (IndexLog l : indexLogs) {
			if (!l.ok()) err++;
		}

		System.out.printf("%nCompleted indexing %d files, with %d errors%n", indexLogs.size(), err);
	}

	private static void indexFile(ContentSubmission sub, ContentManager contentManager, IndexLog log, Consumer<ContentSubmission> done) {
		try (Incoming incoming = new Incoming(sub, log)) {
			Content content = contentManager.checkout(incoming.hash);

			if (content != null) {
				// lets not support re-index yet
				return;
			}

			incoming.prepare();

			ContentClassifier.ContentType type = ContentClassifier.classify(incoming, log);

			content = type.newContent(incoming);

			if (type != ContentClassifier.ContentType.UNKNOWN) { // TODO later support a generic dumping ground for unknown content

				type.indexer.get().index(incoming, content, log, c -> {
					try {
						c.content.lastIndex = LocalDateTime.now();
						if (sub.sourceUrls != null && sub.sourceUrls.length > 0) {
							for (String url : sub.sourceUrls) {
								c.content.downloads.add(new Download(url, LocalDate.now(), false));
							}
						}

						// TODO upload file to our storage, and add to downloads url set

//							Path repack = incoming.getRepack(c.name);

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
	}
}
