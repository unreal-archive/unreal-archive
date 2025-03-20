package org.unrealarchive.content.addons;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.unrealarchive.content.Games;

import static org.unrealarchive.content.Games.*;

public class MapGameTypes {

	private static final List<MapGameType> GAME_TYPES = Arrays.asList(
		new MapGameType(null, "Single Player", "SP", "OSM"),
		new MapGameType(null, "1 on 1", "DM-1on1", "DM-1v1", "DM-1-on-1"),
		new MapGameType(null, "Infiltration", "INF", "CTF-INF-", "DM-INF-", "DOM-INF-", "AS-INF-", "EAS-INF-"),
		new MapGameType(null, "DeathMatch", "DM"),
		new MapGameType(null, "DarkMatch", "DK", "TDK"),
		new MapGameType(UNREAL_TOURNAMENT, "BunnyTrack", "CTF-BT", "BT"),
		new MapGameType(null, "Multi-Team CTF", "CTF4", "CTFM", "MCTF"),
		new MapGameType(null, "Capture The Flag", "CTF"),
		new MapGameType(UNREAL_TOURNAMENT_3, "Greed", "CTF-GRD", "VCTF-GRD", "CTF-Greed-", "VCTF-Greed-"),
		new MapGameType(null, "Domination", "DOM", "CDOM"),
		new MapGameType(null, "Assault", "AS"),
		new MapGameType(null, "Bombing Run", "BR"),
		new MapGameType(null, "Onslaught", "ONS"),
		new MapGameType(null, "Vehicle CTF", "VCTF"),
		new MapGameType(null, "Warfare", "WAR"),
		new MapGameType(UNREAL_TOURNAMENT, "Monster Hunt", "MH"),
		new MapGameType(UNREAL_TOURNAMENT, "Monster Arena", "MA"),
		new MapGameType(UNREAL, "Sniper's Paradise Monster Hunt", "MH-[SP]", "MH-"),
		new MapGameType(null, "Team Monster Hunt", "TMH"),
		new MapGameType(null, "Rocket Arena", "RA-"),
		new MapGameType(null, "Jailbreak", "JB"),
		new MapGameType(UNREAL_TOURNAMENT, "Tactical Ops", "TO", "SW"),
		new MapGameType(UNREAL_TOURNAMENT, "Strike Force", "SF"),
		new MapGameType(null, "UnWheel", "UW"),
		new MapGameType(UNREAL_TOURNAMENT, "Thievery", "TH-"),
		new MapGameType(null, "Unreal4Ever", "U4E"),
		new MapGameType(null, "Unreal Fortress", "UNF"),
		new MapGameType(null, "XMP", "XMP"),
		new MapGameType(null, "FragBall", "FB"),
		new MapGameType(null, "Flag Domination", "FD"),
		new MapGameType(null, "Soccer Tournament", "SCR"),
		new MapGameType(null, "Killing Floor", "KF"),
		new MapGameType(null, "AirFight", "AF", "DM-AF-"),
		new MapGameType(null, "DeathBall", "DB"),
		new MapGameType(null, "Unreal Racer", "UNR-"),
		new MapGameType(null, "Air Buccaneers", "ABU"),
		new MapGameType(null, "Clone Bandits", "CLN"),
		new MapGameType(UNREAL_TOURNAMENT_2004, "Red Orchestra", "RO-"),
		new MapGameType(null, "Fraghouse Invasion", "FHI"),
		new MapGameType(null, "SoldatUT", "2DDM", "2DDOM", "2DONS", "2DCTF", "2DBR"),
		new MapGameType(null, "Dodge Professional Modification", "DPM"),
		new MapGameType(null, "Scavenger Hunt", "SH"),
		new MapGameType(UNREAL_TOURNAMENT, "Smashdroids", "SD"),
		new MapGameType(null, "ChaosUT", "KOTH", "DM-CUT", "CTF-CUT", "DOM-CUT"),
		new MapGameType(null, "Funnel", "FN"),
		new MapGameType(null, "Survival", "SV"),
		new MapGameType(null, "Conquest", "CNQ"),
		new MapGameType(null, "RealCTF", "Real_"),
		new MapGameType(null, "Unreal Badlands", "BL-", "BLC-"),
		new MapGameType(null, "CarBall", "CB-"),
		new MapGameType(null, "XVehicles", "AS-XV-", "CTF-XV-", "CTFM-XV-", "DM-XV-", "MH-XV-", "DOM-XV-"),
		new MapGameType(null, "Perfect Dark UT", "PD-"),
		new MapGameType(null, "Monster Match", "MM-"),
		new MapGameType(null, "Alien Swarm", "AO-"),
		new MapGameType(null, "Movies, Machinimas and Entries", "MOV-"),
		new MapGameType(UNREAL_TOURNAMENT, "Double Domination", "DDOM"),
		new MapGameType(UNREAL_TOURNAMENT_2004, "Double Domination", "DOM"),
		new MapGameType(null, "Invasion", "DM-INV-", "DM-(INV)-"),
		new MapGameType(null, "UT2D", "UT2D-"),
		new MapGameType(UNREAL_TOURNAMENT_3, "Kilter's Bombing Run", "KBR-", "VKBR-"),
		new MapGameType(UNREAL_TOURNAMENT_3, "The Haunted", "TH-"),
		new MapGameType(null, "Crystal Castles", "CC-"),
		// Rune gametypes
		new MapGameType(RUNE, "Arena Match", "AR-"),
		new MapGameType(RUNE, "Headball", "HB-"),
		new MapGameType(RUNE, "Sarkball", "SB-"),
		new MapGameType(RUNE, "Siege", "SG-"),
		new MapGameType(RUNE, "Thirsty Vikings", "TV-"),
		new MapGameType(RUNE, "Capture the Torch", "CTT-")
	);

	// byName is called many times while generating output - cache the lookup results for small optimisation
	private static final java.util.Map<String, MapGameType> LOOKUP_CACHE = new ConcurrentHashMap<>();

	/**
	 * Attempt to find a gametype for a given map name, using a very naive longest prefix match.
	 */
	public static MapGameType forMap(Games game, String mapName) {
		String lower = mapName.toLowerCase();

		final java.util.Map<String, MapGameType> mapMapping = new HashMap<>();
		GAME_TYPES.stream().filter(t -> t.game == null || t.game == game).forEach(g -> g.mapPrefixes.forEach(p -> mapMapping.put(p, g)));

		List<String> sortedPrefixes = mapMapping.keySet().stream()
												.sorted((a, b) -> -Integer.compare(a.length(), b.length()))
												.toList();
		for (String p : sortedPrefixes) {
			MapGameType type = mapMapping.get(p);
			if (lower.startsWith(p.toLowerCase())) return type;
		}
		return null;
	}

	public static MapGameType byName(String name) {
		String lower = name.toLowerCase();
		return LOOKUP_CACHE.computeIfAbsent(lower, l -> {
			for (MapGameType gt : GAME_TYPES) {
				if (gt.name.toLowerCase().equals(lower)) return gt;
			}
			return null;
		});
	}

	public record MapGameType(Games game, String name, Collection<String> mapPrefixes) {

		public MapGameType(Games game, String name, String... mapPrefixes) {
			this(game, name, Arrays.asList(mapPrefixes));
		}
	}

}
