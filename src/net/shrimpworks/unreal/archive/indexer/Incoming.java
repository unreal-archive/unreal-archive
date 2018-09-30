package net.shrimpworks.unreal.archive.indexer;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.ArchiveUtil;
import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.packages.Umod;

public class Incoming implements Closeable {

	public enum FileType {
		CODE(true, "u"),
		MAP(true, "unr", "ut2", "ut3"),
		TEXTURE(true, "utx"),
		MUSIC(true, "umx", "ogg"),
		SOUNDS(true, "uax"),
		ANIMATION(true, "ukx"),
		STATICMESH(true, "usx"),
		PREFAB(true, "upx"),
		PHYSICS(true, "ka"),
		PLAYER(true, "upl"),
		INT(false, "int"),
		INI(false, "ini"),
		UMOD(true, "umod"),
		TEXT(false, "txt"),
		HTML(false, "html", "htm"),
		IMAGE(false, "jpg", "jpeg", "bmp", "png", "gif"),
		;

		public static final FileType[] IMPORTANT = Arrays.stream(values())
														 .filter(t -> t.important)
														 .collect(Collectors.toSet())
														 .toArray(new FileType[0]);

		public static final FileType[] ALL = FileType.values();

		public final boolean important;
		public final Collection<String> ext;

		FileType(boolean important, String... ext) {
			this.important = important;
			this.ext = Collections.unmodifiableCollection(Arrays.asList(ext));
		}

		public boolean matches(String path) {
			return (ext.contains(Util.extension(path).toLowerCase()));
		}

		public static boolean important(Path path) {
			return important(path.toString());
		}

		public static boolean important(String path) {
			for (FileType type : values()) {
				if (type.important && type.ext.contains(Util.extension(path).toLowerCase())) return true;
			}
			return false;
		}

	}

	public final Submission submission;
	public final String hash;
	public final int fileSize;
	public final IndexLog log;

	private final Set<Umod> umods;

	public Path contentRoot;
	public Map<String, Object> files;

	private Path repackPath;

	public Incoming(Submission submission, IndexLog log) throws IOException, UnsupportedOperationException {
		this.submission = submission;
		this.hash = Util.hash(submission.filePath);
		this.fileSize = (int)Files.size(submission.filePath);
		this.umods = new HashSet<>();
		this.log = log;
	}

	public Incoming prepare() throws IOException {
		this.contentRoot = getRoot(submission.filePath);
		this.files = listFiles(submission.filePath, contentRoot);
		return this;
	}

	@Override
	public void close() throws IOException {
		for (Umod v : umods) {
			try {
				v.close();
			} catch (IOException e) {
				log.log(IndexLog.EntryType.INFO, "Failed cleaning up Umod file " + v, e);
			}
		}

		umods.clear();

		if (files != null) files.clear();

		// clean up contentRoot
		if (contentRoot != null) {
			ArchiveUtil.cleanPath(contentRoot);
		}

		if (repackPath != null) {
			ArchiveUtil.cleanPath(repackPath);
		}
	}

	public Path getRepack(String repackName) throws IOException, InterruptedException {
		if (Util.extension(submission.filePath).equalsIgnoreCase("zip")) return null;

		repackPath = Files.createTempDirectory("archive-repack-");

		Path dest = repackPath.resolve(repackName + ".zip");

		if (contentRoot != null) {
			return ArchiveUtil.createZip(contentRoot, dest, Duration.ofSeconds(60));
		}

		return null;
	}

	public Set<IncomingFile> files(FileType... type) {
		Set<IncomingFile> res = new HashSet<>();
		for (FileType t : type) {
			res.addAll(files.keySet().stream()
							.filter(t::matches)
							.map(IncomingFile::new)
							.collect(Collectors.toSet()));
		}
		return Collections.unmodifiableSet(res);
	}

	private Map<String, Object> listFiles(Path filePath, Path contentRoot) throws IOException {
		Map<String, Object> files = new HashMap<>();
		if (contentRoot != null && Files.exists(contentRoot)) {
			Files.walkFileTree(contentRoot, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (file.toString().toLowerCase().endsWith(".umod")) {
						files.putAll(umodFiles(file));
					}
					files.put(file.toString(), file);
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
			if (file.name.equalsIgnoreCase("System\\Manifest.int") || file.name.equalsIgnoreCase("System\\Manifest.ini")) continue;
			fileList.put(file.name.replaceAll("\\\\", "/"), file);
		}

		return fileList;
	}

	private Path getRoot(Path incoming) throws IOException, UnsupportedOperationException {
		Path tempDir = Files.createTempDirectory("archive-incoming-");

		if (ArchiveUtil.isArchive(incoming)) {
			// its an archive of files of some sort, unpack it to the root
			try {
				return ArchiveUtil.extract(incoming, tempDir, Duration.ofSeconds(30), true);
			} catch (InterruptedException e) {
				throw new IOException("Extract took too long", e);
			}
		} else {
			// its simply an loose file of a type we're interested in
			if (FileType.important(incoming)) {
				return Files.copy(incoming, tempDir, StandardCopyOption.REPLACE_EXISTING);
			}
		}

		throw new UnsupportedOperationException("Can't unpack file " + incoming);
	}

	@Override
	public String toString() {
		return String.format("Incoming [submission=%s, contentRoot=%s, hash=%s]",
							 submission, contentRoot, hash);
	}

	public class IncomingFile {

		public final String file;

		private IncomingFile(String file) {
			this.file = file;
		}

		public SeekableByteChannel asChannel() {
			try {
				if (files.get(file) instanceof Path) {
					return FileChannel.open((Path)files.get(file));
				} else if (files.get(file) instanceof Umod.UmodFile) {
					return ((Umod.UmodFile)files.get(file)).read();
				}
			} catch (IOException e) {
				throw new IllegalStateException("Failed to open file for reading " + file, e);
			}

			return null;
		}

		public String fileName() {
			return Util.fileName(file);
		}

		public int fileSize() {
			try {
				if (files.get(file) instanceof Path) {
					return (int)Files.size((Path)files.get(file));
				} else if (files.get(file) instanceof Umod.UmodFile) {
					return ((Umod.UmodFile)files.get(file)).size;
				}
			} catch (IOException e) {
				throw new IllegalStateException("Failed to get file size for " + file, e);
			}
			return 0;
		}

		public LocalDateTime fileDate() {
			try {
				if (files.get(file) instanceof Path) {
					return Files.getLastModifiedTime((Path)files.get(file)).toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime(); // hmm?
				} else if (files.get(file) instanceof Umod.UmodFile) {
					Set<IncomingFile> umodMaybe = files(FileType.UMOD);
					if (!umodMaybe.isEmpty()) return umodMaybe.iterator().next().fileDate();
				}
			} catch (IOException e) {
				throw new IllegalStateException("Failed to get file size for " + file, e);
			}
			return LocalDateTime.now();
		}

		public String hash() {
			try {
				if (files.get(file) instanceof Path) {
					return Util.hash((Path)files.get(file));
				} else if (files.get(file) instanceof Umod.UmodFile) {
					return ((Umod.UmodFile)files.get(file)).sha1();
				}
			} catch (IOException e) {
				throw new IllegalStateException("Failed to get hash for " + file, e);
			}
			return null;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			IncomingFile that = (IncomingFile)o;
			return Objects.equals(file, that.file);
		}

		@Override
		public int hashCode() {
			return Objects.hash(file);
		}

		@Override
		public String toString() {
			return String.format("IncomingFile [file=%s]", file);
		}
	}
}
