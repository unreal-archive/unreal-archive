package net.shrimpworks.unreal.archive.indexer;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import net.shrimpworks.unreal.archive.CLI;
import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.YAML;

public class Indexer {

	private final ContentManager contentManager;
	private final CLI cli;

	public Indexer(ContentManager contentManager, CLI cli) {
		this.contentManager = contentManager;
		this.cli = cli;
	}

	public void index(Path inputPath, boolean force) throws IOException {
		final List<IndexLog> indexLogs = new ArrayList<>();

		// go through all the files in the input path and index them if new
		if (Files.isDirectory(inputPath)) {

			List<Submission> all = new ArrayList<>();
			Files.walkFileTree(inputPath, new SimpleFileVisitor<Path>() {

				final Map<Path, SubmissionOverride> override = new HashMap<>();

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (!Util.extension(file).equalsIgnoreCase("yml")) {
						Submission sub;
						// if there's asubmission file
						if (Files.exists(Paths.get(file.toString() + ".yml"))) {
							System.out.println("Submission exists, using it");
							sub = YAML.fromFile(Paths.get(file.toString() + ".yml"), Submission.class);
							sub.filePath = file;
						} else {
							sub = new Submission(file);
						}

						//if (override != null) sub.override = override;
						sub.override = override.get(file.getParent());
						all.add(sub);
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					// check if there's an override for this directory
					if (Files.exists(dir.resolve("_override.yml"))) {
						override.put(dir, YAML.fromFile(dir.resolve("_override.yml"), SubmissionOverride.class));
					}
					return super.preVisitDirectory(dir, attrs);
				}

			});

			System.out.printf("Found %d maps to index in %s%n", all.size(), inputPath);

			AtomicInteger done = new AtomicInteger();

			all.stream().sorted().forEach(sub -> {
				IndexLog log = new IndexLog(sub);
				indexLogs.add(log);

				indexFile(sub, log, force, c -> {
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

			indexFile(sub, log, force, c -> {
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

	private void indexFile(Submission sub, IndexLog log, boolean force, Consumer<Submission> done) {
		try (Incoming incoming = new Incoming(sub, log)) {
			Content content = contentManager.checkout(incoming.hash);

			if ((content != null && !force)) {
				// lets not support re-index yet, but we can update with urls if there are any
//				if (!content.deleted && sub.sourceUrls != null && sub.sourceUrls.length > 0) {
//					for (String url : sub.sourceUrls) {
//						if (!content.hasDownload(url)) {
//							content.downloads.add(new Content.Download(url, LocalDate.now(), false));
//						}
//					}
////					contentManager.checkin(content);
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
								if (!c.content.hasDownload(url)) {
									c.content.downloads.add(new Content.Download(url, LocalDate.now(), false));
								}
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
}
