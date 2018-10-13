package net.shrimpworks.unreal.archive.indexer.mappacks;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.shrimpworks.unreal.archive.indexer.Content;

public class MapPack extends Content {

	// Type/Game/A/
	private static final String PATH_STRING = "%s/%s/%s/";

	public List<PackMap> maps = new ArrayList<>();

	@Override

	public Path contentPath(Path root) {
		String namePrefix = subGrouping();
		return root.resolve(String.format(PATH_STRING,
										  "MapPacks",
										  game,
										  namePrefix
		));
	}

	public static class PackMap {

		public String name;
		public String title;
		public String author = "Unknown";

	}
}
