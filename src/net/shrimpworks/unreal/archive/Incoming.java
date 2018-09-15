package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.shrimpworks.unreal.packages.Umod;

public class Incoming {

	public final ContentSubmission submission;
	public final Path contentRoot;
	public final Path repack;
	public final String originalSha1;

	public final List<String> files;

	public Incoming(ContentSubmission submission) throws IOException, UnsupportedOperationException {
		this.submission = submission;
		this.contentRoot = getRoot(submission.filePath);
		this.repack = getRepack(submission.filePath, contentRoot);
		this.originalSha1 = sha1(submission.filePath);

		this.files = listFiles(submission.filePath, contentRoot);
	}

	private Path getRoot(Path incoming) throws IOException, UnsupportedOperationException {
		String fName = incoming.toString().toLowerCase();

		if (fName.endsWith(".zip")) {
			// zip files are fine, we can read them without extracting
			FileSystem fs = FileSystems.newFileSystem(incoming, null);
			return fs.getPath("/");
		} else if (fName.endsWith(".rar") || fName.endsWith(".7z")) {
			// TODO extract with 7z?
			throw new UnsupportedOperationException("RAR and 7z not yet supported");
		} else if (fName.endsWith(".exe")) {
			// TODO exec `file fName`
			// note, installshield can be read by 7z
			// note, plain win32 executables may be converted to zips via `zip -J fName -out fName.zip`
			throw new UnsupportedOperationException("Executable packages not yet supported");
		} else {
			System.out.println(fName);
			throw new UnsupportedOperationException("Unknown file format: " + fName.substring(fName.lastIndexOf(".")));
		}

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

	private String sha1(Path original) throws IOException {
		try (FileChannel channel = FileChannel.open(original, StandardOpenOption.READ)) {
			MessageDigest md = MessageDigest.getInstance("SHA-1");

			ByteBuffer buffer = ByteBuffer.allocate(4096);

			while (channel.read(buffer) > 0) {
				buffer.flip();
				md.update(buffer);
				buffer.clear();
			}

			byte[] digest = md.digest();
			StringBuilder sb = new StringBuilder();
			for (byte b : digest) {
				sb.append(Integer.toHexString((0xFF & b)));
			}

			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private List<String> listFiles(Path filePath, Path contentRoot) throws IOException {
		List<String> files = new ArrayList<>();
		if (contentRoot != null && Files.exists(contentRoot)) {
			Files.walkFileTree(contentRoot, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (file.toString().toLowerCase().endsWith(".umod")) {
						files.addAll(umodFiles(file));
					} else {
						files.add(file.toString());
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}
		return files;
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

	@Override
	public String toString() {
		return String.format("Incoming [submission=%s, contentRoot=%s, repack=%s, originalSha1=%s]",
							 submission, contentRoot, repack, originalSha1);
	}
}