package net.shrimpworks.unreal.archive.content.voices;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.shrimpworks.unreal.archive.content.Content;

public class Voice extends Content {

	// Game/Type/A/
	private static final String PATH_STRING = "%s/%s/%s/%s";

	static final Pattern UT_VOICE_MATCH = Pattern.compile("Botpack\\.Voice.+?", Pattern.CASE_INSENSITIVE);
	static final String UT2_VOICE_CLASS = "XGame.xVoicePack";

	public List<String> voices = new ArrayList<>();

	@Override
	public Path contentPath(Path root) {
		String namePrefix = subGrouping();
		return root.resolve(String.format(PATH_STRING,
										  game,
										  "Voices",
										  namePrefix,
										  this.hash.substring(0, 2)
		));
	}
}
