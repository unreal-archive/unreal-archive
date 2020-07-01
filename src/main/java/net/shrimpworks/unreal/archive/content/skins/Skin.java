package net.shrimpworks.unreal.archive.content.skins;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.Games;

public class Skin extends Content {

	// Game/Type/A/
	private static final String PATH_STRING = "%s/%s/%s/%s/";

	static final Pattern NAME_MATCH = Pattern.compile(".+?\\..{4}\\d");
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
		return String.format("%s, a custom player skin for %s with %s and %s, created by %s",
							 name, Games.byName(game).bigName,
							 skins.isEmpty()
									 ? "no skins"
									 : skins.size() > 1 ? skins.size() + " skins" : skins.size() + " skin",
							 faces.isEmpty()
									 ? "no faces"
									 : faces.size() > 1 ? faces.size() + " faces" : faces.size() + " face",
							 author);
	}

	@Override
	public List<String> autoTags() {
		List<String> tags = new ArrayList<>(super.autoTags());
		tags.add(name.toLowerCase());
		tags.addAll(skins.stream().map(String::toLowerCase).collect(Collectors.toList()));
		tags.addAll(faces.stream().map(String::toLowerCase).collect(Collectors.toList()));
		return tags;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Skin)) return false;
		if (!super.equals(o)) return false;
		Skin skin = (Skin)o;
		return teamSkins == skin.teamSkins &&
			   Objects.equals(skins, skin.skins) &&
			   Objects.equals(faces, skin.faces) &&
			   Objects.equals(model, skin.model);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), skins, faces, model, teamSkins);
	}
}
