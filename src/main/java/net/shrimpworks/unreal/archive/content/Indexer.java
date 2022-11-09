package net.shrimpworks.unreal.archive.content;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.YAML;

public class Indexer {

	static final Set<String> INCLUDE_TYPES = new HashSet<>(Arrays.asList(
		"zip", "rar", "ace", "7z", "cab", "tgz", "gz", "tar", "bz2", "exe", "umod", "ut2mod", "ut4mod"
	));

	private final ContentManager contentManager;
	private final IndexerEvents events;
	private final IndexerPostProcessor postProcessor;

	public Indexer(ContentManager contentManager, IndexerEvents events) {
		this(contentManager, events, new IndexerPostProcessor() {});
	}

	public Indexer(ContentManager contentManager, IndexerEvents events, IndexerPostProcessor postProcessor) {
		this.contentManager = contentManager;
		this.events = events;
		this.postProcessor = postProcessor;
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
	 * @param force       if content has already been indexed, index it again
	 * @param newOnly     if true, only attempt to index content with unknown file hashes
	 * @param concurrency number of worker threads to use for indexing; defaults to 1.
	 *                    useful when indexing large directories of content
	 * @param forceType   if not null, use the specified content type, rather than
	 *                    attempting to discover it automatically
	 * @param inputPath   directories or file paths to index
	 * @throws IOException file access failure
	 */
	public void index(boolean force, boolean newOnly, int concurrency, ContentType forceType, Path... inputPath) throws IOException {
		final List<IndexLog> indexLogs = new ArrayList<>();

		// create a task to feed workers with incoming files asynchronously
		final BlockingDeque<Submission> all = new LinkedBlockingDeque<>();
		final CompletableFuture<Void> filesTask = CompletableFuture.runAsync(() -> {
			for (Path p : inputPath) {
				try {
					findFiles(p, newOnly, all);
				} catch (IOException ex) {
					throw new RuntimeException("Failed to find files in path " + p, ex);
				}
			}
		});

		// keep a counter of number of files processed
		final AtomicInteger done = new AtomicInteger();

		// generate the requested number of workers according to concurrency specified
		final CompletableFuture<?>[] workers = new CompletableFuture[concurrency];
		for (int i = 0; i < concurrency; i++) {
			workers[i] = CompletableFuture.runAsync(() -> {
				do {
					try {
						// keep waiting for files
						Submission sub = all.pollFirst(500, TimeUnit.MILLISECONDS);
						if (sub == null) continue;

						IndexLog log = new IndexLog();
						indexLogs.add(log);

						indexFile(sub, log, force, forceType, result -> {
							events.indexed(sub, result, log);
							events.progress(done.incrementAndGet(), all.size(), sub.filePath);
						});
					} catch (InterruptedException e) {
						System.err.printf("Error encountered while processing index queue: %s%n", e.getMessage());
					}
				} while (!filesTask.isDone() || !all.isEmpty());
				// end when after we've completed work, and there are no more files incoming
			});
		}

		// wait for all workers to complete
		CompletableFuture.allOf(workers).join();

		int errorCount = 0;

		for (IndexLog l : indexLogs) {
			if (!l.ok()) errorCount++;
		}

		events.completed(indexLogs.size(), errorCount);
	}

	private void findFiles(Path inputPath, boolean newOnly, Deque<Submission> all) throws IOException {
		if (Files.isDirectory(inputPath)) {
			Files.walkFileTree(inputPath, new SimpleFileVisitor<>() {

				final Map<Path, SubmissionOverride> override = new HashMap<>();

				private SubmissionOverride findOverride(Path file) {
					Path parent = file.getParent();
					if (override.containsKey(parent)) return override.get(parent);

					// there's no immediate override in this directory, so walk up the free
					SubmissionOverride result = null;
					while (parent != null) {
						if (override.containsKey(parent)) {
							result = override.get(parent);
							break;
						}
						parent = parent.getParent();
					}

					// this is a bit of double work, but we will do it once per subdirectory, rather than once per file
					if (result != null) {
						parent = file.getParent();
						// as long as there's no override in a specific directory, add the top most parent
						while (parent != null && !override.containsKey(parent)) {
							override.put(parent, result);
							parent = parent.getParent();
						}
					}

					return result;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					try {
						if (INCLUDE_TYPES.contains(Util.extension(file).toLowerCase())) {
							if (newOnly && contentManager.forHash(Util.hash(file)) != null) return FileVisitResult.CONTINUE;

							Submission sub;
							// if there's a submission file
							Path subFile = Paths.get(file + ".yml");
							if (Files.exists(subFile)) {
								sub = YAML.fromFile(subFile, Submission.class);
								sub.filePath = file;
							} else {
								sub = new Submission(file);
							}

							SubmissionOverride override = findOverride(file);
							if (override != null) sub.override = override;
							all.addLast(sub);
						}
					} catch (Throwable t) {
						throw new IOException("Failed to read file " + file, t);
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
			Path subFile = Paths.get(inputPath + ".yml");
			if (Files.exists(subFile)) {
				sub = YAML.fromFile(subFile, Submission.class);
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
	}

	private void indexFile(
		Submission sub, IndexLog log, boolean force, ContentType forceType, Consumer<Optional<IndexResult<? extends Content>>> done) {
		try (Incoming incoming = new Incoming(sub, log)) {
			Content content = prepContent(incoming, force, forceType);

			ContentType type;
			if (content == null || (type = ContentType.valueOf(content.contentType)) == ContentType.UNKNOWN) return;

			type.indexer.get().index(incoming, content, result -> {
				try {
					Content current = contentManager.forHash(incoming.hash);

					// check if the item is a variation of existing content
					if (current == null) {
						Optional<Content> maybeNewest = contentManager.search(result.content.game, result.content.contentType,
																			  result.content.name, result.content.author)
																	  .stream().max(Comparator.comparing(a -> a.releaseDate));
						Content existing = maybeNewest.orElse(null);
						if (existing != null) {
							if (existing.variationOf == null && existing.releaseDate.compareTo(result.content.releaseDate) < 0) {
								Content variation = contentManager.checkout(existing.hash);
								variation.variationOf = result.content.hash;
								contentManager.checkin(new IndexResult<>(variation, Collections.emptySet()), null);
								log.log(IndexLog.EntryType.CONTINUE,
										String.format("Flagging original content %s variation", existing.originalFilename));
							} else {
								result.content.variationOf = existing.hash;
								log.log(IndexLog.EntryType.CONTINUE,
										String.format("Flagging as variation of %s", existing.originalFilename));
							}
						}
					}

					// add dependencies
					result.content.dependencies = IndexUtils.dependencies(result.content, incoming);

					postProcessor.indexed(sub, current, result);

					if (result.content.name.isEmpty()) {
						throw new IllegalStateException("Name cannot be blank for " + incoming.submission.filePath);
					}

					// before checkin, remove any "new" attachments which already exist... this is a bit of a hack
					if (current != null) {
						result.files.removeIf(f -> {
							if (current.attachments.stream().anyMatch(a -> a.name.equals(f.name))) {
								try {
									Files.deleteIfExists(f.path);
								} catch (IOException e) {
									log.log(IndexLog.EntryType.CONTINUE, "Failed to delete duplicate attachment" + f, e);
								}
								return true;
							}
							return false;
						});
					}

					contentManager.checkin(result, incoming.submission);
				} catch (IOException e) {
					log.log(IndexLog.EntryType.FATAL, "Failed to store content file data for " + sub.filePath.toString(), e);
				}

				done.accept(Optional.of(result));
			});
		} catch (Throwable e) {
			log.log(IndexLog.EntryType.FATAL, e.getMessage(), e);
			done.accept(Optional.empty());
		}
	}

	/**
	 * Prepare and identify content for indexing.
	 *
	 * @param incoming  incoming content to be indexed
	 * @param force     whether to force re-indexing of known content
	 * @param forceType force the content type to this, null to auto-detect
	 * @return null if known, or a new Content instance otherwise
	 * @throws IOException failed to read content files
	 */
	private Content prepContent(Incoming incoming, boolean force, ContentType forceType) throws IOException {
		Content content = contentManager.checkout(incoming.hash);

		if ((content != null && !force)) {
			// even when not forcing a full re-index of something, we can still update download sources
			if (!content.deleted && incoming.submission.sourceUrls != null) {
				for (String url : incoming.submission.sourceUrls) {
					if (url != null && !url.isEmpty() && !content.hasDownload(url)) {
						content.downloads.add(new Content.Download(url, false));
					}
				}
				contentManager.checkin(new IndexResult<>(content, Collections.emptySet()), incoming.submission);
			}
			return null;
		}

		incoming.prepare();

		ContentType type = forceType == null ? ContentType.classify(incoming) : forceType;

		// TODO better way to handle re-indexing - we already have content, but if type changes we can't re-use it
		if (content == null || !type.toString().equalsIgnoreCase(content.contentType)) {
			content = type.newContent(incoming);
		}

		return content;
	}

	public interface IndexerPostProcessor {

		public default void indexed(Submission sub, Content before, IndexResult<? extends Content> result) {
			if (sub.sourceUrls != null) {
				for (String url : sub.sourceUrls) {
					if (url != null && !url.isEmpty() && !result.content.hasDownload(url)) {
						result.content.downloads.add(new Content.Download(url, false));
					}
				}
			}
		}
	}

	public interface IndexerEvents {

		public void starting(int foundFiles);

		public void progress(int indexed, int total, Path currentFile);

		public void indexed(Submission submission, Optional<IndexResult<? extends Content>> indexed, IndexLog log);

		public void completed(int indexedFiles, int errorCount);
	}

	public static class CLIEventPrinter implements IndexerEvents {

		private final boolean verbose;

		public CLIEventPrinter(boolean verbose) {
			this.verbose = verbose;
		}

		@Override
		public void starting(int foundFiles) {
			System.out.printf("Found %d file(s) to index%n", foundFiles);
		}

		@Override
		public void progress(int indexed, int total, Path currentFile) {
			System.out.printf("Completed %d of %d\r", indexed, total);
		}

		@Override
		public void indexed(Submission submission, Optional<IndexResult<? extends Content>> indexed, IndexLog log) {
			for (IndexLog.LogEntry l : log.log) {
				System.out.printf("[%s] %s: %s%n", l.type, Util.fileName(submission.filePath.getFileName()), l.message);
				if (l.exception != null && verbose) {
					l.exception.printStackTrace(System.out);
				}
			}
		}

		@Override
		public void completed(int indexedFiles, int errorCount) {
			System.out.printf("%nCompleted indexing %d files, with %d errors%n", indexedFiles, errorCount);
		}
	}

}
