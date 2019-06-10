package net.shrimpworks.unreal.archive.content;

import java.nio.file.Path;

public class UnknownContent extends Content {

	// Game/Type/NAME5/Name-[hash8]
	private static final String PATH_STRING = "%s/%s/%s/%s-[%s]";

	@Override
	public Path contentPath(Path root) {
		String basicName = name.toUpperCase().replaceAll("[^A-Z0-9]", "");
		basicName = basicName.substring(0, Math.min(4, basicName.length() - 1));
		return root.resolve(String.format(PATH_STRING,
										  game,
										  "Unknown",
										  basicName,
										  name,
										  hash.substring(0, 8)
		));
	}
}
