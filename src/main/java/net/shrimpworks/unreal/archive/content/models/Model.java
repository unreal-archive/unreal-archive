package net.shrimpworks.unreal.archive.content.models;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.Games;

public class Model extends Content {

	// Game/Type/A/
	private static final String PATH_STRING = "%s/%s/%s/%s/";

	static final String UT_PLAYER_CLASS = "Botpack.TournamentPlayer";
	static final String RUNE_PLAYER_CLASS = "RuneI.RunePlayer";
	static final String UT3_CHARACTER_DEF = "UTGame.UTCustomChar_Data";

	static final Pattern NAME_MATCH = Pattern.compile(".+?\\..+?");

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

	@Override
	public String autoDescription() {
		return String.format("%s, a player model for %s with %s%s%s",
							 name, Games.byName(game).bigName,
							 models.isEmpty()
									 ? "no characters"
									 : models.size() > 1 ? models.size() + " characters" : models.size() + " character",
							 skins.isEmpty()
									 ? ""
									 : String.format(" and %s", skins.size() > 1 ? skins.size() + " skins" : skins.size() + " skin"),
							 authorName().equalsIgnoreCase("unknown") ? "" : ", created by " + authorName());
	}

	@Override
	public List<String> autoTags() {
		List<String> tags = new ArrayList<>(super.autoTags());
		tags.add(name.toLowerCase());
		tags.addAll(models.stream().map(String::toLowerCase).toList());
		tags.addAll(skins.stream().map(String::toLowerCase).toList());
		return tags;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Model other)) return false;
		if (!super.equals(o)) return false;
		return Objects.equals(models, other.models) &&
			   Objects.equals(skins, other.skins);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), models, skins);
	}
}
