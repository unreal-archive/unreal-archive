package net.shrimpworks.unreal.archive;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import net.shrimpworks.unreal.archive.maps.Map;

// there appears to be weird mapping issues when using @JsonTypeInfo with YAML
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true, property = "contentType")
@JsonSubTypes({
		@JsonSubTypes.Type(value = Map.class, name = "MAP"),
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Content content = (Content)o;
		return fileSize == content.fileSize
			   && otherFiles == content.otherFiles
			   && deleted == content.deleted
			   && Objects.equals(contentType, content.contentType)
			   && Objects.equals(firstIndex, content.firstIndex)
			   && Objects.equals(lastIndex, content.lastIndex)
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
}
