package org.unrealarchive.common;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.Test;

public class UtilTest {

	@Test
	public void urlTest() throws MalformedURLException {
		for (String u : List.of(
			"https://files.vohzd.com/unreal/unreal-tournament/maps/death-match/dm-`pyramurder.zip",
			"https://files.vohzd.com/unreal/unreal-tournament/maps/death-match/DM-(ASC)3ommies-resort{MOS}Edition.zip",
			"https://files.vohzd.com/unreal/unreal-tournament/maps/bunny-track/CTF-BT-Deck16][Sp!Re`.zip",
			"https://files.vohzd.com/unreal/rune/maps/death-match/DM-Tri^Arena.zip"
		)) {
			System.out.println(new URL(u));
			System.out.println(Util.url(u));
		}
	}
}
