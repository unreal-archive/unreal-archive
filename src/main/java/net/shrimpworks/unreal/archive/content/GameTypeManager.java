package net.shrimpworks.unreal.archive.content;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.YAML;
import net.shrimpworks.unreal.archive.content.gametypes.GameType;
import net.shrimpworks.unreal.archive.storage.DataStore;

public class GameTypeManager {

	private static final String REMOTE_ROOT = "gametypes";
	private static final String DOCUMENT_FILE = "gametype.md";

	private final Path path;

	private final Set<GameTypeHolder> gameTypes;
	private final Map<String, Collection<GameType>> contentFileMap;

	public GameTypeManager(Path path) throws IOException {
		this.path = path;
		this.gameTypes = new HashSet<>();
		this.contentFileMap = new HashMap<>();

		try (Stream<Path> files = Files.walk(path).parallel().filter(file -> Util.extension(file).equalsIgnoreCase("yml"))) {
			files.forEach(file -> {
				try {
					GameType g = YAML.fromFile(file, GameType.class);
					gameTypes.add(new GameTypeHolder(file, g));

					// while reading this content, also index its individual files for later quick lookup
					g.releases.stream().flatMap(r -> r.files.stream()).flatMap(f -> f.files.stream()).forEach(f -> {
						Collection<GameType> fileSet = contentFileMap.computeIfAbsent(f.hash, h -> ConcurrentHashMap.newKeySet());
						fileSet.add(g);
					});
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	public int size() {
		return gameTypes.size();
	}

	public Set<GameType> all() {
		return gameTypes.stream().filter(g -> !g.gametype.deleted).map(g -> g.gametype).collect(Collectors.toSet());
	}

	public Path path(GameType gameType) {
		return gameTypes.stream().filter(g -> gameType.equals(g.gametype)).findFirst().get().path;
	}

	/**
	 * Get the raw text content of the associated description document as a channel.
	 *
	 * @param gameType gametype to retrieve document for
	 * @return document content
	 * @throws IOException failed to open the document
	 */
	public ReadableByteChannel document(GameType gameType) throws IOException {
		GameTypeHolder holder = getGameType(gameType);
		if (holder == null) return null;

		Path docPath = holder.path.resolveSibling(DOCUMENT_FILE);

		if (!Files.exists(docPath)) return null;

		return Files.newByteChannel(docPath, StandardOpenOption.READ);
	}

	public Path init(Games game, String gameType) throws IOException {
		// create path
		final Path path = Files.createDirectories(gameTypePath(game, gameType));
		final String neatName = Util.capitalWords(gameType);

		// create the basic definition
		GameType gt = new GameType();
		gt.addedDate = LocalDate.now();
		gt.game = game.name;
		gt.name = neatName;
		gt.author = neatName + " Team";
		gt.description = "Short description about " + neatName;
		gt.titleImage = "title.png";
		gt.links = Map.of("Homepage", "https://" + Util.slug(gameType) + ".com");
		gt.credits = Map.of("Programming", List.of("Joe 'Programmer' Soap"), "Maps", List.of("MapGuy"));

		GameType.Release release = new GameType.Release();
		release.title = neatName + " Release";
		release.version = "1.0";

		GameType.ReleaseFile file = new GameType.ReleaseFile();
		file.title = "Release Package";
		file.localFile = "/path/to/release.zip";

		release.files.add(file);
		gt.releases.add(release);

		Path yml = Util.safeFileName(path.resolve("gametype.yml"));
		Path md = Util.safeFileName(path.resolve(DOCUMENT_FILE));

		if (!Files.exists(yml)) {
			Files.write(yml, YAML.toString(gt).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
		}

		if (!Files.exists(md)) {
			Files.copy(GameType.class.getResourceAsStream("template.md"), md);
		}

		return path;
	}

	public Path gameTypePath(Games game, String gameType) {
		return path.resolve(game.name).resolve(Util.slug(gameType));
	}

	public void sync(ContentManager content, DataStore contentStore) throws IOException {
		syncReleases(contentStore);
	}

	private GameTypeHolder getGameType(GameType gameType) {
		return gameTypes.stream()
						.filter(g -> gameType.equals(g.gametype))
						.findFirst().orElseThrow(() -> new IllegalArgumentException("GameType was not found: " + gameType.name));
	}

	private void syncReleases(DataStore contentStore) {
		Set<GameTypeHolder> toSync = gameTypes.stream()
											  .filter(g -> g.gametype.releases.stream().flatMap(m -> m.files.stream())
																			  .anyMatch(r -> !r.synced))
											  .collect(Collectors.toSet());

		toSync.forEach(g -> {
			System.out.println("Syncing gametype: " + g.gametype.name);

			GameType clone;
			try {
				clone = YAML.fromString(YAML.toString(g.gametype), GameType.class);
			} catch (IOException e) {
				throw new IllegalStateException("Cannot clone gametype " + g.gametype);
			}

			boolean[] success = { false };

			clone.releases.stream().flatMap(r -> r.files.stream()).filter(f -> !f.synced).forEach(r -> {
				System.out.println(" - sync files for release " + r.title);
				Path f = g.path.resolve(r.localFile);
				if (!Files.exists(f)) throw new IllegalArgumentException(String.format("Local file %s not found!", r.localFile));

				// populate files and dependencies
				try {
					System.out.println(" - get file details for " + f.getFileName());

					// reuse Incoming implementation, capable of unpacking various files and formats
					Incoming incoming = new Incoming(new Submission(f));
					incoming.prepare();

					// gather files
					r.otherFiles = 0;
					r.files.clear();
					for (Incoming.IncomingFile i : incoming.files(Incoming.FileType.ALL)) {
						if (!Incoming.FileType.important(i.file)) {
							r.otherFiles++;
							continue;
						}

						r.files.add(new Content.ContentFile(i.fileName(), i.fileSize(), i.hash()));
					}

					// compute dependencies
					r.dependencies = IndexUtils.dependencies(Games.byName(g.gametype.game), incoming);
				} catch (IOException e) {
					System.err.printf("Could not read files and dependencies for release file %s%n", f);
				}

				try {
					System.out.println(" - storing file " + f.getFileName());

					// store file
					contentStore.store(f, String.join("/", remotePath(g.gametype), f.getFileName().toString()), (url, ex) -> {
						System.out.println(" - stored as " + url);

						try {
							// record download
							if (r.downloads.stream().noneMatch(dl -> dl.url.equals(url))) {
								r.downloads.add(new Content.Download(url, true, false, Content.DownloadState.OK));
							}

							// other file stats
							r.fileSize = Files.size(f);
							r.hash = Util.hash(f);
							r.originalFilename = Util.fileName(f);
							r.synced = true;

							// replace existing with updated
							Files.write(g.path, YAML.toString(clone).getBytes(StandardCharsets.UTF_8),
										StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

							success[0] = true;
						} catch (IOException e) {
							throw new RuntimeException(String.format("Failed to update managed content definition %s: %s%n",
																	 g.path, e.toString()));
						}

					});
				} catch (IOException e) {
					throw new RuntimeException(String.format("Failed to sync file %s: %s%n", r.localFile, e.toString()));
				}
			});

			if (success[0]) {
				gameTypes.remove(g);
				gameTypes.add(new GameTypeHolder(g.path, clone));
			}
		});
	}

	private String remotePath(GameType gametype) {
		return String.join("/", REMOTE_ROOT, gametype.game, Util.slug(gametype.name));
	}

	private static class GameTypeHolder {

		private final Path path;
		private final GameType gametype;

		public GameTypeHolder(Path path, GameType gametype) {
			this.path = path;
			this.gametype = gametype;
		}
	}
}
