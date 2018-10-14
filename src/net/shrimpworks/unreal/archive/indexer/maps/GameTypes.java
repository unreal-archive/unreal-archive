package net.shrimpworks.unreal.archive.indexer.maps;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class GameTypes {

	private static final List<GameType> GAME_TYPES = Arrays.asList(
			new GameType("Single Player", "SP"),
			new GameType("1 on 1", "DM-1on1"),
			new GameType("DeathMatch", "DM"),
			new GameType("BunnyTrack", "CTF-BT", "BT"),
			new GameType("Multi-Team CTF", "CTF4", "CTFM"),
			new GameType("Capture The Flag", "CTF"),
			new GameType("Domination", "DOM"),
			new GameType("Assault", "AS"),
			new GameType("Bombing Run", "BR"),
			new GameType("Onslaught", "ONS"),
			new GameType("Vehicle CTF", "VCTF"),
			new GameType("Monster Hunt", "MH"),
			new GameType("Monster Arena", "MA"),
			new GameType("Team Monster Hunt", "TMH"),
			new GameType("Rocket Arena", "RA"),
			new GameType("Jailbreak", "JB"),
			new GameType("Tactical Ops", "TO"),
			new GameType("Tactical Ops", "SW"),
			new GameType("Infiltration", "INF"),
			new GameType("UnWheel", "UW"),
			new GameType("Thievery", "Thievery"),
			new GameType("Unreal4Ever", "U4E"),
			new GameType("Unreal Fortress", "UNF"),
			new GameType("XMP", "XMP"),
			new GameType("FragBall", "FB"),
			new GameType("Flag Domination", "FD")
	);

	public static GameType forMap(String mapName) {
		String lower = mapName.toLowerCase();
		for (GameType gt : GAME_TYPES) {
			for (String p : gt.mapPrefixes) {
				if (lower.startsWith(p.toLowerCase())) return gt;
			}
		}
		return null;
	}

	public static GameType byName(String name) {
		String lower = name.toLowerCase();
		for (GameType gt : GAME_TYPES) {
			if (gt.name.toLowerCase().equals(lower)) return gt;
		}
		return null;
	}

	public static class GameType {

		public final String name;
		public final Collection<String> mapPrefixes;

		public GameType(String name, Collection<String> mapPrefixes) {
			this.name = name;
			this.mapPrefixes = mapPrefixes;
		}

		public GameType(String name, String... mapPrefixes) {
			this(name, Arrays.asList(mapPrefixes));
		}
	}
}
