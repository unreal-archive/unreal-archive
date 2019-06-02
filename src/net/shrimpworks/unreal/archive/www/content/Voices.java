package net.shrimpworks.unreal.archive.www.content;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.ContentManager;
import net.shrimpworks.unreal.archive.content.voices.Voice;
import net.shrimpworks.unreal.archive.www.SiteMap;
import net.shrimpworks.unreal.archive.www.Templates;

import static net.shrimpworks.unreal.archive.Util.slug;

public class Voices extends ContentPageGenerator {

	private static final String SECTION = "Voices";

	private final Games games;
	private final Path siteRoot;

	public Voices(ContentManager content, Path output, Path staticRoot, boolean localImages) {
		super(content, output.resolve("voices"), staticRoot, localImages);
		this.siteRoot = output;

		this.games = new Games();

		content.get(Voice.class).stream()
			   .filter(v -> !v.deleted)
			   .filter(v -> v.variationOf == null || v.variationOf.isEmpty())
			   .sorted()
			   .forEach(v -> {
				   Game g = games.games.computeIfAbsent(v.game, Game::new);
				   g.add(v);
			   });
	}

	@Override
	public Set<SiteMap.Page> generate() {
		Templates.PageSet pages = new Templates.PageSet("content/voices", siteRoot, staticRoot, root);
		try {
			pages.add("games.ftl", SiteMap.Page.monthly(0.6f), SECTION)
				 .put("games", games)
				 .write(root.resolve("index.html"));

			for (java.util.Map.Entry<String, Game> g : games.games.entrySet()) {

				var game = net.shrimpworks.unreal.archive.content.Games.byName(g.getKey());

				if (g.getValue().voices < Templates.PAGE_SIZE) {
					List<VoiceInfo> all = g.getValue().letters.values().stream()
															  .flatMap(l -> l.pages.stream())
															  .flatMap(e -> e.voices.stream())
															  .sorted()
															  .collect(Collectors.toList());
					pages.add("listing_single.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName))
						 .put("game", g.getValue())
						 .put("voices", all)
						 .write(g.getValue().path.resolve("index.html"));

					// still generate all map pages
					for (VoiceInfo voice : all) {
						voicePage(pages, voice);
					}

					continue;
				}

				for (java.util.Map.Entry<String, LetterGroup> l : g.getValue().letters.entrySet()) {

					for (Page p : l.getValue().pages) {
						pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName))
							 .put("page", p)
							 .write(p.path.resolve("index.html"));

						for (VoiceInfo voice : p.voices) {
							voicePage(pages, voice);
						}
					}

					// output first letter/page combo, with appropriate relative links
					pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName))
						 .put("page", l.getValue().pages.get(0))
						 .write(l.getValue().path.resolve("index.html"));
				}

				// output first letter/page combo, with appropriate relative links
				pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName))
					 .put("page", g.getValue().letters.firstEntry().getValue().pages.get(0))
					 .write(g.getValue().path.resolve("index.html"));
			}

		} catch (IOException e) {
			throw new RuntimeException("Failed to render page", e);
		}

		return pages.pages;
	}

	private void voicePage(Templates.PageSet pages, VoiceInfo voice) throws IOException {
		localImages(voice.voice, root.resolve(voice.path).getParent());

		pages.add("voice.ftl", SiteMap.Page.monthly(0.9f, voice.voice.lastIndex), String.join(" / ", SECTION,
																							  voice.page.letter.game.game.bigName,
																							  voice.voice.name))
			 .put("voice", voice)
			 .write(Paths.get(voice.path.toString() + ".html"));

		// since variations are not top-level things, we need to generate them here
		for (VoiceInfo variation : voice.variations) {
			voicePage(pages, variation);
		}
	}

	public class Games {

		public final TreeMap<String, Game> games = new TreeMap<>();
	}

	public class Game {

		public final net.shrimpworks.unreal.archive.content.Games game;
		public final String name;
		public final String slug;
		public final Path path;
		public final TreeMap<String, LetterGroup> letters = new TreeMap<>();
		public int voices;

		public Game(String name) {
			this.game = net.shrimpworks.unreal.archive.content.Games.byName(name);
			this.name = name;
			this.slug = slug(name);
			this.path = root.resolve(slug);
			this.voices = 0;
		}

		public void add(Voice voice) {
			LetterGroup letter = letters.computeIfAbsent(voice.subGrouping(), l -> new LetterGroup(this, l));
			letter.add(voice);
			this.voices++;
		}
	}

	public class LetterGroup {

		public final Game game;
		public final String letter;
		public final Path path;
		public final List<Page> pages = new ArrayList<>();
		public int voices;

		public LetterGroup(Game game, String letter) {
			this.game = game;
			this.letter = letter;
			this.path = game.path.resolve(letter);
			this.voices = 0;
		}

		public void add(Voice voice) {
			if (pages.isEmpty()) pages.add(new Page(this, pages.size() + 1));
			Page page = pages.get(pages.size() - 1);
			if (page.voices.size() == Templates.PAGE_SIZE) {
				page = new Page(this, pages.size() + 1);
				pages.add(page);
			}

			page.add(voice);
			this.voices++;
		}
	}

	public class Page {

		public final LetterGroup letter;
		public final int number;
		public final Path path;
		public final List<VoiceInfo> voices = new ArrayList<>();

		public Page(LetterGroup letter, int number) {
			this.letter = letter;
			this.number = number;
			this.path = letter.path.resolve(Integer.toString(number));
		}

		public void add(Voice voice) {
			this.voices.add(new VoiceInfo(this, voice));
			Collections.sort(voices);
		}
	}

	public class VoiceInfo implements Comparable<VoiceInfo> {

		public final Page page;
		public final Voice voice;
		public final String slug;
		public final Path path;

		public final Collection<VoiceInfo> variations;
		public final java.util.Map<String, Integer> alsoIn;

		public VoiceInfo(Page page, Voice voice) {
			this.page = page;
			this.voice = voice;
			this.slug = slug(voice.name + "_" + voice.hash.substring(0, 8));

			this.path = voice.slugPath(siteRoot);

			this.alsoIn = new HashMap<>();
			for (Content.ContentFile f : voice.files) {
				Collection<Content> containing = content.containingFile(f.hash);
				if (containing.size() > 1) {
					alsoIn.put(f.hash, containing.size() - 1);
				}
			}

			this.variations = content.variationsOf(voice.hash).stream()
									 .filter(p -> p instanceof Voice)
									 .map(p -> new VoiceInfo(page, (Voice)p))
									 .sorted()
									 .collect(Collectors.toList());

			Collections.sort(this.voice.downloads);
			Collections.sort(this.voice.files);
		}

		@Override
		public int compareTo(VoiceInfo o) {
			return voice.name.toLowerCase().compareTo(o.voice.name.toLowerCase());
		}
	}

}
