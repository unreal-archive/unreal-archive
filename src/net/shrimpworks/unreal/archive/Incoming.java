package net.shrimpworks.unreal.archive;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.shrimpworks.unreal.packages.Umod;

public class Incoming implements Closeable {

	public final ContentSubmission submission;
	public final Path contentRoot;
	public final Path repack;
	public final String originalSha1;

	public final Map<String, Object> files;

	private final Set<Umod> umods;

	public Incoming(ContentSubmission submission, IndexLog log) throws IOException, UnsupportedOperationException {
		this.submission = submission;
		this.contentRoot = getRoot(submission.filePath);
		this.repack = getRepack(submission.filePath, contentRoot);
		this.originalSha1 = Util.sha1(submission.filePath);

		this.umods = new HashSet<>();
		this.files = listFiles(submission.filePath, contentRoot);
	}

	@Override
	public void close() throws IOException {
		for (Umod v : umods) {
			try {
				v.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		umods.clear();
		files.clear();

		if (repack != null) Files.deleteIfExists(repack);

		// clean up contentRoot
		if (contentRoot != null) {
			ArchiveUtil.cleanPath(contentRoot);
		}
	}

	private Path getRoot(Path incoming) throws IOException, UnsupportedOperationException {
		Path tempDir = Files.createTempDirectory("archive-incoming-");

		if (ArchiveUtil.isArchive(incoming)) {
			try {
				return ArchiveUtil.extract(incoming, tempDir, Duration.ofSeconds(30), true);
			} catch (InterruptedException e) {
				throw new IOException("Extract took too long", e);
			}
		} else {
			if (ContentIndexer.KNOWN_FILES.contains(Util.extension(incoming))) {
				return Files.copy(incoming, tempDir, StandardCopyOption.REPLACE_EXISTING);
			}
		}

		throw new UnsupportedOperationException("Can't unpack file " + incoming);
	}

	private Path getRepack(Path incoming, Path contentRoot) {
		String fName = incoming.getFileName().toString().toLowerCase();

		// leave zip files alone
		if (fName.endsWith(".zip")) return null;

		if (contentRoot != null) {
			// TODO add contentRoot to a zip file and return it
		}

		return null;
	}

	private Map<String, Object> listFiles(Path filePath, Path contentRoot) throws IOException {
		Map<String, Object> files = new HashMap<>();
		if (contentRoot != null && Files.exists(contentRoot)) {
			Files.walkFileTree(contentRoot, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (file.toString().toLowerCase().endsWith(".umod")) {
						files.putAll(umodFiles(file));
					} else {
						files.put(file.toString(), file);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}
		return files;
	}

	private Map<String, Umod.UmodFile> umodFiles(Path path) throws IOException {
		final Map<String, Umod.UmodFile> fileList = new HashMap<>();

		Umod umod = new Umod(path);
		umods.add(umod);

		for (Umod.UmodFile file : umod.files) {
			fileList.put(file.name.replaceAll("\\\\", "/"), file);
		}

		return fileList;
	}

	@Override
	public String toString() {
		return String.format("Incoming [submission=%s, contentRoot=%s, repack=%s, originalSha1=%s]",
							 submission, contentRoot, repack, originalSha1);
	}
}