package net.shrimpworks.unreal.archive.indexer.skins;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.shrimpworks.unreal.archive.indexer.Content;

public class Skin extends Content {

	// Game/Type/NAME5/Name-[hash8]
	private static final String PATH_STRING = "%s/%s/%s/%s/%s-[%s]";

	public List<String> skins = new ArrayList<>();
	public String model = "Unknown";
	public boolean teamSkins = false;

	@Override
	public Path contentPath(Path root) {
		String basicName = name.toUpperCase().replaceAll("[^A-Z0-9]", "");
		basicName = basicName.substring(0, Math.min(4, basicName.length() - 1));
		return root.resolve(String.format(PATH_STRING,
										  game,
										  "Skins",
										  basicName,
										  name,
										  hash.substring(0, 8)
		));
	}

}
