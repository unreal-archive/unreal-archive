package org.unrealarchive.content;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContributorsTest {
	@TempDir
	Path tmpDir;

	@Test
	public void namesTest() throws IOException {
		Set<String> names = Contributors.names("Alan 'Talisman' Willard");
		assertTrue(names.contains("Alan 'Talisman' Willard"));

		names = Contributors.names("Alan Willard & Daedalus, Bob Brown");
		System.out.println(names);
		assertTrue(names.contains("Alan Willard"));
		assertTrue(names.contains("Daedalus"));
		assertTrue(names.contains("Bob Brown"));

		names = Contributors.names("Bastiaan Frank (Dominated By: JiveTurkey)");
		System.out.println(names);
		assertTrue(names.contains("Bastiaan Frank"));
		assertTrue(names.contains("JiveTurkey"));

		names = Contributors.names("Alan Willard, Amplified by {LoD}Ultron");
		System.out.println(names);
		assertTrue(names.contains("Alan Willard"));
		assertTrue(names.contains("{LoD}Ultron"));

		names = Contributors.names("Kaal\"TheSinew\"979 * edited by EvilGrins");
		System.out.println(names);
		assertTrue(names.contains("Kaal\"TheSinew\"979"));
		assertTrue(names.contains("EvilGrins"));

		names = Contributors.names("done by Red Dwarf · edited by EvilGrins");
		System.out.println(names);
		assertTrue(names.contains("Red Dwarf"));
		assertTrue(names.contains("EvilGrins"));

		names = Contributors.names("Cliff Bleszinski (Unreal Conversion By UKBikenut)");
		System.out.println(names);
		assertTrue(names.contains("Cliff Bleszinski"));
		assertTrue(names.contains("UKBikenut)"));

		names = Contributors.names("Cliff Bleszinski / Alan Willard");
		System.out.println(names);
		assertTrue(names.contains("Cliff Bleszinski"));
		assertTrue(names.contains("Alan Willard"));

		names = Contributors.names("made by Turret 49 & Konin · edited by EvilGrins");
		System.out.println(names);
		assertTrue(names.contains("Turret 49"));
		assertTrue(names.contains("Konin"));
		assertTrue(names.contains("EvilGrins"));
	}

	@Test
	public void parseNames() throws IOException {
		Authors.setRepository(new AuthorRepository.FileRepository(tmpDir), tmpDir);

		List<String> samples = List.of(
			"Alan 'Talisman' Willard",
			"Alan Willard & Daedalus, Bob Brown",
			"Alan Willard, Amplified by {LoD}Ultron",
			"Whitey(Original By Alan Willard)",
			"Alan Willard / Agent_Scully",
			"Alan Willard - edit by mtbpman",
			"Alan Willard, CTF Version by Yabi MoCheez",
			"Alan Willard [FD adaptation Eric Pietrocupo]",
			"Originally Alan 'Talisman' Willard, modded by Barbie (Rev 25)",
			"Alan 'Talisman' Willard, SE by PJMODOS",
			"Alan 'Talisman' Willard -- v105 by DarkStar",
			"Alan 'Talisman' Willard CE: [J]-Outsiders71",
			"Alan 'Talisman' Willard - Edited by Martijn 'dxeh' kooij",
			"Alan 'Talisman' Willard (CTF By Kraken)",
			"Alan 'Talisman' Willard (Unreal conversion by Pitbull)",
			"Alan 'Talisman' Willard -- edit by NelizMastr",
			"Cedric 'Inoxx' Fiorentino (original), EvilGrins (edited)",
			"Kaal\"TheSinew\"979 * edited by EvilGrins",
			"Deepak OV made · EvilGrins edited",
			"DeeZNutZ modified by Can-o-Worms",
			"ZoiL -- modified by -fanat1c-",
			"done by Red Dwarf · edited by EvilGrins",
			"Action-Force©/edit by nodin",
			"Shock Systems / Map & Textures by Jeff 'Monger' Randall Pathing and Mods By Peter 'Freak' Caccolia",
			"Sp0ngeb0b (original by PsK clan)",
			"Big_Balou (original), EvilGrins (edited)",
			"RoelerCoaster (Original), Buggie (XV)",
			"{Wtf}Diealot original map by MsM - [X]",
			"{Wtf}Diealot, original concept by l3fty",
			"Original John Jones/Edited by [PDS]Salty Pepper",
			"Quintin Stone and the Rebel Programmers Society",
			"DarkSniper/Slapshot/Esop",
			"LuniC - Remixed by DarkSniper",
			"René \"Elite6\" Bokhorst - remixed by eko",
			"Myscha and remixed by Mr.Prophet",
			"BronxBobby *imported from UT by RUSH*",
			"Kaal979 (original), EvilGrins (edited)",
			"Rob Collins + Nachimir",
			"Epic Games / Digital Extremes",
			"John Paul 'Decker' Jones//Edited by |2Åmßõ*Ä§§*",
			"Bastiaan Frank (Dominated By: JiveTurkey)"
		);

		for (String sample : samples) {
			System.out.println(sample);
			Contributors contribs = new Contributors(sample);
			System.out.println(contribs);
			System.out.println();
		}
	}
}
