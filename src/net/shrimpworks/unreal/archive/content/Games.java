package net.shrimpworks.unreal.archive.content;

import java.util.Arrays;

public enum Games {

	UNKNOWN("Unknown", "Unknown", "Unknown"),

	UNREAL("Unreal", "Unreal", "Unreal"),
	UNREAL_TOURNAMENT("Unreal Tournament", "UT99", "Unreal Tournament (UT99)"),
	UNREAL_2("Unreal 2", "Unreal 2", "Unreal II"),
	UNREAL_TOURNAMENT_2004("Unreal Tournament 2004", "UT2004", "Unreal Tournament 2004 (UT2004)");

	public final String name;
	public final String shortName;
	public final String bigName;

	Games(String name, String shortName, String bigName) {
		this.name = name;
		this.shortName = shortName;
		this.bigName = bigName;
	}

	public static Games byName(String name) {
		return Arrays.stream(values()).filter(g -> g.name.equalsIgnoreCase(name)).findFirst().orElse(UNKNOWN);
	}
}
