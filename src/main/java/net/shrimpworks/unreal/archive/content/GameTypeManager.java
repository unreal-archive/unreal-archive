package net.shrimpworks.unreal.archive.content;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.YAML;
import net.shrimpworks.unreal.archive.content.gametypes.GameType;
import net.shrimpworks.unreal.archive.content.mutators.Mutator;
import net.shrimpworks.unreal.archive.storage.DataStore;
import net.shrimpworks.unreal.packages.IntFile;
import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.ExportedObject;
import net.shrimpworks.unreal.packages.entities.objects.Object;
import net.shrimpworks.unreal.packages.entities.properties.Property;
import net.shrimpworks.unreal.packages.entities.properties.StringProperty;

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

		scanPath(path, null);
	}

	private void scanPath(Path root, GameTypeHolder parent) throws IOException {
		try (Stream<Path> files = Files.find(root, 3, (file, attr) -> Util.extension(file).equalsIgnoreCase("yml")).parallel()) {
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

	public int size() {
		return gameTypes.size();
	}

	public Set<GameType> all() {
		return gameTypes.stream()
						.filter(g -> g.variationOf == null)
						.filter(g -> !g.gametype.deleted)
						.map(g -> g.gametype)
						.collect(Collectors.toSet());
	}

	public Set<GameType> variations(GameType gameType) {
		return gameTypes.stream()
						.filter(g -> g.variationOf != null && g.variationOf.gametype.equals(gameType))
						.filter(g -> !g.gametype.deleted)
						.map(g -> g.gametype)
						.collect(Collectors.toSet());
	}

	public Path path(GameType gameType) {
		return gameTypes.stream()
						.filter(g -> gameType.equals(g.gametype))
						.findFirst()
						.get().path;
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

	public void sync(DataStore contentStore) {
		syncReleases(contentStore);
	}

	public void index(DataStore imageStore, Games game, String gameType, String releaseFile) {
		indexReleases(game, gameType, releaseFile, imageStore);
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
				try (Incoming incoming = new Incoming(new Submission(f))) {
					System.out.println(" - get file details for " + f.getFileName());

					// reuse Incoming implementation, capable of unpacking various files and formats
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

	private void indexReleases(Games game, String gameType, String releaseFile, DataStore imagesStore) {
		gameTypes.stream()
				 .filter(g -> !g.gametype.deleted)
				 .filter(g -> g.gametype.game.equals(game.name) && g.gametype.name.equalsIgnoreCase(gameType))
				 .findFirst().ifPresentOrElse(g -> {

			GameType clone;
			try {
				clone = YAML.fromString(YAML.toString(g.gametype), GameType.class);
			} catch (IOException e) {
				throw new IllegalStateException("Cannot clone gametype " + g.gametype);
			}

			clone.releases.stream()
						  .filter(r -> !r.deleted && !r.files.isEmpty())
						  .flatMap(r -> r.files.stream())
						  .filter(r -> !r.deleted && r.originalFilename.equalsIgnoreCase(releaseFile))
						  .findFirst().ifPresentOrElse(r -> {
				Path[] f = { Paths.get(r.localFile) };
				if (!Files.exists(f[0])) {
					System.out.printf("Downloading %s (%dKB)%n", r.originalFilename, r.fileSize / 1024);
					try {
						f[0] = Util.downloadTo(r.downloads.stream().filter(m -> m.main).map(m -> m.url).findFirst().get(),
											   Files.createTempDirectory("ua-gametype").resolve(r.originalFilename));
					} catch (Exception e) {
						throw new RuntimeException(String.format("Could not download file %s", releaseFile), e);
					}
				}

				try (Incoming incoming = new Incoming(new Submission(f[0]))) {
					// reuse Incoming implementation, capable of unpacking various files and formats
					incoming.prepare();

					// find gametypes
					clone.gameTypes = findGameTypes(incoming);

					// find mutators
					clone.mutators = findMutators(incoming);

					// find maps
					clone.maps = findMaps(incoming, clone, imagesStore);

					// replace existing with updated
					Files.write(g.path, YAML.toString(clone).getBytes(StandardCharsets.UTF_8),
								StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				} catch (IOException e) {
					System.err.printf("Could not read files and dependencies for release file %s%n", f);
				}

			}, () -> {
				throw new RuntimeException(String.format("Could not find release file %s", releaseFile));
			});

			gameTypes.remove(g);
			gameTypes.add(new GameTypeHolder(g.path, clone));
		}, () -> {
			throw new RuntimeException(String.format("Could not find game type %s", gameType));
		});
	}

	/**
	 * Find gametype definitions within .int (UT99) and .ucl (UT2004) files.
	 *
	 * @param incoming files to search for game types
	 * @return list of found game types
	 */
	private List<NameDescription> findGameTypes(Incoming incoming) {
		Set<Incoming.IncomingFile> uclFiles = incoming.files(Incoming.FileType.UCL);
		Set<Incoming.IncomingFile> intFiles = incoming.files(Incoming.FileType.INT);

		if (!uclFiles.isEmpty()) {
			// find mutator information via .ucl files (mutator names, descriptions, weapons and vehicles)
			return IndexUtils.readIntFiles(incoming, uclFiles, true)
							 .filter(Objects::nonNull)
							 .flatMap(intFile -> {
								 IntFile.Section section = intFile.section("root");
								 if (section == null) return null;
								 IntFile.ListValue game = section.asList("Game");
								 return game.values.stream();
							 })
							 .filter(Objects::nonNull)
							 .filter(v -> v instanceof IntFile.MapValue && ((IntFile.MapValue)v).value.containsKey("FallbackName"))
							 .map(v -> (IntFile.MapValue)v)
							 .map(mapVal -> new NameDescription(mapVal.get("FallbackName"), mapVal.getOrDefault("FallbackDesc", "")))
							 .sorted(Comparator.comparing(a -> a.name))
							 .collect(Collectors.toList());
		} else if (!intFiles.isEmpty()) {
			// search int files for objects describing a mutator and related things
			return IndexUtils.readIntFiles(incoming, intFiles)
							 .filter(Objects::nonNull)
							 .flatMap(intFile -> {
								 IntFile.Section section = intFile.section("public");
								 if (section == null) return null;
								 IntFile.ListValue prefs = section.asList("Preferences");
								 return prefs.values.stream();
							 })
							 .filter(Objects::nonNull)
							 .filter(v -> v instanceof IntFile.MapValue
										  && ((IntFile.MapValue)v).value.containsKey("Caption")
										  && ((IntFile.MapValue)v).value.containsKey("Parent"))
							 .map(v -> (IntFile.MapValue)v)
							 .filter(mapVal -> mapVal.get("Parent").equalsIgnoreCase("Game Types"))
							 .map(mapVal -> new NameDescription(mapVal.get("Caption")))
							 .sorted(Comparator.comparing(a -> a.name))
							 .collect(Collectors.toList());
		}

		return List.of();
	}

	/**
	 * Find mutator definitions within .int (UT99) and .ucl (UT2004) files.
	 * <p>
	 * This only returns a small set of information, borrowed from the full
	 * implementation in {@link net.shrimpworks.unreal.archive.content.mutators.MutatorIndexHandler}.
	 *
	 * @param incoming files to search for mutators
	 * @return list of found mutators
	 */
	private List<NameDescription> findMutators(Incoming incoming) {
		Set<Incoming.IncomingFile> uclFiles = incoming.files(Incoming.FileType.UCL);
		Set<Incoming.IncomingFile> intFiles = incoming.files(Incoming.FileType.INT);

		if (!uclFiles.isEmpty()) {
			// find mutator information via .ucl files (mutator names, descriptions, weapons and vehicles)
			return IndexUtils.readIntFiles(incoming, uclFiles, true)
							 .filter(Objects::nonNull)
							 .flatMap(intFile -> {
								 IntFile.Section section = intFile.section("root");
								 if (section == null) return null;
								 IntFile.ListValue mutator = section.asList("Mutator");
								 return mutator.values.stream();
							 })
							 .filter(Objects::nonNull)
							 .filter(v -> v instanceof IntFile.MapValue && ((IntFile.MapValue)v).value.containsKey("FallbackName"))
							 .map(v -> (IntFile.MapValue)v)
							 .map(mapVal -> new NameDescription(mapVal.get("FallbackName"), mapVal.getOrDefault("FallbackDesc", "")))
							 .sorted(Comparator.comparing(a -> a.name))
							 .collect(Collectors.toList());
		} else if (!intFiles.isEmpty()) {
			// search int files for objects describing a mutator and related things
			return IndexUtils.readIntFiles(incoming, intFiles)
							 .filter(Objects::nonNull)
							 .flatMap(intFile -> {
								 IntFile.Section section = intFile.section("public");
								 if (section == null) return null;
								 IntFile.ListValue prefs = section.asList("Object");
								 return prefs.values.stream();
							 })
							 .filter(Objects::nonNull)
							 .filter(v -> v instanceof IntFile.MapValue && ((IntFile.MapValue)v).value.containsKey("MetaClass"))
							 .map(v -> (IntFile.MapValue)v)
							 .filter(mapVal -> Mutator.UT_MUTATOR_CLASS.equalsIgnoreCase(mapVal.get("MetaClass")))
							 .map(mapVal -> new NameDescription(mapVal.get("Description")))
							 .sorted(Comparator.comparing(a -> a.name))
							 .collect(Collectors.toList());
		}

		return List.of();
	}

	private List<GameType.GameTypeMap> findMaps(Incoming incoming, GameType gameType, DataStore imageStore) {
		Set<Incoming.IncomingFile> mapFiles = incoming.files(Incoming.FileType.MAP);

		class FileAndPackage {

			final Incoming.IncomingFile f;
			final Package p;

			public FileAndPackage(Incoming.IncomingFile f, Package p) {
				this.f = f;
				this.p = p;
			}
		}

		return mapFiles.stream()
					   .map(mf -> new FileAndPackage(mf, new Package(new PackageReader(mf.asChannel()))))
					   .map(fp -> {
						   final String mapName = Util.plainName(fp.f.file);
						   String title = "";
						   String author = "";

						   Collection<ExportedObject> maybeLevelInfo = fp.p.objectsByClassName("LevelInfo");
						   if (maybeLevelInfo == null || maybeLevelInfo.isEmpty()) {
							   return null;
						   }

						   // if there are multiple LevelInfos in a map, try to find the right one...
						   Object level = maybeLevelInfo.stream()
														.map(ExportedObject::object)
														.filter(l -> l.property("Title") != null || l.property("Author") != null)
														.findFirst()
														.orElse(maybeLevelInfo.iterator().next().object());

						   // read some basic level info
						   Property authorProp = level.property("Author");
						   Property titleProp = level.property("Title");
						   Property screenshot = level.property("Screenshot");

						   if (authorProp != null) author = ((StringProperty)authorProp).value.trim();
						   if (titleProp != null) title = ((StringProperty)titleProp).value.trim();

						   if (author.isBlank()) author = "Unknown";
						   if (title.isBlank()) title = mapName;

						   Content.Attachment[] attachment = { null };

						   try {
							   List<BufferedImage> screenshots = IndexUtils.screenshots(incoming, fp.p, screenshot);
							   if (!screenshots.isEmpty()) {
								   System.out.printf("Storing screenshot for map %s%n", fp.f.fileName());
								   Path imgPath = Files.createTempFile(Util.slug(mapName), ".png");
								   ImageIO.write(screenshots.get(0), "png", imgPath.toFile());

								   imageStore.store(imgPath,
													Paths.get("").relativize(gameType.contentPath(Paths.get(""))).resolve("maps").resolve(
															imgPath.getFileName().toString()).toString(),
													(url, ex) -> {
														if (ex == null && url != null) {
															attachment[0] = new Content.Attachment(Content.AttachmentType.IMAGE,
																								   imgPath.getFileName().toString(), url);
															System.out.printf("Stored as %s%n", url);
														}
													});
							   }
						   } catch (Exception e) {
							   System.err.printf("Failed to save screenshot for map %s%n", mapName);
							   e.printStackTrace();
						   }

						   return new GameType.GameTypeMap(mapName, title, author, attachment[0]);
					   })
					   .filter(Objects::nonNull)
					   .sorted(Comparator.comparing(a -> a.name))
					   .collect(Collectors.toList());
	}

	private String remotePath(GameType gametype) {
		return String.join("/", REMOTE_ROOT, gametype.game, Util.slug(gametype.name));
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

	}
}
