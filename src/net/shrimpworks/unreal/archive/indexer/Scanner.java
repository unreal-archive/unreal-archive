package net.shrimpworks.unreal.archive.indexer;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import net.shrimpworks.unreal.archive.CLI;
import net.shrimpworks.unreal.archive.Util;

public class Scanner {

	private final ContentManager contentManager;
	private final CLI cli;

	public Scanner(ContentManager contentManager, CLI cli) {
		this.contentManager = contentManager;
		this.cli = cli;
	}

	public void scan(Path inputPath) throws IOException {
		// find all files within the scan path
		List<Path> all = findFiles(inputPath);

		System.err.printf("Found %d files to scan in %s%n", all.size(), inputPath);

		System.err.printf("%-100s %-6s %-12s %-12s %-20s%n",
						  "File",
						  "Known",
						  "Curr Type",
						  "Scan Type",
						  "Failure");

		AtomicInteger done = new AtomicInteger();

		all.stream().sorted().forEach(path -> {
			System.err.printf("[%d/%d] : %s \r", done.incrementAndGet(), all.size(), Util.fileName(path));

			Submission sub = new Submission(path);
			IndexLog log = new IndexLog(sub);

			scanFile(sub, log, r -> {
				System.err.printf("%-100s %-6s %-12s %-12s %-20s%n",
								  r.filePath,
								  r.known,
								  r.oldType,
								  r.newType,
								  r.failed != null ? r.failed.getClass().getSimpleName() : "-");
			});
		});

		System.err.printf("%nCompleted scanning %d files%n", done.get());
	}

	private List<Path> findFiles(Path inputPath) throws IOException {
		List<Path> all = new ArrayList<>();
		if (Files.isDirectory(inputPath)) {
			Files.walkFileTree(inputPath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					if (!Util.extension(file).equalsIgnoreCase("yml")) {
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
			incoming.prepare();

			content = contentManager.forHash(incoming.hash);

			classifiedType = ContentType.classify(incoming);

		} catch (Throwable e) {
			failed = e;
		} finally {
			if (failed == null) failed = log.log.stream()
												.filter(l -> l.type == IndexLog.EntryType.FATAL && l.exception != null)
												.map(l -> l.exception)
												.findFirst().orElse(null);

			done.accept(new ScanResult(
					sub.filePath.toString(),
					content != null ? "KNOWN" : "NEW",
					content != null ? content.contentType : "-",
					classifiedType.name(),
					failed
			));
		}
	}

	private static class ScanResult {

		private final String filePath;
		private final String known;
		private final String oldType;
		private final String newType;
		private final Throwable failed;

		public ScanResult(String filePath, String known, String oldType, String newType, Throwable failed) {
			this.filePath = filePath;
			this.known = known;
			this.oldType = oldType;
			this.newType = newType;
			this.failed = failed;
		}
	}
}
