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
		if (!Files.isDirectory(inputPath)) {
			System.err.println("input-path must be a directory!");
			System.exit(3);
		}

		final List<IndexLog> indexLogs = new ArrayList<>();

		final ContentManager contentManager = new ContentManager(contentPath);

		Files.list(inputPath).sorted().forEach(f -> {

			ContentSubmission sub = new ContentSubmission(f);
			IndexLog log = new IndexLog(sub);
			indexLogs.add(log);

			try (Incoming incoming = new Incoming(sub, log)) {
				ContentClassifier.ContentType type = ContentClassifier.classify(incoming, log);

				if (type != ContentClassifier.ContentType.UNKNOWN) { // TODO later support a generic dumping ground for unknown content

					type.indexer.get().index(incoming, type.newContent(incoming), log, c -> {
						try {
							c.lastIndex = LocalDateTime.now();
							if (sub.sourceUrls != null && sub.sourceUrls.length > 0) {
								for (String url : sub.sourceUrls) {
									c.downloads.add(new Download(url, LocalDate.now(), false));
								}
							}

							// TODO upload file to our storage, and add to downloads url set

//							Path repack = incoming.getRepack(c.name);

							YAML.toString(c);
						} catch (IOException e) {
							System.out.println("Failed to output " + f.toString());
							e.printStackTrace();
						}
					});
				} else {
					log.log(IndexLog.EntryType.FATAL, "File " + f + " cannot be classified.");
				}
			} catch (Throwable e) {
				log.log(IndexLog.EntryType.FATAL, e.getMessage(), e);
				System.out.println("Failed processing " + f.toString());
				e.printStackTrace();
			}
		});

		int err = 0;

		for (IndexLog l : indexLogs) {
			if (!l.ok()) {
				err++;
				System.out.println(l);
			}
		}

		System.out.printf("%nCompleted indexing %d files, with %d errors%n", indexLogs.size(), err);
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
