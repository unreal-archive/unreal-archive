package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ArchiveUtil {

	private static final Duration KILL_WAIT = Duration.ofSeconds(1);

	private static final Set<String> ARCHIVES = new HashSet<>(Arrays.asList(
			"zip", "z", "gz", "7z", "rar", "lzh", "exe"
	));

	private static final String SEVENZIP_BIN = "/usr/bin/7z";
	private static final String UNRAR_BIN = "/usr/bin/unrar";
	private static final String ZIP_BIN = "/usr/bin/zip";

	public static boolean isArchive(Path path) {
		if (!Files.isRegularFile(path)) return false;
		return ARCHIVES.contains(Util.extension(path.toString().toLowerCase()));
	}

	public static Path extract(Path source, Path destination, Duration timeout)
			throws IOException, InterruptedException, IllegalStateException {
		return extract(source, destination, timeout, false, new HashSet<>());
	}

	public static Path extract(Path source, Path destination, Duration timeout, boolean recursive)
			throws IOException, InterruptedException, IllegalStateException {
		return extract(source, destination, timeout, recursive, new HashSet<>());
	}

	private static Path extract(Path source, Path destination, Duration timeout, boolean recursive, Set<Path> visited)
			throws IOException, InterruptedException, IllegalStateException {

		if (!Files.isDirectory(destination)) Files.createDirectories(destination);

		Path result;

		String ext = Util.extension(source).toLowerCase();
		switch (ext) {
			case "zip":
			case "7z":
			case "lzh":
			case "lza":
			case "exe":
				result = exec(sevenZipCmd(source, destination), source, destination, timeout);
				break;
			case "rar":
				result = exec(rarCmd(source, destination), source, destination, timeout);
				break;
			default:
				throw new UnsupportedOperationException(String.format("Format %s not supported for archive %s", ext, source));
		}

		visited.add(source);

		if (recursive) {
			Set<Path> next = new HashSet<>();
			Files.walkFileTree(result, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (!visited.contains(file) && isArchive(file)) next.add(file);
					return FileVisitResult.CONTINUE;
				}
			});

			for (Path path : next) {
				try {
					if (!visited.contains(path)) extract(path, result, timeout, recursive, visited);
				} catch (Exception e) {
					// be lenient with recursive extraction...
				}
			}
		}

		return result;
	}

	public static void cleanPath(Path path) throws IOException {
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});

		Files.deleteIfExists(path);
	}

	private static String[] sevenZipCmd(Path source, Path destination) {
		return new String[] {
				SEVENZIP_BIN,
				"x",                          // extract
				"-bd",                        // no progress
				"-y",                         // yes to all
				source.toString(),            // file to extract
				"-o" + destination.toString() // destination directory
		};
	}

	private static String[] rarCmd(Path source, Path destination) {
		return new String[] {
				UNRAR_BIN,
				"e",                   // extract
				"-y",                  // yes to all
				source.toString(),     // file to extract
				destination.toString() // destination directory
		};
	}

	private static Path exec(String[] cmd, Path source, Path destination, Duration timeout)
			throws IOException, InterruptedException, IllegalStateException {

		Process process = new ProcessBuilder()
				.command(cmd)
				.directory(destination.toFile())
				.start();
		boolean b = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
		if (!b) {
			process.destroyForcibly().waitFor(KILL_WAIT.toMillis(), TimeUnit.MILLISECONDS);
		}

		if (process.exitValue() != 0) {
			// cleanup
			cleanPath(destination);
			throw new IllegalStateException(String.format("File %s was not unpacked successfully", source));
		}

		return destination;
	}

	public static Path createZip(Path source, Path destination, Duration timeout)
			throws IOException, InterruptedException, IllegalStateException {

		if (!Files.isDirectory(source)) throw new IllegalArgumentException("Source is expected to be a directory");

		Process process = new ProcessBuilder()
				.command(
						ZIP_BIN,
						"-9",                   // compress more
						"-r",                   // recursive
						destination.toString(), // destination zip file
						source.toString()       // source directory
				)
				.directory(source.toFile())
				.start();
		boolean b = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
		if (!b) {
			process.destroyForcibly().waitFor(KILL_WAIT.toMillis(), TimeUnit.MILLISECONDS);
		}

		if (process.exitValue() != 0) {
			throw new IllegalStateException(String.format("File %s was not unpacked successfully", source));
		}

		return destination;
	}
}
