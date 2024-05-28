package org.unrealarchive.content.managed;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.unrealarchive.common.Platform;
import org.unrealarchive.common.Util;
import org.unrealarchive.content.AuthorNames;
import org.unrealarchive.content.ContentEntity;
import org.unrealarchive.content.Download;

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
	public String subGroup = "Patches";             // subgrouping
	public String document = "readme.md";           // file name of an associated markdown document
	public String title;                            // Brush Importer for UnrealED
	public String author = "Unknown";               // Joe Soap
	public String homepage = "";                    // https://cool-editor-tool.com/
	public String description = "No description";   // A cool tool to do things in the editor.
	public LocalDate releaseDate;
	public String titleImage;                       // "pic.png"
	public List<String> images = new ArrayList<>(); // [image1.png, screenshot.jpg]
	public List<ManagedFile> downloads = new ArrayList<>();
	public Map<String, String> links = new HashMap<>();
	public Map<String, String> problemLinks = new HashMap<>();

	public boolean published = true;                // false will hide it

	public String fullPath() {
		return String.join("/", game, group, subGroup);
	}

	@Override
	public Path contentPath(Path root) {
		return slugPath(root.resolve("managed"));
	}

	@Override
	public Path slugPath(Path root) {
		String game = Util.slug(this.game);
		String group = Util.slug(this.group);
		String subGroup = Util.slug(this.subGroup);
		String name = Util.slug(this.title);
		return root.resolve(game).resolve(group).resolve(subGroup).resolve(name);
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
	public Map<String, String> problemLinks() {
		return problemLinks;
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
		if (!(o instanceof Managed managed)) return false;
		return published == managed.published
			   && Objects.equals(createdDate, managed.createdDate)
			   && Objects.equals(updatedDate, managed.updatedDate)
			   && Objects.equals(game, managed.game)
			   && Objects.equals(document, managed.document)
			   && Objects.equals(group, managed.group)
			   && Objects.equals(subGroup, managed.subGroup)
			   && Objects.equals(title, managed.title)
			   && Objects.equals(author, managed.author)
			   && Objects.equals(homepage, managed.homepage)
			   && Objects.equals(description, managed.description)
			   && Objects.equals(titleImage, managed.titleImage)
			   && Objects.equals(links, managed.links)
			   && Objects.equals(problemLinks, managed.problemLinks)
			   && Objects.equals(images, managed.images);
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdDate, updatedDate, game, document, group, subGroup, title, author, homepage, description,
							titleImage, images, published, links, problemLinks);
	}

	public static class ManagedFile {

		public String title;
		public String version;
		public String description = "";             // specific description for this file
		public String localFile;                    // local path to the file to be synced
		public Platform platform = Platform.ANY;    // platform-specific files

		public List<Download> downloads = new ArrayList<>(); // list of download mirrors for this file, sync process will add to this

		public String originalFilename;             // dm-mymap.zip
		public String hash;
		public long fileSize = 0;                   // filesize, we'll determine when synced
		public boolean synced = false;              // if false, localFile will be uploaded and turned into a download upon sync
		public boolean deleted = false;             // if deleted, prevents from syncing and will not publish

		public Download directDownload() {
			return downloads.stream().filter(d -> d.direct).findAny().orElse(null);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof ManagedFile that)) return false;
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
