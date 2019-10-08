package net.shrimpworks.unreal.archive.content.skins;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.shrimpworks.unreal.archive.content.Content;

public class Skin extends Content {

	// Game/Type/A/
	private static final String PATH_STRING = "%s/%s/%s/%s/";

	static final Pattern NAME_MATCH = Pattern.compile(".+?\\..+?\\d");
	static final Pattern NAME_MATCH_UNREAL = Pattern.compile(".+?\\.(.+?)");
	static final Pattern FACE_MATCH = Pattern.compile(".+?\\..+?\\d[a-zA-Z0-9]+");
	static final Pattern FACE_PORTRAIT_MATCH = Pattern.compile("(.+?)\\.(.+?5[a-zA-Z0-9]+)"); // (something_lol).(word5name)
	static final Pattern TEAM_MATCH = Pattern.compile(".+?\\..+?\\dT_\\d", Pattern.CASE_INSENSITIVE);

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
										  namePrefix,
										  hashPath()
		));
	}

}
