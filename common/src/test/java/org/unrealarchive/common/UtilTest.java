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

		// '&' and ',' separate a query's parameters and values, and must survive encoding, while
		// characters already encoded within a value must not be encoded again
		assertEquals("http://medor.no-ip.org/index.php?dir=&search_mode=f&search=Fury+part2,fixed.zip",
					 Util.url("http://medor.no-ip.org/index.php?dir=&search_mode=f&search=Fury+part2,fixed.zip").toString());
		assertEquals("http://ut-files.com/index.php?dir=Maps/DeathMatch/&file=Dm-PacmanSE%26NT.zip",
					 Util.url("http://ut-files.com/index.php?dir=Maps/DeathMatch/&file=Dm-PacmanSE%26NT.zip").toString());

		// ... within a path however, they're escaped
		assertEquals("https://example.com/maps/DM-Pool%26Mirrors%2CFixed.zip",
					 Util.url("https://example.com/maps/DM-Pool&Mirrors,Fixed.zip").toString());

		// '#' would truncate a URL, so it's escaped in both paths and queries
		assertEquals("https://example.com/maps/GiantMap_pack_%231.zip?file=pack%232.zip",
					 Util.url("https://example.com/maps/GiantMap_pack_#1.zip?file=pack#2.zip").toString());

		// encoding is idempotent, since stored URLs are already (partially) encoded
		for (String u : List.of(
			"https://example.com/Unreal Tournament/DM-My Cool Map[].zip",
			"https://example.com/maps/DM-My+Cool+Map.zip",
			"http://medor.no-ip.org/index.php?dir=&search_mode=f&search=Fury+part2.zip"
		)) {
			String once = Util.url(u).toString();
			assertEquals(once, Util.url(once).toString());
		}
	}
}
