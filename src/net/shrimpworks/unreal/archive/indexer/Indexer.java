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

	/**
	 * Indexes a path or individual file.
	 * <p>
	 * Indexing process (indexing a path):
	 * <p>
	 * Each directory is checked for file <code>_override.yml</code>, which contains a
	 * collection of free-form key-value overrides which individual content indexing
	 * handlers may choose to reference to allow overriding specific pieces of data.
	 * This file's structure is defined as per {@link SubmissionOverride}.
	 * <p>
	 * Then, each file found within a directory is added to a collection to be
	 * classified and then indexed. Additionally, if a file with the same name of the
	 * file to be indexed with a <code>.yml</code> extension is found, this file is
	 * also loaded, and may contain additional file-specific information.
	 * This file's structure is defined as per {@link Submission}.
	 * <p>
	 * Once a file is found, an {@link Incoming} instance for it will be created, and
	 * it will be classified using a {@link Classifier}, to determine its
	 * {@link ContentType}.
	 * <p>
	 * When the Content Type is found, a new type-specific {@link Content} instance will
	 * be created with as many generic properties filled as possible. The incomplete
	 * content will be handed to and processed via the associated {@link IndexHandler}
	 * implementation, which further enriches it, and finally returns it via a
	 * {@link Consumer}.
	 *
	 * @param inputPath directory or path to index
	 * @param force     if content has already been indexed, index it again
	 * @throws IOException file access failure
	 */
	public void index(Path inputPath, boolean force) throws IOException {
		final List<IndexLog> indexLogs = new ArrayList<>();

		// go through all the files in the input path and index them if new
		List<Submission> all = findFiles(inputPath);

		System.out.printf("Found %d files to index in %s%n", all.size(), inputPath);

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

		int err = 0;

		for (IndexLog l : indexLogs) {
			if (!l.ok()) err++;
		}

		System.out.printf("%nCompleted indexing %d files, with %d errors%n", indexLogs.size(), err);
	}

	private List<Submission> findFiles(Path inputPath) throws IOException {
		List<Submission> all = new ArrayList<>();
		if (Files.isDirectory(inputPath)) {
			Files.walkFileTree(inputPath, new SimpleFileVisitor<Path>() {

				final Map<Path, SubmissionOverride> override = new HashMap<>();

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (!Util.extension(file).equalsIgnoreCase("yml")) {
						Submission sub;
						// if there's a submission file
						if (Files.exists(Paths.get(file.toString() + ".yml"))) {
							System.out.println("Submission exists, using it");
							sub = YAML.fromFile(Paths.get(file.toString() + ".yml"), Submission.class);
							sub.filePath = file;
						} else {
							sub = new Submission(file);
						}

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
		} else {
			Submission sub;
			// if there's a submission file
			if (Files.exists(Paths.get(inputPath.toString() + ".yml"))) {
				System.out.println("Submission exists, using it");
				sub = YAML.fromFile(Paths.get(inputPath.toString() + ".yml"), Submission.class);
				sub.filePath = inputPath;
			} else {
				sub = new Submission(inputPath);
			}

			// even a single file should respect directory overrides
			if (Files.exists(inputPath.getParent().resolve("_override.yml"))) {
				sub.override = YAML.fromFile(inputPath.getParent().resolve("_override.yml"), SubmissionOverride.class);
			}

			all.add(sub);
		}

		return all;
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

			// TODO better way to handle re-indexing - we already have content, but if type changes we can't re-use it
			if (content == null || type.toString().equalsIgnoreCase(content.contentType)) {
				content = type.newContent(incoming);
			}

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
