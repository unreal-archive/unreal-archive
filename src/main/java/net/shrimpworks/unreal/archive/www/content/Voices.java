package net.shrimpworks.unreal.archive.www.content;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.shrimpworks.unreal.archive.content.ContentRepository;
import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.Games;
import net.shrimpworks.unreal.archive.content.voices.Voice;
import net.shrimpworks.unreal.archive.www.SiteFeatures;
import net.shrimpworks.unreal.archive.www.SiteMap;
import net.shrimpworks.unreal.archive.www.Templates;

public class Voices extends GenericContentPage<Voice> {

	private static final String SECTION = "Voices";
	private static final String SUBGROUP = "all";

	public Voices(ContentRepository content, Path output, Path staticRoot, SiteFeatures localImages) {
		super(content, output, output.resolve("voices"), staticRoot, localImages);
	}

	@Override
	public Set<SiteMap.Page> generate() {
		GameList games = loadContent(Voice.class, content);

		Templates.PageSet pages = pageSet("content/voices");
		pages.add("games.ftl", SiteMap.Page.monthly(0.6f), SECTION)
			 .put("games", games)
			 .write(root.resolve("index.html"));

		games.games.entrySet().parallelStream().forEach(g -> {

			Map<Integer, Map<Integer, Integer>> timeline = timeline(g.getValue());

			Games game = Games.byName(g.getKey());

			if (g.getValue().count < Templates.PAGE_SIZE) {
				List<ContentInfo<Voice>> all = g.getValue().groups.get(SUBGROUP).letters.values().stream()
																						.flatMap(l -> l.pages.stream())
																						.flatMap(e -> e.items.stream())
																						.sorted()
																						.toList();
				pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName))
					 .put("game", g.getValue())
					 .put("timeline", timeline)
					 .put("voices", all)
					 .write(g.getValue().path.resolve("index.html"));

				// still generate all map pages
				all.parallelStream().forEach(voice -> voicePage(pages, voice));

				generateTimeline(pages, timeline, g.getValue(), SECTION);

				return;
			}

			g.getValue().groups.get(SUBGROUP).letters.entrySet().parallelStream().forEach(l -> {
				l.getValue().pages.parallelStream().forEach(p -> {
					pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName))
						 .put("timeline", timeline)
						 .put("page", p)
						 .write(p.path.resolve("index.html"));

					p.items.parallelStream().forEach(voice -> voicePage(pages, voice));
				});

				// output first letter/page combo, with appropriate relative links
				pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName))
					 .put("timeline", timeline)
					 .put("page", l.getValue().pages.get(0))
					 .write(l.getValue().path.resolve("index.html"));
			});

			// output first letter/page combo, with appropriate relative links
			pages.add("listing.ftl", SiteMap.Page.weekly(0.65f), String.join(" / ", SECTION, game.bigName))
				 .put("timeline", timeline)
				 .put("page", g.getValue().groups.get(SUBGROUP).letters.firstEntry().getValue().pages.get(0))
				 .write(g.getValue().path.resolve("index.html"));

			generateTimeline(pages, timeline, g.getValue(), SECTION);
		});

		return pages.pages;
	}

	private void voicePage(Templates.PageSet pages, ContentInfo<Voice> voice) {
		final Content item = voice.item();
		localImages(item, root.resolve(voice.path).getParent());

		pages.add("voice.ftl", SiteMap.Page.monthly(0.9f, item.firstIndex), String.join(" / ", SECTION,
																						voice.page.letter.group.game.game.bigName,
																						item.name))
			 .put("voice", voice)
			 .write(Paths.get(voice.path + ".html"));

		// since variations are not top-level things, we need to generate them here
		for (ContentInfo<Voice> variation : voice.variations) {
			voicePage(pages, variation);
		}
	}

	@Override
	protected String gameSubGroup(Voice item) {
		return SUBGROUP;
	}

}
