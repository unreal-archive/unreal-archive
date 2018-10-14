package net.shrimpworks.unreal.archive.indexer.skins;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.shrimpworks.unreal.archive.indexer.Content;

public class Skin extends Content {

	// Game/Type/A/
	private static final String PATH_STRING = "%s/%s/%s/";

	static final Pattern NAME_MATCH = Pattern.compile(".+?\\..+?\\d");
	static final Pattern FACE_MATCH = Pattern.compile(".+?\\..+?\\d[a-zA-Z0-9]+");
	static final Pattern TEAM_MATCH = Pattern.compile(".+?\\..+?\\dT_\\d", Pattern.CASE_INSENSITIVE);

	static final Pattern AUTHOR_MATCH = Pattern.compile("(.+)?(author|by)([\\s:]+)?([A-Za-z0-9 _]{2,25})(\\s+)?", Pattern.CASE_INSENSITIVE);

	public List<String> skins = new ArrayList<>();
	public List<String> faces = new ArrayList<>();
	public String model = "Unknown";
	public boolean teamSkins = false;

	@Override
	public Path contentPath(Path root) {
		String namePrefix = subGrouping();
		return root.resolve(String.format(PATH_STRING,
										  game,
										  "Skins",
										  namePrefix
		));
	}

}
