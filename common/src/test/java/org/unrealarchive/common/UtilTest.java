package org.unrealarchive.common;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

	@Test
	public void urlEncodingTest() throws MalformedURLException {
		assertEquals("https://example.com/Unreal%20Tournament/DM-My%20Cool%20Map%5B%5D.zip",
					 Util.url("https://example.com/Unreal Tournament/DM-My Cool Map[].zip").toString());

		// S3 hosts 404 on a literal '+' in a path, so it has to be encoded
		assertEquals("https://example.com/maps/DM-My+Cool+Map.zip".replace("+", "%2B"),
					 Util.url("https://example.com/maps/DM-My+Cool+Map.zip").toString());

		// ... but in a query string '+' means space, and must be left as-is
		assertEquals("https://example.com/index.php?file=DM-My+Cool+Map.zip",
					 Util.url("https://example.com/index.php?file=DM-My+Cool+Map.zip").toString());
	}
}
