package net.shrimpworks.unreal.archive;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

// there appears to be weird mapping issues when using @JsonTypeInfo with YAML
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true, property = "contentType")
@JsonSubTypes({ @JsonSubTypes.Type(value = Content.class, name = "UNKNOWN") })
public class Content {

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
	public String sha1;
	public int fileSize;
	public List<ContentFile> files = new ArrayList<>();     // [DM-MyMap.unr, MyTex.utx]
	public int otherFiles = 0;                              // count of non-content files (readme, html, etc)
	public List<Download> downloads = new ArrayList<>();

	public boolean deleted = false;
}
