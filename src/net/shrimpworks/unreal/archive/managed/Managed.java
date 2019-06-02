package net.shrimpworks.unreal.archive.managed;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.Util;

/**
 * Managed files represent content we cannot automatically index and
 * categorise.
 * <p>
 * Additionally we're able to provide attached documentation, and multi-
 * platform variations of managed file downloads.
 * <p>
 * Since manged files are little more static than other user content
 * files, management and hosting of the files is a little more tricky.
 * Thus, an additional "sync" step is required, which will upload local
 * copies of files to remote storage.
 * <p>
 * During the sync process, unsynced local files will be published to
 * remote storage, and may then be discarded.
 */
public class Managed {

	public enum Platform {
		ANY,
		WINDOWS,
		LINUX,
		MACOS
	}

	public LocalDate createdDate;
	public LocalDate updatedDate;

	public String game = "Unreal Tournament";
	public String document = "readme.md";           // file name of an associated markdown document
	public String path = "";                        // defines a path-like structure for navigation; "Tools/Editor"
	public String title;                            // Brush Importer for UnrealED
	public String author = "Unknown";               // Joe Soap
	public String homepage = "";                    // https://cool-editor-tool.com/
	public String description = "No description";   // A cool tool to do things in the editor.
	public LocalDate releaseDate;
	public String titleImage;                       // "pic.png"
	public List<String> images = new ArrayList<>(); // [image1.png, screenshot.jpg]
	public List<ManagedFile> downloads = new ArrayList<>();

	public boolean published = true;                // false will hide it

	public Path slugPath(Path root) {
		String game = Util.slug(this.game);
		String path = Arrays.stream(this.path.split("/")).map(Util::slug).collect(Collectors.joining("/"));
		String name = Util.slug(this.title);
		return root.resolve(game).resolve(path).resolve(name);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Managed)) return false;
		Managed managed = (Managed)o;
		return published == managed.published
			   && Objects.equals(createdDate, managed.createdDate)
			   && Objects.equals(updatedDate, managed.updatedDate)
			   && Objects.equals(game, managed.game)
			   && Objects.equals(document, managed.document)
			   && Objects.equals(path, managed.path)
			   && Objects.equals(title, managed.title)
			   && Objects.equals(author, managed.author)
			   && Objects.equals(homepage, managed.homepage)
			   && Objects.equals(description, managed.description)
			   && Objects.equals(titleImage, managed.titleImage)
			   && Objects.equals(images, managed.images)
			   && Objects.equals(downloads, managed.downloads);
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdDate, updatedDate, game, document, path, title, author, homepage, description, titleImage, images,
							downloads, published);
	}

	public static class ManagedFile {

		public String title;
		public String version;
		public String description = "";             // specific description for this file
		public String localFile;                    // local path to the file to be synced
		public List<String> downloads = new ArrayList<>(); // list of download mirrors for this file, sync process will add to this
		public long fileSize = 0;                    // filesize, we'll determine when synced
		public Platform platform = Platform.ANY;    // platform-specific files
		public boolean synced = false;              // if false, localFile will be uploaded and turned into a download upon sync
		public boolean deleted = false;             // if deleted, prevents from syncing and will not publish

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof ManagedFile)) return false;
			ManagedFile that = (ManagedFile)o;
			return fileSize == that.fileSize
				   && synced == that.synced
				   && deleted == that.deleted
				   && Objects.equals(title, that.title)
				   && Objects.equals(version, that.version)
				   && Objects.equals(description, that.description)
				   && Objects.equals(localFile, that.localFile)
				   && Objects.equals(downloads, that.downloads)
				   && platform == that.platform;
		}

		@Override
		public int hashCode() {
			return Objects.hash(title, version, description, localFile, downloads, fileSize, platform, synced, deleted);
		}
	}

}
