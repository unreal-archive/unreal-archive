package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

	private static final String OPTION_PATTERN = "--([a-zA-Z0-9-_]+)=(.+)?";

	public static void main(String[] args) throws IOException {
		Map<String, String> options = parseCLI(Collections.emptyMap(), args);

		if (options.get("content-path") == null) {
			System.err.println("content-path must be specified!");
			System.exit(1);
		}

		if (options.get("input-path") == null) {
			System.err.println("input-path must be specified!");
			System.exit(1);
		}

		Path contentPath = Paths.get(options.get("content-path"));
		if (!Files.isDirectory(contentPath)) {
			System.err.println("content-path must be a directory!");
			System.exit(2);
		}

		Path inputPath = Paths.get(options.get("input-path"));
		if (!Files.exists(inputPath)) {
			System.err.println("input-path does not exist!");
			System.exit(3);
		}

		final List<IndexLog> indexLogs = new ArrayList<>();

		final ContentManager contentManager = new ContentManager(contentPath);

		Map<Class<? extends Content>, Long> byType = contentManager.countByType();
		if (byType.size() > 0) {
			System.out.println("Current content by Type:");
			byType.forEach((type, count) -> System.out.printf(" > %s: %d%n", type.getSimpleName(), count));

			System.out.println("Current content by Game:");
			contentManager.countByGame().forEach((game, count) -> System.out.printf(" > %s: %d%n", game, count));
		} else {
			System.out.println("No content stored yet");
		}

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
//			System.out.println("Failed processing " + f.toString());
//			e.printStackTrace();
		}
	}

	private static Map<String, String> parseCLI(Map<String, String> defOptions, String... args) {
		final Map<String, String> props = new HashMap<>();

		// populate default options
		props.putAll(defOptions);

		Pattern optPattern = Pattern.compile(OPTION_PATTERN);

		final StringBuilder commandBuilder = new StringBuilder();

		for (String arg : args) {
			Matcher optMatcher = optPattern.matcher(arg);

			if (optMatcher.matches()) {
				props.put(optMatcher.group(1), optMatcher.group(2) == null ? "" : optMatcher.group(2));
			}
		}
		return props;
	}

}
