package net.shrimpworks.unreal.archive.content.models;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.shrimpworks.unreal.archive.content.Content;

public class Model extends Content {

	// Game/Type/A/
	private static final String PATH_STRING = "%s/%s/%s/%s";

	static final String UT_PLAYER_CLASS = "Botpack.TournamentPlayer";

	static final Pattern NAME_MATCH = Pattern.compile(".+?\\..+?\\d");

	public List<String> models = new ArrayList<>();
	public List<String> skins = new ArrayList<>();

	@Override
	public Path contentPath(Path root) {
		String namePrefix = subGrouping();
		return root.resolve(String.format(PATH_STRING,
										  game,
										  "Models",
										  namePrefix,
										  hashPath()
		));
	}
}
