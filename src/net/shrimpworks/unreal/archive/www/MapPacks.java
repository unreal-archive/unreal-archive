package net.shrimpworks.unreal.archive.www;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.indexer.Content;
import net.shrimpworks.unreal.archive.indexer.ContentManager;
import net.shrimpworks.unreal.archive.indexer.mappacks.MapPack;

import static net.shrimpworks.unreal.archive.www.Templates.slug;

public class MapPacks {

	private static final String SECTION = "Map Packs";

	private final ContentManager content;
	private final Path output;
	private final Path root;
	private final Path staticRoot;

	private final Games games;

	public MapPacks(ContentManager content, Path output, Path staticRoot) {
		this.content = content;
		this.output = output;
		this.root = output.resolve("mappacks");
		this.staticRoot = staticRoot;
		this.games = new Games();

		Collection<MapPack> packs = content.get(MapPack.class).stream()
										   .sorted(Comparator.comparing(a -> a.name.toLowerCase()))
										   .collect(Collectors.toList());
		for (MapPack p : packs) {
			Game g = games.games.computeIfAbsent(p.game, Game::new);
			g.add(p);
		}
	}

	public int generate() {
		int count = 0;
		try {

			Templates.template("mappacks/games.ftl")
					 .put("static", root.relativize(staticRoot))
					 .put("title", SECTION)
					 .put("games", games)
					 .put("siteRoot", root)
					 .write(root.resolve("index.html"));
			count++;

			for (Map.Entry<String, Game> g : games.games.entrySet()) {
				Templates.template("mappacks/gametypes.ftl")
						 .put("static", root.resolve(g.getValue().path).relativize(staticRoot))
						 .put("title", String.join(" / ", SECTION, g.getKey()))
						 .put("game", g.getValue())
						 .put("siteRoot", root.resolve(g.getValue().path).relativize(root))
						 .write(root.resolve(g.getValue().path).resolve("index.html"));
				count++;

				for (Map.Entry<String, Gametype> gt : g.getValue().gametypes.entrySet()) {

					if (gt.getValue().packs < Templates.PAGE_SIZE) {
						// we can output all maps on a single page
						List<MapPackInfo> all = gt.getValue().pages.stream()
																   .flatMap(p -> p.packs.stream())
																   .sorted()
																   .collect(Collectors.toList());
						Templates.template("mappacks/listing_single.ftl")
								 .put("static", root.resolve(gt.getValue().path).relativize(staticRoot))
								 .put("title", String.join(" / ", SECTION, g.getKey(), gt.getKey()))
								 .put("gametype", gt.getValue())
								 .put("packs", all)
								 .put("siteRoot", root.resolve(gt.getValue().path).relativize(root))
								 .write(root.resolve(gt.getValue().path).resolve("index.html"));
						count++;

						// still generate all map pages
						for (MapPackInfo pack : all) {
							packPage(pack);
							count++;
						}

						continue;
					}

					for (Page p : gt.getValue().pages) {
						Templates.template("mappacks/listing.ftl")
								 .put("static", root.resolve(p.path).relativize(staticRoot))
								 .put("title", String.join(" / ", SECTION, g.getKey(), gt.getKey()))
								 .put("page", p)
								 .put("root", p.path)
								 .put("siteRoot", root.resolve(p.path).relativize(root))
								 .write(root.resolve(p.path).resolve("index.html"));
						count++;

						for (MapPackInfo pack : p.packs) {
							packPage(pack);
							count++;
						}
					}

					// output first letter/page combo, with appropriate relative links
					Templates.template("mappacks/listing.ftl")
							 .put("static", root.resolve(gt.getValue().path).relativize(staticRoot))
							 .put("title", String.join(" / ", SECTION, g.getKey(), gt.getKey()))
							 .put("page", gt.getValue().pages.get(0))
							 .put("root", gt.getValue().path)
							 .put("siteRoot", root.resolve(gt.getValue().path).relativize(root))
							 .write(root.resolve(gt.getValue().path).resolve("index.html"));
					count++;

				}
			}

		} catch (IOException e) {
			throw new RuntimeException("Failed to render page", e);
		}

		return count;
	}

	private void packPage(MapPackInfo pack) throws IOException {
		Templates.template("mappacks/mappack.ftl")
				 .put("static", root.resolve(pack.path).getParent().relativize(staticRoot))
				 .put("title", String.join(" / ", SECTION, pack.page.gametype.game.name, pack.page.gametype.name, pack.pack.name))
				 .put("pack", pack)
				 .put("siteRoot", root.resolve(pack.path).getParent().relativize(root))
				 .write(root.resolve(pack.path + ".html"));
	}

	public class Games {

		public final TreeMap<String, Game> games = new TreeMap<>();
	}

	public class Game {

		public final String name;
		public final String slug;
		public final String path;
		public final TreeMap<String, Gametype> gametypes = new TreeMap<>();
		public int packs;

		public Game(String name) {
			this.name = name;
			this.slug = slug(name);
			this.path = slug;
		}

		public void add(MapPack p) {
			Gametype gametype = gametypes.computeIfAbsent(p.gametype, g -> new Gametype(this, g));
			gametype.add(p);
			this.packs++;
		}
	}

	public class Gametype {

		public final Game game;

		public final String name;
		public final String slug;
		public final String path;
		public final List<Page> pages = new ArrayList<>();
		public int packs;

		public Gametype(Game game, String name) {
			this.game = game;
			this.name = name;
			this.slug = slug(name);
			this.path = String.join("/", game.path, slug);
			this.packs = 0;
		}

		public void add(MapPack p) {
			if (pages.isEmpty()) pages.add(new Page(this, pages.size() + 1));
			Page page = pages.get(pages.size() - 1);
			if (page.packs.size() == Templates.PAGE_SIZE) {
				page = new Page(this, pages.size() + 1);
				pages.add(page);
			}

			page.add(p);
			this.packs++;
		}
	}

	public class Page {

		public final Gametype gametype;
		public final int number;
		public final String path;
		public final List<MapPackInfo> packs = new ArrayList<>();

		public Page(Gametype gametype, int number) {
			this.gametype = gametype;
			this.number = number;
			this.path = String.join("/", gametype.path, Integer.toString(number));
		}

		public void add(MapPack p) {
			this.packs.add(new MapPackInfo(this, p));
			Collections.sort(packs);
		}
	}

	public class MapPackInfo implements Comparable<MapPackInfo> {

		public final Page page;
		public final MapPack pack;
		public final String slug;
		public final String path;

		public final Collection<Content> variations;
		public final Map<String, Integer> alsoIn;

		public MapPackInfo(Page page, MapPack pack) {
			this.page = page;
			this.pack = pack;
			this.slug = slug(pack.name + "_" + pack.hash.substring(0, 8));

			if (page != null) this.path = String.join("/", page.path, slug);
			else this.path = slug;

			this.alsoIn = new HashMap<>();
			for (Content.ContentFile f : pack.files) {
				Collection<Content> containing = content.containingFile(f.hash);
				if (containing.size() > 1) {
					alsoIn.put(f.hash, containing.size() - 1);
				}
			}

			this.variations = content.variationsOf(pack.hash);

			this.pack.downloads.sort((a, b) -> a.main ? -1 : 0);
		}

		@Override
		public int compareTo(MapPackInfo o) {
			return pack.name.toLowerCase().compareTo(o.pack.name.toLowerCase());
		}
	}

}
