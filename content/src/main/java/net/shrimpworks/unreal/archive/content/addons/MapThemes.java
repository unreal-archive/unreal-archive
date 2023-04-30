package net.shrimpworks.unreal.archive.content.addons;

import java.util.Map;

public class MapThemes {

	public static final int MAX_THEMES = 5;
	public static final double MIN_THRESHOLD = 0.05d; // only include something as a theme if it contributes more than 5%

	public static final Map<String, String> THEMES;

	static final String TH_ANCIENT = "Ancient";
	static final String TH_NALI_TEMPLE = "Nali Temple";
	static final String TH_NALI_CASTLE = "Nali Castle";
	static final String TH_TECH = "Tech";
	static final String TH_INDUSTRIAL = "Industrial";
	static final String TH_CITY = "City";
	static final String TH_EGYPTIAN = "Egyptian";
	static final String TH_NATURAL = "Natural";
	static final String TH_SKAARJ_TECH = "Skaarj Tech";
	static final String TH_SKAARJ_CRYPT = "Skaarj Crypt";

	static {
		THEMES = Map.<String, String>ofEntries(
			/*
			  Unreal Tournament
			*/
			Map.entry("ancient", TH_NALI_TEMPLE),
			Map.entry("arenatex", TH_TECH),
			Map.entry("coret_fx", TH_TECH),
			Map.entry("city", TH_CITY),
			Map.entry("crypt_fx", TH_SKAARJ_CRYPT),
			Map.entry("crypt2", TH_SKAARJ_CRYPT),
			Map.entry("decayeds", TH_SKAARJ_TECH),
			Map.entry("egypt", TH_EGYPTIAN),
			Map.entry("egyptpan", TH_EGYPTIAN),
			Map.entry("eol", TH_ANCIENT),
			Map.entry("fractalfx", TH_TECH),
			Map.entry("genearth", TH_NATURAL),
			Map.entry("genfluid", TH_NATURAL),
			Map.entry("genin", TH_ANCIENT),
			Map.entry("genterra", TH_NATURAL),
			Map.entry("indus1", TH_NATURAL),
			Map.entry("indus2", TH_ANCIENT),
			Map.entry("indus3", TH_TECH),
			Map.entry("indus4", TH_TECH),
			Map.entry("indus5", TH_TECH),
			Map.entry("indus6", TH_ANCIENT),
			Map.entry("indus7", TH_TECH),
			Map.entry("lian-x", TH_TECH),
			Map.entry("metalmys", TH_INDUSTRIAL),
			Map.entry("mine", TH_SKAARJ_TECH),
			Map.entry("nalicast", TH_NALI_CASTLE),
			Map.entry("nalifX", TH_NALI_CASTLE),
			Map.entry("noxxpack", TH_INDUSTRIAL),
			Map.entry("old_fx", TH_NALI_TEMPLE),
			Map.entry("playrshp", TH_TECH),
			Map.entry("queen", TH_SKAARJ_TECH),
			Map.entry("rainfx", TH_INDUSTRIAL),
			Map.entry("richrig", TH_INDUSTRIAL),
			Map.entry("sgtech1", TH_TECH),
			Map.entry("anc", TH_ANCIENT),
			Map.entry("factory", TH_INDUSTRIAL),
			Map.entry("citytex", TH_CITY),
			Map.entry("a3text", TH_CITY),
			Map.entry("fluid", TH_NATURAL),
			Map.entry("anc2", TH_ANCIENT),
			Map.entry("hourindusx_ut", TH_INDUSTRIAL),
			Map.entry("hourpitores_ut", TH_INDUSTRIAL),
			Map.entry("wood", TH_INDUSTRIAL),
			Map.entry("quake3", TH_TECH),
			Map.entry("uttech4", TH_INDUSTRIAL),
			Map.entry("sscoldsteel", TH_TECH),
			Map.entry("quake3c", TH_TECH),
			Map.entry("ribeira3", TH_INDUSTRIAL),
			Map.entry("anc4", TH_ANCIENT),
			Map.entry("dino081199", TH_INDUSTRIAL),
			Map.entry("ribeira1", TH_INDUSTRIAL),
			Map.entry("ribeira2", TH_INDUSTRIAL),
			Map.entry("drmpa", TH_INDUSTRIAL),
			Map.entry("egyptian", TH_EGYPTIAN),
			Map.entry("uttech1", TH_INDUSTRIAL),
			Map.entry("braveheart", TH_ANCIENT),
			Map.entry("jump", TH_TECH),
			Map.entry("zeitkind", TH_INDUSTRIAL),
			Map.entry("houriceskaarj", TH_TECH),
			Map.entry("fallout", TH_INDUSTRIAL),
			Map.entry("castle", TH_ANCIENT),
			Map.entry("ambrosia", TH_NATURAL),
			Map.entry("chicoruinspack", TH_ANCIENT),
			Map.entry("gravdig", TH_ANCIENT),
			Map.entry("slums", TH_CITY),
			Map.entry("hourkraden_ut", TH_INDUSTRIAL),
			Map.entry("forsakent", TH_ANCIENT),

			Map.entry("urban", TH_CITY),
			Map.entry("blade", TH_CITY),
			Map.entry("vierheilig", TH_INDUSTRIAL),
			Map.entry("shanechurch", TH_ANCIENT),

			Map.entry("hourdinoratex", TH_INDUSTRIAL),
			Map.entry("r3tex", TH_ANCIENT),
			Map.entry("bentropical01", TH_NATURAL),

			/*
			  Unreal Tournament 2004
			*/
			Map.entry("humanoidarchitecture", TH_INDUSTRIAL),
			Map.entry("humanoidarchitecture2", TH_INDUSTRIAL),
			Map.entry("abaddonarchitecture", TH_INDUSTRIAL),
			Map.entry("abaddonarchitecture-tech", TH_INDUSTRIAL),
			Map.entry("shiptech", TH_TECH),
			Map.entry("shiptech2", TH_TECH),
			Map.entry("alleriaarchitecture", TH_TECH),
			Map.entry("barrensarchitecture", TH_EGYPTIAN),
			Map.entry("barrensarchitecture-epic", TH_EGYPTIAN),
			Map.entry("barrensarchitecture-scion", TH_EGYPTIAN),
			Map.entry("h_e_l_ltx", TH_ANCIENT),
			Map.entry("arboreaarchitecture", TH_ANCIENT),
			Map.entry("albatross_architecture", TH_ANCIENT),
			Map.entry("alleriaterrain", TH_NATURAL),
			Map.entry("phobos2_cp", TH_TECH),
			Map.entry("abaddonterrain", TH_NATURAL),
			Map.entry("barrensterrain", TH_NATURAL),
			Map.entry("cp_evil1", TH_TECH),
			Map.entry("cp_evil2", TH_TECH),
			Map.entry("cp_evil3", TH_TECH),
			Map.entry("sc_volcano_t", TH_NATURAL),
			Map.entry("antalustextures", TH_NATURAL),
			Map.entry("x_cp_evil1", TH_TECH),
			Map.entry("x_cp_evil2", TH_TECH),
			Map.entry("x_cp_evil3", TH_TECH),
			Map.entry("fareast", TH_ANCIENT),
			Map.entry("skyline-epic", TH_CITY),
			Map.entry("aw-metals", TH_INDUSTRIAL),
			Map.entry("aw-metals2", TH_INDUSTRIAL),
			Map.entry("aw-stone", TH_CITY),
			Map.entry("x_mechcity1_cp", TH_CITY),
			Map.entry("x_mechstandard", TH_TECH),
			Map.entry("towerterrain", TH_INDUSTRIAL),
			Map.entry("davestextures", TH_NATURAL),
			Map.entry("mech_decayed", TH_INDUSTRIAL),
			Map.entry("jwdecemberarchitecture", TH_INDUSTRIAL),
			Map.entry("c_sc-city", TH_CITY),
			Map.entry("despfallencity", TH_CITY),
			Map.entry("cp_ut2k3_techset1", TH_TECH),
			Map.entry("mechstandard", TH_TECH),
			Map.entry("cf_tex01", TH_TECH),
			Map.entry("cf_tex02", TH_TECH),
			Map.entry("cp_evilmetal", TH_TECH),
			Map.entry("hourmoria", TH_NATURAL),
			Map.entry("x_mech_decayed", TH_TECH),
			Map.entry("gaciertextures", TH_TECH),
			Map.entry("x_futuretech1", TH_TECH),
			Map.entry("futuretech1", TH_TECH),
			Map.entry("x_aw-convert", TH_NATURAL),
			Map.entry("pc_urbantex", TH_CITY),
			Map.entry("pipe_set", TH_INDUSTRIAL),
			Map.entry("2k4reducedtextures", TH_INDUSTRIAL),
			Map.entry("village", TH_ANCIENT),
			Map.entry("alientex", TH_TECH),
			Map.entry("xceptone", TH_TECH),
			Map.entry("xcepttwo", TH_TECH),
			Map.entry("xceptthree", TH_TECH),
			Map.entry("aw-citystuff", TH_CITY),
			Map.entry("industrial", TH_INDUSTRIAL),
			Map.entry("cp_forestswamp", TH_NATURAL),
			Map.entry("terrain", TH_NATURAL),
			Map.entry("cp_junkyard", TH_INDUSTRIAL)
		);
	}

	public static String findTheme(String packageName) {
		return THEMES.get(packageName.trim().toLowerCase());
	}

}
