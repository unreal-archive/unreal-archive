package org.unrealarchive.indexing;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.unrealarchive.common.Util;
import org.unrealarchive.common.YAML;
import org.unrealarchive.content.Download;
import org.unrealarchive.content.Games;
import org.unrealarchive.content.addons.Addon;
import org.unrealarchive.content.addons.SimpleAddonRepository;
import org.unrealarchive.content.addons.SimpleAddonType;

public class Indexer {

	static final Set<String> INCLUDE_TYPES = Set.of(
		"zip", "rar", "ace", "7z", "cab", "tgz", "gz", "tar", "bz2", "exe", "umod", "ut2mod", "ut4mod"
	);

	private final SimpleAddonRepository repo;
	private final ContentManager contentManager;
	private final IndexerEvents events;
	private final IndexerPostProcessor postProcessor;

	public Indexer(SimpleAddonRepository repo, ContentManager contentManager, IndexerEvents events) {
		this(repo, contentManager, events, new IndexerPostProcessor() {});
	}

	public Indexer(SimpleAddonRepository repo, ContentManager contentManager, IndexerEvents events, IndexerPostProcessor postProcessor) {
		this.repo = repo;
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
	 * {@link SimpleAddonType}.
	 * <p>
	 * When the Content Type is found, a new type-specific {@link Addon} instance will
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
	 * @param forceGame   if not null, forces this game
	 * @param inputPath   directories or file paths to index
	 * @throws IOException file access failure
	 */
	public void index(boolean force, boolean newOnly, int concurrency, SimpleAddonType forceType, Games forceGame, Path... inputPath)
		throws IOException {
		final List<IndexLog> indexLogs = new ArrayList<>();

		// keep a counter of number of files processed
		final AtomicInteger done = new AtomicInteger();
		final AtomicInteger allFound = new AtomicInteger();

		// create a task to feed workers with incoming files asynchronously
		final BlockingDeque<Submission> all = new LinkedBlockingDeque<>();
		final CompletableFuture<Void> filesTask = CompletableFuture.runAsync(() -> {
			for (Path p : inputPath) {
				try {
					findFiles(p, newOnly, all, allFound);
				} catch (IOException ex) {
					throw new RuntimeException("Failed to find files in path " + p, ex);
				}
			}
		});

		// generate the requested number of workers according to concurrency specified
		final CompletableFuture<?>[] workers = new CompletableFuture[concurrency];
		for (int i = 0; i < concurrency; i++) {
			workers[i] = CompletableFuture.runAsync(() -> {
				do {
					try {
						// keep waiting for files
						Submission sub = all.pollFirst(500, TimeUnit.MILLISECONDS);
						if (sub == null) continue;

						if (forceGame != null) sub.override.overrides.put("game", forceGame.name);

						IndexLog log = new IndexLog();
						indexLogs.add(log);

						indexFile(sub, log, force, forceType, result -> {
							events.indexed(sub, result, log);
							events.progress(done.incrementAndGet(), allFound.get(), sub.filePath);
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

	private void findFiles(Path inputPath, boolean newOnly, Deque<Submission> all, AtomicInteger allFound) throws IOException {
		if (Files.isDirectory(inputPath)) {
			Files.walkFileTree(inputPath, new SimpleFileVisitor<>() {

				final Map<Path, SubmissionOverride> overrides = new HashMap<>();

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					try {
						if (INCLUDE_TYPES.contains(Util.extension(file).toLowerCase())) {
							if (newOnly && repo.forHash(Util.hash(file)) != null) return FileVisitResult.CONTINUE;

							Submission sub;
							// if there's a submission file
							Path subFile = Paths.get(file + ".yml");
							if (Files.exists(subFile)) {
								sub = YAML.fromFile(subFile, Submission.class);
								sub.filePath = file;
							} else {
								sub = new Submission(file);
							}

							SubmissionOverride override = findOverride(file, overrides);
							if (override != null) sub.override = override;
							all.addLast(sub);
							allFound.incrementAndGet();
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
						overrides.put(dir, YAML.fromFile(dir.resolve("_override.yml"), SubmissionOverride.class));
					}
					return super.preVisitDirectory(dir, attrs);
				}

			});
		} else {
			if (newOnly && repo.forHash(Util.hash(inputPath)) != null) return;

			Submission sub;
			// if there's a submission file
			Path subFile = Paths.get(inputPath + ".yml");
			if (Files.exists(subFile)) {
				sub = YAML.fromFile(subFile, Submission.class);
				sub.filePath = inputPath;
			} else {
				sub = new Submission(inputPath);
			}

			SubmissionOverride override = findOverride(inputPath, new HashMap<>());
			if (override != null) sub.override = override;
			all.add(sub);
			allFound.incrementAndGet();
		}
	}

	private SubmissionOverride findOverride(Path file, Map<Path, SubmissionOverride> override) {
		Path parent = file.getParent();
		if (override.containsKey(parent)) return override.get(parent);

		// there's no immediate override in this directory, so walk up the free
		SubmissionOverride result = null;
		while (parent != null) {
			// there's already an override for this page
			if (override.containsKey(parent)) {
				result = override.get(parent);
				break;
			}

			// no existing override - maybe there's an override file here
			try {
				if (Files.exists(parent.resolve("_override.yml"))) {
					result = YAML.fromFile(parent.resolve("_override.yml"), SubmissionOverride.class);
					override.put(parent, result);
					break;
				}
			} catch (IOException e) {
				throw new RuntimeException("Failed to read override file in path " + parent, e);
			}

			// keep walking up the tree
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

	private void indexFile(
		Submission sub, IndexLog log, boolean force, SimpleAddonType forceType, Consumer<Optional<IndexResult<? extends Addon>>> done) {
		try (Incoming incoming = new Incoming(sub, log)) {
			identifyContent(incoming, force, forceType, (ident, content) -> {
				if (content == null || ident.contentType() == SimpleAddonType.UNKNOWN) {
					log.log(IndexLog.EntryType.CONTINUE, String.format("No content identified in %s", sub.filePath.getFileName()));
					done.accept(Optional.empty());
					return;
				}

				ident.indexer().get().index(incoming, content, result -> {
					try {
						Addon current = repo.forHash(incoming.hash);

						// hmm, post indexing cleanup... not great.
						result.content.name = result.content.name.trim();
						result.content.author = result.content.author.trim();

						// check if the item is a variation of existing content
						if (current == null) {
							Addon existing = repo.search(result.content.game, result.content.contentType,
														 result.content.name, result.content.author)
												 .stream().max(Comparator.comparing(a -> a.releaseDate))
												 .orElse(null);
							if (existing != null) {
								if (existing.variationOf == null && existing.releaseDate.compareTo(result.content.releaseDate) < 0) {
									Addon variation = contentManager.checkout(existing.hash);
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
								if (current.attachments.stream().anyMatch(a -> a.name.equals(f.name()))) {
									try {
										Files.deleteIfExists(f.path());
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
	 * @param done      called once content has been identified and instantiated, either result may be null
	 * @throws IOException failed to read content files
	 */
	private void identifyContent(Incoming incoming, boolean force, SimpleAddonType forceType,
								 BiConsumer<AddonClassifier.AddonIdentifier, Addon> done) throws IOException {
		Addon content = contentManager.checkout(incoming.hash);

		if ((content != null && !force)) {
			// even when not forcing a full re-index of something, we can still update download sources
			if (!content.deleted && incoming.submission.sourceUrls != null) {
				for (String url : incoming.submission.sourceUrls) {
					if (url != null && !url.isEmpty() && !content.hasDownload(url)) {
						content.downloads.add(new Download(url));
					}
				}
				contentManager.checkin(new IndexResult<>(content, Collections.emptySet()), incoming.submission);
			}
			content = null;
		}

		incoming.prepare();

		AddonClassifier.AddonIdentifier ident = forceType == null
			? AddonClassifier.classify(incoming)
			: AddonClassifier.identifierForType(forceType);

		// TODO better way to handle re-indexing - we already have content, but if type changes we can't re-use it
		if (content == null || !ident.contentType().toString().equalsIgnoreCase(content.contentType)) {
			content = AddonClassifier.newContent(ident, incoming);
		}

		done.accept(ident, content);
	}

	public interface IndexerPostProcessor {

		public default void indexed(Submission sub, Addon before, IndexResult<? extends Addon> result) {
			// if there are additional source urls specified in the submission metadata, append it to the result
			if (sub.sourceUrls != null) {
				for (String url : sub.sourceUrls) {
					if (url != null && !url.isEmpty() && !result.content.hasDownload(url)) {
						result.content.downloads.add(new Download(url));
					}
				}
			}
		}
	}

	public interface IndexerEvents {

		public void starting(int foundFiles);

		public void progress(int indexed, int total, Path currentFile);

		public void indexed(Submission submission, Optional<IndexResult<? extends Addon>> indexed, IndexLog log);

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
		public void indexed(Submission submission, Optional<IndexResult<? extends Addon>> indexed, IndexLog log) {
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
