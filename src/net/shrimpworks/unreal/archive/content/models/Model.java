package net.shrimpworks.unreal.archive.content.models;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.shrimpworks.unreal.archive.content.Content;

public class Model extends Content {

	// Game/Type/A/
	private static final String PATH_STRING = "%s/%s/%s/";

	static final String UT_PLAYER_CLASS = "Botpack.TournamentPlayer";

	static final Pattern NAME_MATCH = Pattern.compile(".+?\\..+?\\d");
	static final Pattern FACE_MATCH = Pattern.compile(".+?\\..+?\\d[a-zA-Z0-9]+");

	static final Pattern AUTHOR_MATCH = Pattern.compile("(.+)?(author|by)([\\s:]+)?([A-Za-z0-9 _]{2,25})(\\s+)?", Pattern.CASE_INSENSITIVE);

	public List<String> models = new ArrayList<>();
	public List<String> skins = new ArrayList<>();

	@Override
	public Path contentPath(Path root) {
		String namePrefix = subGrouping();
		return root.resolve(String.format(PATH_STRING,
										  game,
										  "Models",
										  namePrefix
		));
	}
}
