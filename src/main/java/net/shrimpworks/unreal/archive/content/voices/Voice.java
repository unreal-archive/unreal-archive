package net.shrimpworks.unreal.archive.content.voices;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.Games;

public class Voice extends Content {

	// Game/Type/A/
	private static final String PATH_STRING = "%s/%s/%s/%s/";

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
										  hashPath()
		));
	}

	@Override
	public String autoDescription() {
		return String.format("%s, a custom voice pack for %s with %s%s",
							 name, Games.byName(game).bigName,
							 voices.isEmpty()
									 ? "no voices"
									 : voices.size() > 1 ? voices.size() + " voices" : voices.size() + " voice",
							 author.equalsIgnoreCase("unknown") ? "" : ", created by " + author);
	}

	@Override
	public List<String> autoTags() {
		List<String> tags = new ArrayList<>(super.autoTags());
		tags.add(name.toLowerCase());
		tags.addAll(voices.stream().map(String::toLowerCase).collect(Collectors.toList()));
		return tags;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Voice)) return false;
		if (!super.equals(o)) return false;
		Voice voice = (Voice)o;
		return Objects.equals(voices, voice.voices);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), voices);
	}
}
