package net.shrimpworks.unreal.archive.indexer;

import java.beans.ConstructorProperties;

public class ContentFile {

	public String name;
	public int fileSize;
	public String hash;

	@ConstructorProperties({"name", "fileSize", "hash"})
	public ContentFile(String name, int fileSize, String hash) {
		this.name = name;
		this.fileSize = fileSize;
		this.hash = hash;
	}
}
