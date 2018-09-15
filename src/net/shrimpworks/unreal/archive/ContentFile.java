package net.shrimpworks.unreal.archive;

import java.beans.ConstructorProperties;

public class ContentFile {

	public String name;
	public int fileSize;
	public String sha1;

	@ConstructorProperties({"name", "fileSize", "sha1"})
	public ContentFile(String name, int fileSize, String sha1) {
		this.name = name;
		this.fileSize = fileSize;
		this.sha1 = sha1;
	}
}
