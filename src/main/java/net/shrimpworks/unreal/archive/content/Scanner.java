package net.shrimpworks.unreal.archive.content;

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

import net.shrimpworks.unreal.archive.CLI;
import net.shrimpworks.unreal.archive.Util;

public class Scanner {

	private final ContentManager contentManager;

	private final boolean newOnly;
	private final Pattern nameMatch;
	private final Pattern nameExclude;
	private final long maxFileSize;
	private final int concurrency;

	public Scanner(ContentManager contentManager, CLI cli) {
		this.contentManager = contentManager;

		this.newOnly = cli.option("new-only", "").equalsIgnoreCase("true") || cli.option("new-only", "").equalsIgnoreCase("1");
		this.maxFileSize = Long.parseLong(cli.option("max-size", "0"));
		this.concurrency = Integer.parseInt(cli.option("concurrency", "1"));

		if (cli.option("match", "").isEmpty()) {
			this.nameMatch = null;
		} else {
			this.nameMatch = Pattern.compile(cli.option("match", ""));
		}

		if (cli.option("exclude", "").isEmpty()) {
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

		events.starting(all.size(), nameMatch, nameExclude);

		AtomicInteger done = new AtomicInteger();

		ForkJoinPool fjPool = new ForkJoinPool(concurrency);
		fjPool.submit(() -> all.parallelStream().sorted().forEach(path -> {
						  events.progress(done.incrementAndGet(), all.size(), path);

						  Submission sub = new Submission(path);
						  IndexLog log = new IndexLog(sub);

						  scanFile(sub, log, events::scanned);
					  })
		).join();

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

						if (nameMatch != null && !nameMatch.matcher(file.getFileName().toString()).matches()) {
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
		Content content = null;
		ContentType classifiedType = ContentType.UNKNOWN;

		try (Incoming incoming = new Incoming(sub, log)) {
			content = contentManager.forHash(incoming.hash);

			if (newOnly && content != null) return;

			incoming.prepare();

			classifiedType = ContentType.classify(incoming);

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
						content != null ? ContentType.valueOf(content.contentType) : null,
						classifiedType,
						failed
				));
			}
		}
	}

	public static class ScanResult {

		public final Path filePath;
		public final boolean known;
		public final ContentType oldType;
		public final ContentType newType;
		public final Throwable failed;

		public ScanResult(Path filePath, boolean known, ContentType oldType, ContentType newType, Throwable failed) {
			this.filePath = filePath;
			this.known = known;
			this.oldType = oldType;
			this.newType = newType;
			this.failed = failed;
		}

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
