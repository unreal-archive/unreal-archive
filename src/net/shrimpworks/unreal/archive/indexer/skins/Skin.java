package net.shrimpworks.unreal.archive.indexer.skins;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.shrimpworks.unreal.archive.indexer.Content;

public class Skin extends Content {

	// Game/Type/NAME5/Name-[hash8]
	private static final String PATH_STRING = "%s/%s/%s/%s_[%s]";

	static final Pattern NAME_MATCH = Pattern.compile(".+?\\..+?\\d");
	static final Pattern FACE_MATCH = Pattern.compile(".+?\\..+?\\d[a-zA-Z0-9]+");
	static final Pattern TEAM_MATCH = Pattern.compile(".+?\\..+?\\dT_\\d", Pattern.CASE_INSENSITIVE);

	static final Pattern AUTHOR_MATCH = Pattern.compile("(.+)?(author|by)([\\s:]+)?([A-Za-z0-9 _]+)(.+)?", Pattern.CASE_INSENSITIVE);

	static final String TEXTURE = "utx";
	static final String INT = "int";

	public List<String> skins = new ArrayList<>();
	public List<String> faces = new ArrayList<>();
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
