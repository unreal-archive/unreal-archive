package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
			Files.list(inputPath).sorted().forEach(f -> {
				ContentSubmission sub = new ContentSubmission(f);
				IndexLog log = new IndexLog(sub);
				indexLogs.add(log);

				indexFile(sub, contentManager, log);
			});
		} else {
			ContentSubmission sub = new ContentSubmission(inputPath);
			IndexLog log = new IndexLog(sub);
			indexLogs.add(log);

			indexFile(sub, contentManager, log);
		}

		int err = 0;

		for (IndexLog l : indexLogs) {
			if (!l.ok()) {
				err++;
				System.out.println(l);
			}
		}

		System.out.printf("%nCompleted indexing %d files, with %d errors%n", indexLogs.size(), err);
	}

	private static void indexFile(ContentSubmission sub, ContentManager contentManager, IndexLog log) {
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
			} else {
				log.log(IndexLog.EntryType.FATAL, "File " + sub.filePath + " cannot be classified.");
			}
		} catch (Throwable e) {
			log.log(IndexLog.EntryType.FATAL, e.getMessage(), e);
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
		System.out.println("  ls [game] [type] --content-path=<path>");
		System.out.println("    List indexed content in <content-path>, filtered by game or type");
		System.out.println("  show [name ...] [hash ...] --content-path=<path>");
		System.out.println("    Show data for the content items specified");
	}
}
