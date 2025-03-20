package org.unrealarchive.content.addons;

import org.unrealarchive.content.Games;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapGameTypesTest {

	@Test
	public void gametypeDetection() {
		assertEquals("Greed", MapGameTypes.forMap(Games.UNREAL_TOURNAMENT_3, "CTF-GRD-Cake").name());
		assertEquals("Greed", MapGameTypes.forMap(Games.UNREAL_TOURNAMENT_3, "VCTF-GRD-Cake").name());
		assertEquals("Capture The Flag", MapGameTypes.forMap(Games.RUNE, "CTF-Lies").name());
		assertEquals("DeathMatch", MapGameTypes.forMap(Games.UNREAL_TOURNAMENT, "DM-DeathMatch").name());

		assertEquals("Thievery", MapGameTypes.forMap(Games.UNREAL_TOURNAMENT, "TH-Sneaky").name());
		assertEquals("The Haunted", MapGameTypes.forMap(Games.UNREAL_TOURNAMENT_3, "TH-Scary").name());
	}

}
