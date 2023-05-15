package org.unrealarchive.content.addons;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.unrealarchive.common.Util;
import org.unrealarchive.common.YAML;
import org.unrealarchive.content.Games;

public interface GameTypeRepository {

	public int size();

	public Set<GameType> all();

	public Set<GameType> variations(GameType gameType);

	public void create(Games game, String gameType, Consumer<GameType> completed);

	public void put(GameType gameType) throws IOException;

	/**
	 * Get the raw text content of the associated description document as a channel.
	 */
	public ReadableByteChannel document(GameType gameType) throws IOException;

	/**
	 * Copy assets (gallery, title images, etc) to the specified `outPath`
	 */
	public void writeContent(GameType gameType, Path outPath) throws IOException;

	public GameType findGametype(Games game, String gameType);

	public static class FileRepository implements GameTypeRepository {

		private static final String DOCUMENT_FILE = "gametype.md";
		private static final String DOCUMENT_TEMPLATE_FILE = "gametype-template.md";

		private final Path path;

		private final Set<GameTypeHolder> gameTypes;
		private final Map<String, Collection<GameType>> contentFileMap;

		public FileRepository(Path path) throws IOException {
			this.path = path;
			this.gameTypes = new HashSet<>();
			this.contentFileMap = new HashMap<>();

			scanPath(path, null);
		}

		private void scanPath(Path root, GameTypeHolder parent) throws IOException {
			try (Stream<Path> files = Files.find(root, 3, (file, attr) -> file.toString().endsWith(".yml")).parallel()) {
				files.forEach(file -> {
					try {
						GameType g = YAML.fromFile(file, GameType.class);
						GameTypeHolder holder = new GameTypeHolder(file, g, parent);
						gameTypes.add(holder);

						// while reading this content, also index its individual files for later quick lookup
						g.releases.stream().flatMap(r -> r.files.stream()).flatMap(f -> f.files.stream()).forEach(f -> {
							Collection<GameType> fileSet = contentFileMap.computeIfAbsent(f.hash, h -> ConcurrentHashMap.newKeySet());
							fileSet.add(g);
						});

						Path variations = file.resolveSibling("variations");
						if (Files.isDirectory(variations)) {
							scanPath(variations, holder);
						}
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
			}
		}

		@Override
		public int size() {
			return gameTypes.size();
		}

		@Override
		public Set<GameType> all() {
			return gameTypes.stream()
							.filter(g -> !g.gametype.deleted())
							.filter(g -> g.variationOf == null)
							.map(g -> g.gametype)
							.collect(Collectors.toSet());
		}

		@Override
		public Set<GameType> variations(GameType gameType) {
			return gameTypes.stream()
							.filter(g -> !g.gametype.deleted())
							.filter(g -> g.variationOf != null && g.variationOf.gametype.equals(gameType))
							.map(g -> g.gametype)
							.collect(Collectors.toSet());
		}

		@Override
		public void create(Games game, String gameType, Consumer<GameType> completed) {
			if (findGametype(game, gameType) != null) throw new IllegalStateException("Gametype already exists!");

			try {
				System.err.println("Initialising gametype in directory:");
				System.err.printf("  - %s%n", gameTypePath(game, gameType).toAbsolutePath());
				System.err.println("\nPopulate the appropriate files, add images, etc.");
				System.err.println("To upload gametype files, execute the `sync` command.");

				// create path
				final String neatName = Util.capitalWords(gameType);

				// create the basic definition
				GameType gt = new GameType();
				gt.addedDate = LocalDate.now();
				gt.game = game.name;
				gt.name = neatName;
				gt.author = neatName + " Team";
				gt.description = "Short description about " + neatName;
				gt.titleImage = "title.png";
				gt.links.put("Homepage", "https://" + Util.slug(gameType) + ".com");
				gt.credits.putAll(Map.of("Programming", List.of("Joe 'Programmer' Soap"), "Maps", List.of("MapGuy")));

				GameType.Release release = new GameType.Release();
				release.title = neatName + " Release";
				release.version = "1.0";

				GameType.ReleaseFile file = new GameType.ReleaseFile();
				file.title = "Release Package";
				file.localFile = "/path/to/release.zip";

				release.files.add(file);
				gt.releases.add(release);

				// we accept early, to allow mutations before writing
				completed.accept(gt);

				put(gt);

				// create initial gametype doc
				final Path docPath = Files.createDirectories(gameTypePath(game, gameType));
				Path md = Util.safeFileName(docPath.resolve(DOCUMENT_FILE));
				if (!Files.exists(md)) Files.copy(getClass().getResourceAsStream(DOCUMENT_TEMPLATE_FILE), md);
			} catch (IOException e) {
				throw new RuntimeException("Gametype creation failed", e);
			}
		}

		@Override
		public void put(GameType gameType) throws IOException {
			final Path path = Files.createDirectories(gameTypePath(Games.byName(gameType.game), gameType.name));
			Path yml = Util.safeFileName(path.resolve("gametype.yml"));
			Files.writeString(yml, YAML.toString(gameType), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

			// replace existing entry
			gameTypes.removeIf(gh -> gh.gametype.game.equalsIgnoreCase(gameType.game) && gh.gametype.name.equalsIgnoreCase(gameType.name));
			gameTypes.add(new GameTypeHolder(yml, gameType));
		}

		@Override
		public ReadableByteChannel document(GameType gameType) throws IOException {
			GameTypeHolder holder = getGameType(gameType);
			if (holder == null) return null;

			Path docPath = holder.path.resolveSibling(DOCUMENT_FILE);

			if (!Files.exists(docPath)) return null;

			return Files.newByteChannel(docPath, StandardOpenOption.READ);
		}

		@Override
		public void writeContent(GameType gameType, Path outPath) throws IOException {
			Path sourcePath = path(gameType);
			if (sourcePath == null) return;

			Util.copyTree(sourcePath.getParent(), outPath);
		}

		@Override
		public GameType findGametype(Games game, String gameType) {
			return gameTypes.stream()
							.filter(g -> !g.gametype.deleted())
							.filter(g -> g.gametype.game().equals(game.name) && g.gametype.name().equalsIgnoreCase(gameType))
							.findFirst()
							.map(h -> h.gametype)
							.orElse(null);
		}

		private GameTypeHolder getGameType(GameType gameType) {
			return gameTypes.stream()
							.filter(g -> gameType.equals(g.gametype))
							.findFirst().orElseThrow(() -> new IllegalArgumentException("GameType was not found: " + gameType.name()));
		}

		private Path gameTypePath(Games game, String gameType) {
			return path.resolve(game.name).resolve(Util.slug(gameType));
		}

		private Path path(GameType gameType) {
			return gameTypes.stream()
							.filter(g -> gameType.equals(g.gametype))
							.findFirst()
							.map(h -> h.path)
							.orElse(null);
		}

		private static class GameTypeHolder {

			private final Path path;
			private final GameType gametype;
			private final GameTypeHolder variationOf;

			public GameTypeHolder(Path path, GameType gametype) {
				this(path, gametype, null);
			}

			public GameTypeHolder(Path path, GameType gametype, GameTypeHolder variationOf) {
				this.path = path;
				this.gametype = gametype;
				this.variationOf = variationOf;
			}

			@Override
			public boolean equals(java.lang.Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;
				GameTypeHolder that = (GameTypeHolder)o;
				return Objects.equals(gametype, that.gametype);
			}

			@Override
			public int hashCode() {
				return Objects.hash(gametype);
			}
		}
	}
}
