package net.shrimpworks.unreal.archive;

import java.time.LocalDateTime;
import java.util.List;

public class Content {

	public LocalDateTime firstIndex;
	public LocalDateTime lastIndex;

	public String game;                 // Unreal Tournament
	public String name;                 // DM-MyMap
	public String author;               // Joe Soap

	public String releaseDate;          // 2001-05
	public List<String> screenshots;    // [Screenshot.png, Screenshot2.jpg]
	public String packageSHA1;
	public int fileSize;
	public List<ContentFile> files;     // [DM-MyMap.unr, MyTex.utx]
	public List<Download> downloads;

	public boolean deleted;

}
