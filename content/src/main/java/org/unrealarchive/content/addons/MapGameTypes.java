package org.unrealarchive.content.addons;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapGameTypes {

	private static final Map<String, MapGameType> mapMapping = new HashMap<>();

	private static final List<MapGameType> GAME_TYPES = Arrays.asList(
		new MapGameType("Single Player", "SP", "OSM"),
		new MapGameType("1 on 1", "DM-1on1", "DM-1v1", "DM-1-on-1"),
		new MapGameType("Infiltration", "INF", "CTF-INF-", "DM-INF-", "DOM-INF-", "AS-INF-", "EAS-INF-"),
		new MapGameType("DeathMatch", "DM"),
		new MapGameType("DarkMatch", "DK", "TDK"),
		new MapGameType("BunnyTrack", "CTF-BT", "BT"),
		new MapGameType("Multi-Team CTF", "CTF4", "CTFM", "MCTF"),
		new MapGameType("Capture The Flag", "CTF"),
		new MapGameType("Greed", "CTF-GRD", "VCTF-GRD"),
		new MapGameType("Domination", "DOM", "CDOM"),
		new MapGameType("Assault", "AS"),
		new MapGameType("Bombing Run", "BR"),
		new MapGameType("Onslaught", "ONS"),
		new MapGameType("Vehicle CTF", "VCTF"),
		new MapGameType("Warfare", "WAR"),
		new MapGameType("Monster Hunt", "MH"),
		new MapGameType("Monster Arena", "MA"),
		new MapGameType("Team Monster Hunt", "TMH"),
		new MapGameType("Rocket Arena", "RA-"),
		new MapGameType("Jailbreak", "JB"),
		new MapGameType("Tactical Ops", "TO", "SW"),
		new MapGameType("Strike Force", "SF"),
		new MapGameType("UnWheel", "UW"),
		new MapGameType("Thievery", "TH-"),
		new MapGameType("Unreal4Ever", "U4E"),
		new MapGameType("Unreal Fortress", "UNF"),
		new MapGameType("XMP", "XMP"),
		new MapGameType("FragBall", "FB"),
		new MapGameType("Flag Domination", "FD"),
		new MapGameType("Soccer Tournament", "SCR"),
		new MapGameType("Killing Floor", "KF"),
		new MapGameType("AirFight", "AF", "DM-AF-", "CTF-AF-", "DOM-AF-"),
		new MapGameType("DeathBall", "DB"),
		new MapGameType("Unreal Racer", "UNR-"),
		new MapGameType("Air Buccaneers", "ABU"),
		new MapGameType("Clone Bandits", "CLN"),
		new MapGameType("Red Orchestra", "RO-"),
		new MapGameType("Fraghouse Invasion", "FHI"),
		new MapGameType("SoldatUT", "2DDM", "2DDOM", "2DONS", "2DCTF", "2DBR"),
		new MapGameType("Dodge Professional Modification", "DPM"),
		new MapGameType("Scavenger Hunt", "SH"),
		new MapGameType("Smashdroids", "SD"),
		new MapGameType("ChaosUT", "KOTH", "DM-CUT", "CTF-CUT", "DOM-CUT"),
		new MapGameType("Funnel", "FN"),
		new MapGameType("Survival", "SV"),
		new MapGameType("Conquest", "CNQ"),
		new MapGameType("RealCTF", "Real_"),
		new MapGameType("Unreal Badlands", "BL-", "BLC-"),
		new MapGameType("CarBall", "CB-"),
		new MapGameType("XVehicles", "AS-XV-", "CTF-XV-", "CTFM-XV-", "DM-XV-", "MH-XV-", "DOM-XV-"),
		new MapGameType("Perfect Dark UT", "PD-"),
		new MapGameType("Monster Match", "MM-"),
		new MapGameType("Alien Swarm", "AO-"),
		new MapGameType("Movies, Machinimas and Entries", "MOV-"),
		new MapGameType("Double Domination", "DDOM"),
		new MapGameType("Invasion", "DM-INV-", "DM-(INV)-"),
		new MapGameType("UT2D", "UT2D-"),
		new MapGameType("Kilter's Bombing Run", "KBR-", "VKBR-"),
		// Rune gametypes
		new MapGameType("Arena Match", "AR-"),
		new MapGameType("Headball", "HB-"),
		new MapGameType("Sarkball", "SB-"),
		new MapGameType("Siege", "SG-"),
		new MapGameType("Thirsty Vikings", "TV-"),
		new MapGameType("Capture the Torch", "CTT-")
	);

	/**
	 * Attempt to find a gametype for a given map name, using a very naive longest prefix match.
	 */
	public static MapGameType forMap(String mapName) {
		String lower = mapName.toLowerCase();
		List<String> sortedPrefixes = mapMapping.keySet().stream()
												.sorted((a, b) -> -Integer.compare(a.length(), b.length()))
												.toList();
		for (String p : sortedPrefixes) {
			if (lower.startsWith(p.toLowerCase())) return mapMapping.get(p);
		}
		return null;
	}

	public static MapGameType byName(String name) {
		String lower = name.toLowerCase();
		for (MapGameType gt : GAME_TYPES) {
			if (gt.name.toLowerCase().equals(lower)) return gt;
		}
		return null;
	}

	public static class MapGameType {

		public final String name;
		public final Collection<String> mapPrefixes;

		public MapGameType(String name, Collection<String> mapPrefixes) {
			this.name = name;
			this.mapPrefixes = mapPrefixes;

			for (String mapPrefix : mapPrefixes) {
				mapMapping.put(mapPrefix, this);
			}
		}

		public MapGameType(String name, String... mapPrefixes) {
			this(name, Arrays.asList(mapPrefixes));
		}
	}
}
