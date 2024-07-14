package org.unrealarchive.indexing;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.unrealarchive.common.CLI;
import org.unrealarchive.common.Util;
import org.unrealarchive.content.addons.Addon;
import org.unrealarchive.content.addons.SimpleAddonRepository;
import org.unrealarchive.content.addons.SimpleAddonType;

public class Scanner {

	private final SimpleAddonRepository repository;

	private final boolean newOnly;
	private final Pattern nameInclude;
	private final Pattern nameExclude;
	private final long maxFileSize;
	private final int concurrency;

	public Scanner(SimpleAddonRepository repository, CLI cli) {
		this.repository = repository;

		this.newOnly = cli.option("new-only", "").equalsIgnoreCase("true") || cli.option("new-only", "").equalsIgnoreCase("1");
		this.maxFileSize = Long.parseLong(cli.option("max-size", "0"));
		this.concurrency = Integer.parseInt(cli.option("concurrency", "1"));

		if (cli.option("include", "").isBlank()) {
			this.nameInclude = null;
		} else {
			this.nameInclude = Pattern.compile(cli.option("include", ""));
		}

		if (cli.option("exclude", "").isBlank()) {
			this.nameExclude = null;
		} else {
			this.nameExclude = Pattern.compile(cli.option("exclude", ""));
		}
	}

	public void scan(ScannerEvents events, Path... inputPath) throws IOException {
		// find all files within the scan path
		List<Path> all = new ArrayList<>();
		for (Path p : inputPath) {
			all.addAll(findFiles(p));
		}

		events.starting(all.size(), nameInclude, nameExclude);

		AtomicInteger done = new AtomicInteger();

		try (ForkJoinPool fjPool = new ForkJoinPool(concurrency)) {
			fjPool.submit(() -> all.parallelStream().sorted().forEachOrdered(path -> {
							  events.progress(done.incrementAndGet(), all.size(), path);

							  Submission sub = new Submission(path);
							  IndexLog log = new IndexLog();

							  scanFile(sub, log, events::scanned);
						  })
			).join();
		}

		events.completed(done.get());
	}

	private List<Path> findFiles(Path inputPath) throws IOException {
		List<Path> all = new ArrayList<>();
		if (Files.isDirectory(inputPath)) {
			Files.walkFileTree(inputPath, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					if (Indexer.INCLUDE_TYPES.contains(Util.extension(file).toLowerCase())) {

						try {
							if (maxFileSize > 0 && Files.size(file) > maxFileSize) {
								return FileVisitResult.CONTINUE;
							}
						} catch (Exception ignored) {
							//
						}

						if (nameInclude != null && !nameInclude.matcher(file.getFileName().toString()).matches()) {
							return FileVisitResult.CONTINUE;
						}

						if (nameExclude != null && nameExclude.matcher(file.getFileName().toString()).matches()) {
							return FileVisitResult.CONTINUE;
						}

						all.add(file);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} else if (Files.exists(inputPath) && Files.isRegularFile(inputPath)) {
			all.add(inputPath);
		}

		return all;
	}

	private void scanFile(Submission sub, IndexLog log, Consumer<ScanResult> done) {
		Throwable failed = null;
		Addon content = null;
		SimpleAddonType classifiedType = SimpleAddonType.UNKNOWN;

		try (Incoming incoming = new Incoming(sub, log)) {
			content = repository.forHash(incoming.hash);

			if (newOnly && content != null) return;

			incoming.prepare();

			classifiedType = AddonClassifier.classify(incoming).contentType();

		} catch (Throwable e) {
			failed = e;
		} finally {
			if (!newOnly || content == null) {
				if (failed == null) failed = log.log.stream()
													.filter(l -> l.type == IndexLog.EntryType.FATAL && l.exception != null)
													.map(l -> l.exception)
													.findFirst().orElse(null);

				done.accept(new ScanResult(
					sub.filePath,
					content != null,
					content != null ? SimpleAddonType.valueOf(content.contentType) : null,
					classifiedType,
					failed
				));
			}
		}
	}

	public record ScanResult(Path filePath, boolean known, SimpleAddonType oldType, SimpleAddonType newType, Throwable failed) {

		@Override
		public String toString() {
			return String.format("ScanResult [filePath=%s, known=%s, oldType=%s, newType=%s, failed=%s]",
								 filePath, known, oldType, newType, failed);
		}
	}

	public interface ScannerEvents {

		public void starting(int foundFiles, Pattern included, Pattern excluded);

		public void progress(int scanned, int total, Path currentFile);

		public void scanned(ScanResult scanned);

		public void completed(int scannedFiles);
	}

	public static class CLIEventPrinter implements ScannerEvents {

		@Override
		public void starting(int foundFiles, Pattern included, Pattern excluded) {
			System.err.printf("Found %d file(s) to scan %s %s%n", foundFiles,
							  included != null ? "matching " + included.pattern() : "",
							  excluded != null ? "excluding " + excluded.pattern() : ""
			);

			System.err.printf("%s;%s;%s;%s;%s%n",
							  "File",
							  "Known",
							  "Current Type",
							  "Scanned Type",
							  "Failure");
		}

		@Override
		public void progress(int scanned, int total, Path currentFile) {
			System.err.printf("[%d/%d] : %s \r", scanned, total, Util.fileName(currentFile));
		}

		@Override
		public void scanned(ScanResult scanned) {
			System.out.printf("%s;%s;%s;%s;%s%n",
							  scanned.filePath,
							  scanned.known ? "KNOWN" : "NEW",
							  scanned.oldType != null ? scanned.oldType.name() : "-",
							  scanned.newType.name(),
							  scanned.failed != null ? (String.format("%s(%s)", scanned.failed.getClass().getSimpleName(),
																	  scanned.failed.getMessage())) : "-");
		}

		@Override
		public void completed(int scannedFiles) {
			System.err.printf("%nCompleted scanning %d files%n", scannedFiles);
		}
	}
}
