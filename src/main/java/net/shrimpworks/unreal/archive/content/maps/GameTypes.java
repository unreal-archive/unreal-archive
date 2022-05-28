package net.shrimpworks.unreal.archive.content.maps;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class GameTypes {

	private static final java.util.Map<String, GameType> mapMapping = new HashMap<>();

	private static final List<GameType> GAME_TYPES = Arrays.asList(
		new GameType("Single Player", "SP", "OSM"),
		new GameType("1 on 1", "DM-1on1", "DM-1v1", "DM-1-on-1"),
		new GameType("Infiltration", "INF", "CTF-INF-", "DM-INF-", "DOM-INF-", "AS-INF-", "EAS-INF-"),
		new GameType("DeathMatch", "DM"),
		new GameType("DarkMatch", "DK", "TDK"),
		new GameType("BunnyTrack", "CTF-BT", "BT"),
		new GameType("Multi-Team CTF", "CTF4", "CTFM", "MCTF"),
		new GameType("Capture The Flag", "CTF"),
		new GameType("Greed", "CTF-GRD-", "VCTF-GRD-"),
		new GameType("Domination", "DOM"),
		new GameType("Assault", "AS"),
		new GameType("Bombing Run", "BR"),
		new GameType("Onslaught", "ONS"),
		new GameType("Vehicle CTF", "VCTF"),
		new GameType("Warfare", "WAR"),
		new GameType("Monster Hunt", "MH"),
		new GameType("Monster Arena", "MA"),
		new GameType("Team Monster Hunt", "TMH"),
		new GameType("Rocket Arena", "RA-"),
		new GameType("Jailbreak", "JB"),
		new GameType("Tactical Ops", "TO", "SW"),
		new GameType("Strike Force", "SF"),
		new GameType("UnWheel", "UW"),
		new GameType("Thievery", "TH-"),
		new GameType("Unreal4Ever", "U4E"),
		new GameType("Unreal Fortress", "UNF"),
		new GameType("XMP", "XMP"),
		new GameType("FragBall", "FB"),
		new GameType("Flag Domination", "FD"),
		new GameType("Soccer Tournament", "SCR"),
		new GameType("Killing Floor", "KF"),
		new GameType("AirFight", "AF", "DM-AF-", "CTF-AF-", "DOM-AF-"),
		new GameType("DeathBall", "DB"),
		new GameType("Unreal Racer", "UNR-"),
		new GameType("Air Buccaneers", "ABU"),
		new GameType("Clone Bandits", "CLN"),
		new GameType("Red Orchestra", "RO-"),
		new GameType("Fraghouse Invasion", "FHI"),
		new GameType("SoldatUT", "2DDM", "2DDOM", "2DONS", "2DCTF", "2DBR"),
		new GameType("Dodge Professional Modification", "DPM"),
		new GameType("Scavenger Hunt", "SH"),
		new GameType("Smashdroids", "SD"),
		new GameType("ChaosUT", "KOTH", "DM-CUT", "CTF-CUT", "DOM-CUT"),
		new GameType("Funnel", "FN"),
		new GameType("Survival", "SV"),
		new GameType("Conquest", "CNQ"),
		new GameType("RealCTF", "Real_"),
		new GameType("Unreal Badlands", "BL-", "BLC-"),
		new GameType("CarBall", "CB-"),
		new GameType("Classic Domination", "cDOM-"),
		new GameType("XVehicles", "AS-XV-", "CTF-XV-", "CTFM-XV-", "DM-XV-", "MH-XV-", "DOM-XV-"),
		new GameType("Perfect Dark UT", "PD-"),
		new GameType("Monster Match", "MM-"),
		new GameType("Alien Swarm", "AO-"),
		new GameType("Movies, Machinimas and Entries", "MOV-"),
		new GameType("Double Domination", "DDOM")
		new GameType("Invasion", "DM-INV-", "DM-(INV)-")
	);

	/**
	 * Attempt to find a gametype for a given map name, using a very naive longest prefix match.
	 */
	public static GameType forMap(String mapName) {
		String lower = mapName.toLowerCase();
		List<String> sortedPrefixes = mapMapping.keySet().stream()
												.sorted((a, b) -> -Integer.compare(a.length(), b.length()))
												.collect(Collectors.toList());
		for (String p : sortedPrefixes) {
			if (lower.startsWith(p.toLowerCase())) return mapMapping.get(p);
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

			for (String mapPrefix : mapPrefixes) {
				mapMapping.put(mapPrefix, this);
			}
		}

		public GameType(String name, String... mapPrefixes) {
			this(name, Arrays.asList(mapPrefixes));
		}
	}
}
