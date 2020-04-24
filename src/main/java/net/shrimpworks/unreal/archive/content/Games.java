package net.shrimpworks.unreal.archive.content;

import java.util.Arrays;
import java.util.List;

public enum Games {

	UNKNOWN("Unknown", "Unknown", "Unknown", List.of()),

	UNREAL("Unreal", "Unreal", "Unreal", List.of("unreal", "u1", "gold")),
	UNREAL_TOURNAMENT("Unreal Tournament", "UT99", "Unreal Tournament (UT99)", List.of("ut", "ut99", "unreal tournament", "goty")),
	UNREAL_2("Unreal 2", "Unreal 2", "Unreal II", List.of("unreal 2", "u2", "uii")),
	UNREAL_TOURNAMENT_2004("Unreal Tournament 2004", "UT2004", "Unreal Tournament 2004 (UT2004)",
						   List.of("ut2004", "ut2k4", "ut2003", "ut2k3", "ece"));

	public final String name;
	public final String shortName;
	public final String bigName;
	public final List<String> tags;

	Games(String name, String shortName, String bigName, List<String> tags) {
		this.name = name;
		this.shortName = shortName;
		this.bigName = bigName;
		this.tags = tags;
	}

	public static Games byName(String name) {
		return Arrays.stream(values()).filter(g -> g.name.equalsIgnoreCase(name)).findFirst().orElse(UNKNOWN);
	}
}
