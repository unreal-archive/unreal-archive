package net.shrimpworks.unreal.archive.indexer;

import java.beans.ConstructorProperties;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import net.shrimpworks.unreal.archive.indexer.mappacks.MapPack;
import net.shrimpworks.unreal.archive.indexer.maps.Map;
import net.shrimpworks.unreal.archive.indexer.models.Model;
import net.shrimpworks.unreal.archive.indexer.skins.Skin;

// there appears to be weird mapping issues when using @JsonTypeInfo with YAML
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true, property = "contentType")
@JsonSubTypes({
		@JsonSubTypes.Type(value = Map.class, name = "MAP"),
		@JsonSubTypes.Type(value = MapPack.class, name = "MAP_PACK"),
		@JsonSubTypes.Type(value = Skin.class, name = "SKIN"),
		@JsonSubTypes.Type(value = Model.class, name = "MODEL"),
		@JsonSubTypes.Type(value = UnknownContent.class, name = "UNKNOWN")
})
public abstract class Content implements Comparable<Content> {

	public static final DateTimeFormatter RELEASE_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZoneId.systemDefault());

	public String contentType;

	public LocalDateTime firstIndex;
	public LocalDateTime lastIndex;

	/**
	 * If set to a valid content hash, this content will not be displayed or listed
	 * on its own in www output, but will only appear as an alternative variation
	 * on the indicated content's information page.
	 * <p>
	 * For example, if this content is an earlier version of a mod, we could, for
	 * historical purposes, indicate that this is a variation of the latest version
	 * of that mod.
	 * <p>
	 * Additionally, it can be used to group a file released with zip, umod and exe
	 * versions as the same piece of content.
	 */
	public String variationOf = null;

	public String game = "Unknown";                         // Unreal Tournament
	public String name;                                     // DM-MyMap
	public String author = "Unknown";                       // Joe Soap
	public String description = "None";                     // My cool map is cool and stuff

	public String releaseDate = "Unknown";                  // 2001-05

	public List<Attachment> attachments = new ArrayList<>();// screenshots, videos, documents, etc

	public String originalFilename;                         // dm-mymap.zip
	public String hash;
	public int fileSize;
	public List<ContentFile> files = new ArrayList<>();     // [DM-MyMap.unr, MyTex.utx]
	public int otherFiles = 0;                              // count of non-content files (readme, html, etc)

	public List<Download> downloads = new ArrayList<>();

	/**
	 * If true, will not show up in www output, and will be ignored in index passes.
	 */
	public boolean deleted = false;

	/**
	 * Generate a path to where this content should live within a directory tree.
	 *
	 * @param root root directory
	 * @return content directory
	 */
	public abstract Path contentPath(Path root);

	/**
	 * Grouping for pagination and directory partitioning.
	 * <p>
	 * Generally the first letter of the content's name.
	 *
	 * @return grouping character(s)
	 */
	public String subGrouping() {
		char first = name.toUpperCase().replaceAll("[^A-Z0-9]", "").charAt(0);
		if (Character.isDigit(first)) first = '0';
		return Character.toString(first);
	}

	/**
	 * Check whether or not the given URL is already known as a download
	 * location for this content.
	 *
	 * @param url url to check
	 * @return true if the provided url is already known
	 */
	public boolean hasDownload(String url) {
		for (Download download : downloads) {
			if (download.url.equals(url)) return true;
		}
		return false;
	}

	@Override
	public int compareTo(Content o) {
		return name.compareToIgnoreCase(o.name);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Content content = (Content)o;
		return fileSize == content.fileSize
			   && otherFiles == content.otherFiles
			   && deleted == content.deleted
			   && Objects.equals(contentType, content.contentType)
			   && Objects.equals(variationOf, content.variationOf)
			   && Objects.equals(game, content.game)
			   && Objects.equals(name, content.name)
			   && Objects.equals(author, content.author)
			   && Objects.equals(description, content.description)
			   && Objects.equals(releaseDate, content.releaseDate)
			   && Objects.equals(attachments, content.attachments)
			   && Objects.equals(originalFilename, content.originalFilename)
			   && Objects.equals(hash, content.hash)
			   && Objects.equals(files, content.files)
			   && Objects.equals(downloads, content.downloads);
	}

	@Override
	public int hashCode() {
		return Objects.hash(contentType, firstIndex, lastIndex, variationOf, game, name, author, description, releaseDate, attachments,
							originalFilename, hash, fileSize, files, otherFiles, downloads, deleted);
	}

	public static class ContentFile implements Comparable<ContentFile> {

		public String name;
		public int fileSize;
		public String hash;

		@ConstructorProperties({ "name", "fileSize", "hash" })
		public ContentFile(String name, int fileSize, String hash) {
			this.name = name;
			this.fileSize = fileSize;
			this.hash = hash;
		}

		@Override
		public int compareTo(ContentFile o) {
			return name.compareToIgnoreCase(o.name);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ContentFile that = (ContentFile)o;
			return Objects.equals(hash, that.hash);
		}

		@Override
		public int hashCode() {
			return Objects.hash(hash);
		}
	}

	public static enum AttachmentType {
		IMAGE,
		VIDEO,
		MARKDOWN,
		OTHER
	}

	public static class Attachment {

		public final AttachmentType type;
		public final String name;
		public final String url;

		@ConstructorProperties({ "type", "name", "url" })
		public Attachment(AttachmentType type, String name, String url) {
			this.type = type;
			this.name = name;
			this.url = url;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Attachment that = (Attachment)o;
			return type == that.type
				   && Objects.equals(name, that.name)
				   && Objects.equals(url, that.url);
		}

		@Override
		public int hashCode() {
			return Objects.hash(type, name, url);
		}
	}

	public static class Download implements Comparable<Download> {

		public final String url;
		public final boolean main;
		public final LocalDate added;
		public LocalDate lastChecked;
		public boolean ok;                // health at last check date
		public final boolean repack;

		public boolean deleted;

		@ConstructorProperties({ "url", "main", "added", "lastChecked", "ok", "repack", "deleted" })
		public Download(String url, boolean main, LocalDate added, LocalDate lastChecked, boolean ok, boolean repack, boolean deleted) {
			this.url = url;
			this.main = main;
			this.added = added;
			this.lastChecked = lastChecked;
			this.ok = ok;
			this.repack = repack;
			this.deleted = deleted;
		}

		public Download(String url, LocalDate added, boolean repack) {
			this.url = url;
			this.added = added;
			this.repack = repack;
			this.main = false;
		}

		@Override
		public int compareTo(Download o) {
			return main ? -1 : 0;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Download download = (Download)o;
			return ok == download.ok
				   && main == download.main
				   && repack == download.repack
				   && deleted == download.deleted
				   && Objects.equals(url, download.url);
		}

		@Override
		public int hashCode() {
			return Objects.hash(url, main, ok, repack, deleted);
		}
	}
}
