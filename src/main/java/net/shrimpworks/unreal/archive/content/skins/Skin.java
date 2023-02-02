package net.shrimpworks.unreal.archive.content.skins;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.Games;

public class Skin extends Content {

	// Game/Type/A/
	private static final String PATH_STRING = "%s/%s/%s/%s/";

	public static final Pattern NAME_MATCH = Pattern.compile(".+?\\..{4}\\d");
	static final Pattern NAME_MATCH_UNREAL = Pattern.compile(".+?\\.(.+?)");
	static final Pattern FACE_MATCH = Pattern.compile(".+?\\..{4}\\d[a-zA-Z0-9]+");
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

	@Override
	public String autoDescription() {
		return String.format("%s, a player skin for %s with %s%s%s",
							 name, Games.byName(game).bigName,
							 skins.isEmpty()
									 ? "no skins"
									 : skins.size() > 1 ? skins.size() + " skins" : skins.size() + " skin",
							 faces.isEmpty()
									 ? ""
									 : String.format(" and %s", faces.size() > 1 ? faces.size() + " faces" : faces.size() + " face"),
							 authorName().equalsIgnoreCase("unknown") ? "" : ", created by " + authorName());
	}

	@Override
	public List<String> autoTags() {
		List<String> tags = new ArrayList<>(super.autoTags());
		tags.add(name.toLowerCase());
		tags.addAll(skins.stream().map(String::toLowerCase).toList());
		tags.addAll(faces.stream().map(String::toLowerCase).toList());
		return tags;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Skin other)) return false;
		if (!super.equals(o)) return false;
		return teamSkins == other.teamSkins &&
			   Objects.equals(skins, other.skins) &&
			   Objects.equals(faces, other.faces) &&
			   Objects.equals(model, other.model);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), skins, faces, model, teamSkins);
	}
}
