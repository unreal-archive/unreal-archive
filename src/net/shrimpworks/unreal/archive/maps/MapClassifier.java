package net.shrimpworks.unreal.archive.maps;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

import net.shrimpworks.unreal.archive.ContentClassifier;
import net.shrimpworks.unreal.archive.ContentSubmission;
import net.shrimpworks.unreal.packages.Umod;

import static java.util.Arrays.stream;

public class MapClassifier implements ContentClassifier.Classifier {

	private static final String[] MAP_EXTENSIONS = new String[] { ".unr", ".ut2" };

	@Override
	public boolean classify(ContentSubmission submission) {
		final String fileName = submission.filePath.getFileName().toString();

		// no need to look further if it's just a map
		boolean isMap = stream(MAP_EXTENSIONS).anyMatch(e -> fileName.toLowerCase().endsWith(e));
		if (isMap) return true;

		Set<String> files = fileList(submission);

		// find all map files in the archive
		long count = files.stream().filter(f -> stream(MAP_EXTENSIONS).anyMatch(e -> f.toLowerCase().endsWith(e))).count();

		// a bit naive, if there's a one-map mod, it would be caught here
		return count == 1;
	}

	private Set<String> fileList(ContentSubmission submission) {
		final Set<String> fileList = new HashSet<>();

		if (submission.filePath.getFileName().toString().endsWith(".zip")) {
			try {
				FileSystem fs = FileSystems.newFileSystem(submission.filePath, null);
				Files.walkFileTree(fs.getPath("/"), new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						if (file.toString().toLowerCase().endsWith(".umod")) {
							fileList.addAll(umodFiles(file));
						} else {
							fileList.add(file.toString());
						}
						return FileVisitResult.CONTINUE;
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			throw new UnsupportedOperationException("Unsupported archive " + submission.filePath.getFileName().toString());
		}

		return fileList;
	}

	private Set<String> umodFiles(Path path) throws IOException {
		final Set<String> fileList = new HashSet<>();

		try (Umod umod = new Umod(path)) {
			for (Umod.UmodFile file : umod.files) {
				fileList.add(file.name);
			}
		}

		return fileList;
	}
}
