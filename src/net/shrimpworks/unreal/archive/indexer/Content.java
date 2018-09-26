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

import net.shrimpworks.unreal.archive.indexer.maps.Map;
import net.shrimpworks.unreal.archive.indexer.skins.Skin;

// there appears to be weird mapping issues when using @JsonTypeInfo with YAML
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true, property = "contentType")
@JsonSubTypes({
		@JsonSubTypes.Type(value = Map.class, name = "MAP"),
		@JsonSubTypes.Type(value = Skin.class, name = "SKIN"),
		@JsonSubTypes.Type(value = UnknownContent.class, name = "UNKNOWN")
})
public abstract class Content {

	public static final DateTimeFormatter RELEASE_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZoneId.systemDefault());

	public String contentType;

	public LocalDateTime firstIndex;
	public LocalDateTime lastIndex;

	public String game = "Unknown";                         // Unreal Tournament
	public String name;                                     // DM-MyMap
	public String author = "Unknown";                       // Joe Soap
	public String description = "None";                     // My cool map is cool and stuff

	public String releaseDate = "Unknown";                  // 2001-05
	public List<String> screenshots = new ArrayList<>();    // [Screenshot.png, Screenshot2.jpg]

	public String originalFilename;                         // dm-mymap.zip
	public String hash;
	public int fileSize;
	public List<ContentFile> files = new ArrayList<>();     // [DM-MyMap.unr, MyTex.utx]
	public int otherFiles = 0;                              // count of non-content files (readme, html, etc)
	public List<Download> downloads = new ArrayList<>();

	public boolean deleted = false;

	public abstract Path contentPath(Path root);

	public boolean hasDownload(String url) {
		for (Download download : downloads) {
			if (download.url.equals(url)) return true;
		}
		return false;
	}

	public boolean containsFile(String hash) {
		return files.stream().anyMatch(f -> f.hash.equals(hash));
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
			   && Objects.equals(game, content.game)
			   && Objects.equals(name, content.name)
			   && Objects.equals(author, content.author)
			   && Objects.equals(description, content.description)
			   && Objects.equals(releaseDate, content.releaseDate)
			   && Objects.equals(screenshots, content.screenshots)
			   && Objects.equals(originalFilename, content.originalFilename)
			   && Objects.equals(hash, content.hash)
			   && Objects.equals(files, content.files)
			   && Objects.equals(downloads, content.downloads);
	}

	@Override
	public int hashCode() {
		return Objects.hash(contentType, firstIndex, lastIndex, game, name, author, description, releaseDate, screenshots, originalFilename,
							hash, fileSize, files, otherFiles, downloads, deleted);
	}

	public static class ContentFile {

		public String name;
		public int fileSize;
		public String hash;

		@ConstructorProperties({ "name", "fileSize", "hash" })
		public ContentFile(String name, int fileSize, String hash) {
			this.name = name;
			this.fileSize = fileSize;
			this.hash = hash;
		}
	}

	public static class Download {

		public final String url;
		public final LocalDate added;
		public LocalDate lastChecked;
		public boolean ok;                // health at last check date
		public final boolean repack;

		public boolean deleted;

		@ConstructorProperties({ "url", "added", "lastChecked", "ok", "repack", "deleted" })
		public Download(String url, LocalDate added, LocalDate lastChecked, boolean ok, boolean repack, boolean deleted) {
			this.url = url;
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
		}
	}
}