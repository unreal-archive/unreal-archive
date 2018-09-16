package net.shrimpworks.unreal.archive;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Content {

	public static final DateTimeFormatter RELEASE_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZoneId.systemDefault());

	public LocalDateTime firstIndex;
	public LocalDateTime lastIndex;

	public String game;                 // Unreal Tournament
	public String name;                 // DM-MyMap
	public String author;               // Joe Soap
	public String description;          // My cool map is cool and stuff

	public String releaseDate;          // 2001-05
	public List<String> screenshots;    // [Screenshot.png, Screenshot2.jpg]
	public String sha1;
	public int fileSize;
	public List<ContentFile> files;     // [DM-MyMap.unr, MyTex.utx]
	public int otherFiles;				// count of non-content files (readme, html, etc)
	public List<Download> downloads;

	public boolean deleted;

}
