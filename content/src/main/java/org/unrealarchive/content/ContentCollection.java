package org.unrealarchive.content;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.unrealarchive.common.Platform;
import org.unrealarchive.common.Util;

import static org.unrealarchive.content.addons.Addon.RELEASE_DATE_FMT;

public class ContentCollection implements ContentEntity<ContentCollection> {

	public String title;
	public String description;
	public String author;
	public Map<String, String> links = new HashMap<>();
	public String titleImage;

	public LocalDate createdDate;

	public List<CollectionItem> items = new ArrayList<>();

	public boolean published = true;

	public List<CollectionArchive> archives = new ArrayList<>();

	private transient AuthorInfo authorInfo;

	@Override
	public ContentId id() {
		return new ContentId("COLLECTION", Util.slug(title));
	}

	@Override
	public Path contentPath(Path root) {
		return slugPath(root);
	}

	@Override
	public String game() {
		return "Mixed";
	}

	@Override
	public String contentType() {
		return "Collection";
	}

	@Override
	public String friendlyType() {
		return "Collection";
	}

	@Override
	public Map<String, String> problemLinks() {
		return Map.of();
	}

	@Override
	public boolean isVariation() {
		return false;
	}

	@Override
	public Path slugPath(Path root) {
		return root.resolve("collections").resolve(Util.slug(this.title));
	}

	@Override
	public Path pagePath(Path root) {
		return slugPath(root).resolve("index.html");
	}

	@Override
	public String name() {
		return title;
	}

	@Override
	public String author() {
		if (author == null || author.isBlank()) return "Unknown";
		else return author;
	}

	@Override
	public AuthorInfo authorInfo() {
		if (authorInfo == null) authorInfo = new AuthorInfo(author);
		return authorInfo;
	}

	@Override
	public String autoDescription() {
		return description;
	}

	@Override
	public Set<String> autoTags() {
		return Set.of();
	}

	@Override
	public String releaseDate() {
		return RELEASE_DATE_FMT.format(createdDate);
	}

	@Override
	public LocalDateTime addedDate() {
		return createdDate.atStartOfDay();
	}

	@Override
	public Map<String, String> links() {
		return links;
	}

	@Override
	public String leadImage() {
		if (titleImage == null || titleImage.isBlank()) return "";
		return slugPath(Paths.get("")).resolve(titleImage).toString();
	}

	@Override
	public boolean deleted() {
		return !published;
	}

	public int compareTo(ContentCollection o) {
		return createdDate.compareTo(o.createdDate);
	}

	@Override
	public int compareTo(ContentEntity<?> o) {
		return name().compareToIgnoreCase(o.name());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ContentCollection that = (ContentCollection)o;
		return published == that.published
			   && Objects.equals(title, that.title)
			   && Objects.equals(description, that.description)
			   && Objects.equals(author, that.author)
			   && Objects.equals(links, that.links)
			   && Objects.equals(titleImage, that.titleImage)
			   && Objects.equals(createdDate, that.createdDate)
			   && Objects.equals(items, that.items)
			   && Objects.equals(archives, that.archives);
	}

	@Override
	public int hashCode() {
		return Objects.hash(title, description, author, links, titleImage, createdDate, items, published, archives);
	}

	public static class CollectionItem {

		public String title;

		/**
		 * The id identifies how a content item should be resolved. Maps to the result of `ContentEntity.id()`
		 */
		public String id;

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			CollectionItem that = (CollectionItem)o;
			return Objects.equals(title, that.title) && Objects.equals(id, that.id);
		}

		@Override
		public int hashCode() {
			return Objects.hash(title, id);
		}
	}

	public static class CollectionArchive {

		public String title;
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
			if (o == null || getClass() != o.getClass()) return false;
			CollectionArchive that = (CollectionArchive)o;
			return fileSize == that.fileSize
				   && synced == that.synced
				   && deleted == that.deleted
				   && Objects.equals(title, that.title)
				   && Objects.equals(localFile, that.localFile)
				   && platform == that.platform
				   && Objects.equals(downloads, that.downloads)
				   && Objects.equals(originalFilename, that.originalFilename)
				   && Objects.equals(hash, that.hash);
		}

		@Override
		public int hashCode() {
			return Objects.hash(title, localFile, platform, downloads, originalFilename, hash, fileSize, synced, deleted);
		}
	}
}
