package net.shrimpworks.unreal.archive.www.content;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.ContentManager;
import net.shrimpworks.unreal.archive.content.voices.Voice;
import net.shrimpworks.unreal.archive.www.Templates;

import static net.shrimpworks.unreal.archive.www.Templates.slug;

public class Voices extends ContentPageGenerator {

	private final Games games;

	public Voices(ContentManager content, Path output, Path staticRoot, boolean localImages) {
		super(content, output.resolve("voices"), staticRoot, localImages);

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
	public int generate() {
		int count = 0;
		try {
			Templates.template("content/voices/games.ftl")
					 .put("static", root.relativize(staticRoot))
					 .put("title", "Voices")
					 .put("games", games)
					 .put("siteRoot", root)
					 .write(root.resolve("index.html"));
			count++;

			for (java.util.Map.Entry<String, Game> g : games.games.entrySet()) {

				if (g.getValue().voices < Templates.PAGE_SIZE) {
					// we can output all maps on a single page
					List<VoiceInfo> all = g.getValue().letters.values().stream()
															  .flatMap(l -> l.pages.stream())
															  .flatMap(e -> e.voices.stream())
															  .sorted()
															  .collect(Collectors.toList());
					Templates.template("content/voices/listing_single.ftl")
							 .put("static", root.resolve(g.getValue().path).relativize(staticRoot))
							 .put("title", String.join(" / ", "Voices", g.getKey()))
							 .put("game", g.getValue())
							 .put("voices", all)
							 .put("siteRoot", root.resolve(g.getValue().path).relativize(root))
							 .write(root.resolve(g.getValue().path).resolve("index.html"));
					count++;

					// still generate all map pages
					for (VoiceInfo skin : all) {
						count += voicePage(skin);
					}

					continue;
				}

				for (java.util.Map.Entry<String, LetterGroup> l : g.getValue().letters.entrySet()) {

					for (Page p : l.getValue().pages) {
						Templates.template("content/voices/listing.ftl")
								 .put("static", root.resolve(p.path).relativize(staticRoot))
								 .put("title", String.join(" / ", "Voices", g.getKey()))
								 .put("page", p)
								 .put("root", p.path)
								 .put("siteRoot", root.resolve(p.path).relativize(root))
								 .write(root.resolve(p.path).resolve("index.html"));
						count++;

						for (VoiceInfo skin : p.voices) {
							count += voicePage(skin);
						}
					}

					// output first letter/page combo, with appropriate relative links
					Templates.template("content/voices/listing.ftl")
							 .put("static", root.resolve(l.getValue().path).relativize(staticRoot))
							 .put("title", String.join(" / ", "Voices", g.getKey()))
							 .put("page", l.getValue().pages.get(0))
							 .put("root", l.getValue().path)
							 .put("siteRoot", root.resolve(l.getValue().path).relativize(root))
							 .write(root.resolve(l.getValue().path).resolve("index.html"));
					count++;
				}

				// output first letter/page combo, with appropriate relative links
				Templates.template("content/voices/listing.ftl")
						 .put("static", root.resolve(g.getValue().path).relativize(staticRoot))
						 .put("title", String.join(" / ", "Voices", g.getKey()))
						 .put("page", g.getValue().letters.firstEntry().getValue().pages.get(0))
						 .put("root", g.getValue().path)
						 .put("siteRoot", root.resolve(g.getValue().path).relativize(root))
						 .write(root.resolve(g.getValue().path).resolve("index.html"));
				count++;
			}

		} catch (IOException e) {
			throw new RuntimeException("Failed to render page", e);
		}

		return count;
	}

	private int voicePage(VoiceInfo voice) throws IOException {
		localImages(voice.voice, root.resolve(voice.path).getParent());

		Templates.template("content/voices/voice.ftl")
				 .put("static", root.resolve(voice.path).getParent().relativize(staticRoot))
				 .put("title", String.join(" / ", "Voices", voice.page.letter.game.name, voice.voice.name))
				 .put("voice", voice)
				 .put("siteRoot", root.resolve(voice.path).getParent().relativize(root))
				 .write(root.resolve(voice.path + ".html"));

		int res = 1;

		// since variations are not top-level things, we need to generate them here
		for (VoiceInfo variation : voice.variations) {
			res += this.voicePage(variation);
		}

		return res;
	}

	public class Games {

		public final TreeMap<String, Game> games = new TreeMap<>();
	}

	public class Game {

		public final String name;
		public final String slug;
		public final String path;
		public final TreeMap<String, LetterGroup> letters = new TreeMap<>();
		public int voices;

		public Game(String name) {
			this.name = name;
			this.slug = slug(name);
			this.path = slug;
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
		public final String path;
		public final List<Page> pages = new ArrayList<>();
		public int voices;

		public LetterGroup(Game game, String letter) {
			this.game = game;
			this.letter = letter;
			this.path = String.join("/", game.path, letter);
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
		public final String path;
		public final List<VoiceInfo> voices = new ArrayList<>();

		public Page(LetterGroup letter, int number) {
			this.letter = letter;
			this.number = number;
			this.path = String.join("/", letter.path, Integer.toString(number));
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
		public final String path;

		public final Collection<VoiceInfo> variations;
		public final java.util.Map<String, Integer> alsoIn;

		public VoiceInfo(Page page, Voice voice) {
			this.page = page;
			this.voice = voice;
			this.slug = slug(voice.name + "_" + voice.hash.substring(0, 8));

			if (page != null) this.path = String.join("/", page.path, slug);
			else this.path = slug;

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
