package net.shrimpworks.unreal.archive.www;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.indexer.Content;
import net.shrimpworks.unreal.archive.indexer.ContentManager;
import net.shrimpworks.unreal.archive.indexer.mappacks.MapPack;

import static net.shrimpworks.unreal.archive.www.Templates.slug;

public class MapPacks {

	private static final int PAGE_SIZE = 150;

	private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
	private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

	private final ContentManager content;
	private final Path output;
	private final Path staticRoot;

	private final Games games;

	public MapPacks(ContentManager content, Path output, Path staticRoot) {
		this.content = content;
		this.output = output;
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
			Path root = output.resolve("mappacks");

			Templates.template("mappacks/games.ftl")
					 .put("static", root.relativize(staticRoot))
					 .put("title", "Map Packs")
					 .put("games", games)
					 .write(root.resolve("games.html"));
			count++;

//			for (java.util.Map.Entry<String, Game> g : games.games.entrySet()) {
//				Templates.template("maps/gametypes.ftl")
//						 .put("static", root.resolve(g.getValue().path).relativize(staticRoot))
//						 .put("title", String.join(" / ", "Maps", g.getKey()))
//						 .put("game", g.getValue())
//						 .write(root.resolve(g.getValue().path).resolve("index.html"));
//				count++;
//			}

		} catch (IOException e) {
			throw new RuntimeException("Failed to render page", e);
		}

		return count;
	}

	public class Games {

		public final TreeMap<String, Game> games = new TreeMap<>();
	}

	public class Game {

		public final String name;
		public final String slug;
		public final String path;
		public final List<MapPackInfo> packs = new ArrayList<>();

		public Game(String name) {
			this.name = name;
			this.slug = slug(name);
			this.path = slug;
		}

		public void add(MapPack p) {
			this.packs.add(new MapPackInfo(this, p));
			Collections.sort(packs);
		}
	}

	public class MapPackInfo implements Comparable<MapPackInfo> {

		public final Game game;
		public final MapPack pack;
		public final String slug;
		public final String path;

		public final java.util.Map<String, Integer> alsoIn;

		public MapPackInfo(Game game, MapPack pack) {
			this.game = game;
			this.pack = pack;
			this.slug = slug(pack.name + "_" + pack.hash.substring(0, 8));

			if (game != null) this.path = String.join("/", game.path, slug);
			else this.path = slug;

			this.alsoIn = new HashMap<>();
			for (Content.ContentFile f : pack.files) {
				Collection<Content> containing = content.containing(f.hash);
				if (containing.size() > 1) {
					alsoIn.put(f.hash, containing.size() - 1);
				}
			}

			this.pack.downloads.sort((a, b) -> a.main ? -1 : 0);
		}

		@Override
		public int compareTo(MapPackInfo o) {
			return pack.name.compareTo(o.pack.name);
		}
	}

}
