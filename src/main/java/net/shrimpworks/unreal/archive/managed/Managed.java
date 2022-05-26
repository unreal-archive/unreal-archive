package net.shrimpworks.unreal.archive.managed;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

import net.shrimpworks.unreal.archive.AuthorNames;
import net.shrimpworks.unreal.archive.ContentEntity;
import net.shrimpworks.unreal.archive.Platform;
import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.content.Content;

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
public class Managed implements ContentEntity<Managed> {

	private static final DateTimeFormatter RELEASE_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

	public LocalDate createdDate;
	public LocalDate updatedDate;

	public String game = "Unreal Tournament";
	public String group = "Patches & Updates";      // root level grouping
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
	public java.util.Map<String, String> links = new HashMap<>();

	public boolean published = true;                // false will hide it

	public String fullPath() {
		return String.join("/", group, game, path);
	}

	@Override
	public Path contentPath(Path root) {
		return slugPath(root);
	}

	@Override
	public Path slugPath(Path root) {
		String group = Util.slug(this.group);
		String game = Util.slug(this.game);
		String path = Arrays.stream(this.path.split("/")).map(Util::slug).collect(Collectors.joining("/"));
		String name = Util.slug(this.title);
		return root.resolve(group).resolve(game).resolve(path).resolve(name);
	}

	@Override
	public Path pagePath(Path root) {
		return slugPath(root).resolve("index.html");
	}

	@Override
	public String game() {
		return game;
	}

	@Override
	public String name() {
		return title;
	}

	@Override
	public String author() {
		return author;
	}

	@Override
	public String authorName() {
		return AuthorNames.nameFor(author);
	}

	@Override
	public String releaseDate() {
		return releaseDate != null ? releaseDate.format(RELEASE_DATE_FMT) : createdDate.format(RELEASE_DATE_FMT);
	}

	@Override
	public String autoDescription() {
		return description;
	}

	@Override
	public LocalDateTime addedDate() {
		return createdDate.atStartOfDay();
	}

	@Override
	public String contentType() {
		return Util.slug(this.group);
	}

	@Override
	public String friendlyType() {
		return group;
	}

	@Override
	public String leadImage() {
		return titleImage;
	}

	@Override
	public Map<String, String> links() {
		return links;
	}

	@Override
	public boolean deleted() {
		return !published;
	}

	@JsonIgnore
	@Override
	public boolean isVariation() {
		return false;
	}

	@Override
	public int compareTo(ContentEntity<?> o) {
		return releaseDate().compareToIgnoreCase(o.releaseDate());
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
			   && Objects.equals(links, managed.links)
			   && Objects.equals(images, managed.images);
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdDate, updatedDate, game, document, path, title, author, homepage, description, titleImage, images,
							published, links);
	}

	public static class ManagedFile {

		public String title;
		public String version;
		public String description = "";             // specific description for this file
		public String localFile;                    // local path to the file to be synced
		public Platform platform = Platform.ANY;    // platform-specific files

		public List<Content.Download> downloads = new ArrayList<>(); // list of download mirrors for this file, sync process will add to this

		public String originalFilename;             // dm-mymap.zip
		public String hash;
		public long fileSize = 0;                   // filesize, we'll determine when synced
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
