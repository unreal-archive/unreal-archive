package net.shrimpworks.unreal.archive.indexing;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.imageio.ImageIO;

import net.shrimpworks.unreal.archive.common.Platform;
import net.shrimpworks.unreal.archive.common.Util;
import net.shrimpworks.unreal.archive.common.YAML;
import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.FileType;
import net.shrimpworks.unreal.archive.content.GameTypeRepository;
import net.shrimpworks.unreal.archive.content.Games;
import net.shrimpworks.unreal.archive.content.NameDescription;
import net.shrimpworks.unreal.archive.content.gametypes.GameType;
import net.shrimpworks.unreal.archive.indexing.mutators.MutatorClassifier;
import net.shrimpworks.unreal.archive.indexing.mutators.MutatorIndexHandler;
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

	private final GameTypeRepository repo;

	private final DataStore contentStore;
	private final DataStore imageStore;

	public GameTypeManager(GameTypeRepository repo, DataStore contentStore, DataStore imageStore) {
		this.repo = repo;
		this.contentStore = contentStore;
		this.imageStore = imageStore;
	}

	public GameType checkout(GameType gameType) {
		try {
			return YAML.fromString(YAML.toString(gameType), GameType.class);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot clone GameType " + gameType.name());
		}
	}

	public void checkin(GameType gameType) {
		try {
			repo.put(gameType);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot store GameType " + gameType.name());
		}
	}

	public void addRelease(Games game, String gameType, String releaseName, Path localFile, Map<String, String> params,
						   BiConsumer<GameType, GameType.Release> complete)
		throws IOException {
		GameType gt = Optional.ofNullable(repo.findGametype(game, gameType))
							  .or(() -> {
								  repo.create(game, gameType, (created) -> {
									  // we clear all these things, to make a somewhat cleaner "just ok" gametype definition
									  created.releases.clear();
									  created.links.clear();
									  created.credits.clear();
									  created.description = "";
									  created.titleImage = "";
									  created.bannerImage = "";
								  });
								  return Optional.ofNullable(repo.findGametype(game, gameType));
							  }).orElseThrow();

		GameType.Release rel = gt.releases.stream()
										  .filter(r -> r.title.equalsIgnoreCase(releaseName))
										  .findFirst()
										  .orElseGet(() -> {
											  GameType.Release release = new GameType.Release();
											  release.title = releaseName;
											  release.version = params.getOrDefault("version", releaseName);
											  release.releaseDate = params.getOrDefault("releaseDate", release.releaseDate);
											  release.description = params.getOrDefault("description", release.description);
											  gt.releases.add(release);
											  return release;
										  });

		GameType.ReleaseFile file = new GameType.ReleaseFile();
		file.localFile = localFile.toString();
		file.originalFilename = Util.fileName(localFile);
		file.platform = Platform.valueOf(params.getOrDefault("platform", file.platform.name()));
		file.title = params.getOrDefault("title", releaseName);
		rel.files.add(file);

		repo.put(gt);

		complete.accept(gt, rel);
	}

	public void sync() {
		syncReleases();
	}

	public void index(GameType gameType, GameType.Release release, GameType.ReleaseFile releaseFile) {
		// clone things... hmm
		GameType clone = checkout(gameType);
		GameType.Release releaseClone = clone.releases.stream().filter(r -> r.equals(release)).findFirst().get();
		GameType.ReleaseFile releaseFileClone = releaseClone.files.stream().filter(f -> f.equals(releaseFile)).findFirst().get();

		indexReleases(clone, releaseFileClone, imageStore);

		checkin(clone);
	}

	public void syncReleases() {
		repo.all().stream()
			.filter(g -> g.releases.stream()
								   .flatMap(m -> m.files.stream())
								   .anyMatch(r -> !r.synced))
			.forEach(this::syncReleases);
	}

	private void syncReleases(GameType gameType) {
		System.out.println("Syncing gametype: " + gameType.name());

		GameType clone = checkout(gameType);

		boolean[] success = { false };

		clone.releases.stream().flatMap(r -> r.files.stream()).filter(f -> !f.synced).forEach(r -> {
			Path f = Paths.get(r.localFile);
			syncReleaseFile(clone, r, f, success);
		});

		if (success[0]) checkin(clone);
	}

	public void syncReleaseFile(GameType gameType, GameType.ReleaseFile r, Path localFile, boolean[] success) {
		System.out.println(" - sync files for release " + r.title);
		if (!Files.exists(localFile)) throw new IllegalArgumentException(String.format("Local file %s not found!", localFile));

		// populate files and dependencies
		if (!r.synced) {
			try (Incoming incoming = new Incoming(new Submission(localFile))) {
				System.out.println(" - get file details for " + localFile.getFileName());

				// reuse Incoming implementation, capable of unpacking various files and formats
				incoming.prepare();

				// gather files
				r.otherFiles = 0;
				r.files.clear();
				for (Incoming.IncomingFile i : incoming.files(FileType.ALL)) {
					if (!FileType.important(i.file)) {
						r.otherFiles++;
						continue;
					}

					r.files.add(new Content.ContentFile(i.fileName(), i.fileSize(), i.hash()));
				}

				// compute dependencies
				r.dependencies = IndexUtils.dependencies(Games.byName(gameType.game), incoming);
			} catch (Exception e) {
				System.err.printf("Could not read files and dependencies for release file %s%n", localFile);
			}
		}

		try {
			System.out.println(" - storing file " + localFile.getFileName());

			// store file
			storeReleaseFile(gameType, r, localFile, success);
		} catch (IOException e) {
			throw new RuntimeException(String.format("Failed to sync file %s: %s%n", r.localFile, e));
		}
	}

	private void storeReleaseFile(GameType gameType, GameType.ReleaseFile releaseFile, Path localFile, boolean[] success)
		throws IOException {
		contentStore.store(localFile, String.join("/", remotePath(gameType), localFile.getFileName().toString()), (url, ex) -> {
			System.out.println(" - stored as " + url);

			try {
				// record download
				if (releaseFile.downloads.stream().noneMatch(dl -> dl.url.equals(url))) {
					releaseFile.downloads.add(new Content.Download(url, !releaseFile.synced, false, Content.DownloadState.OK));
				}

				// other file stats
				if (!releaseFile.synced) {
					releaseFile.fileSize = Files.size(localFile);
					releaseFile.hash = Util.hash(localFile);
					releaseFile.originalFilename = Util.fileName(localFile);
					releaseFile.synced = true;
				}

				success[0] = true;
			} catch (IOException e) {
				throw new RuntimeException(String.format("Failed to update gametype definition %s: %s%n", gameType.name(), e));
			}
		});
	}

	private void indexReleases(GameType gameType, GameType.ReleaseFile releaseFile, DataStore imagesStore) {

		Path[] f = { Paths.get(releaseFile.localFile) };
		if (!Files.exists(f[0])) {
			System.out.printf("Downloading %s (%dKB)%n", releaseFile.originalFilename, releaseFile.fileSize / 1024);
			try {
				f[0] = Util.downloadTo(releaseFile.mainDownload().url,
									   Files.createTempDirectory("ua-gametype").resolve(releaseFile.originalFilename));
			} catch (Exception e) {
				throw new RuntimeException(String.format("Could not download file %s", releaseFile), e);
			}
		}

		try (Incoming incoming = new Incoming(new Submission(f[0]))) {
			// reuse Incoming implementation, capable of unpacking various files and formats
			incoming.prepare();

			// find gametypes
			gameType.gameTypes = findGameTypes(incoming);

			// find mutators
			gameType.mutators = findMutators(incoming);

			// find maps
			gameType.maps = findMaps(incoming, gameType, imagesStore);

		} catch (IOException e) {
			System.err.printf("Could not read files and dependencies for release file %s%n", f[0]);
			e.printStackTrace();
		}

	}

	/**
	 * Find gametype definitions within .int (UT99) and .ucl (UT2004) files.
	 *
	 * @param incoming files to search for game types
	 * @return list of found game types
	 */
	private List<NameDescription> findGameTypes(Incoming incoming) {
		Set<Incoming.IncomingFile> uclFiles = incoming.files(FileType.UCL);
		Set<Incoming.IncomingFile> intFiles = incoming.files(FileType.INT);

		if (!uclFiles.isEmpty()) {
			// find mutator information via .ucl files (mutator names, descriptions, weapons and vehicles)
			return IndexUtils.readIntFiles(incoming, uclFiles, true)
							 .filter(Objects::nonNull)
							 .flatMap(intFile -> {
								 IntFile.Section section = intFile.section("root");
								 if (section == null) return null;
								 IntFile.ListValue game = section.asList("Game");
								 return game.values().stream();
							 })
							 .filter(Objects::nonNull)
							 .filter(v -> v instanceof IntFile.MapValue && ((IntFile.MapValue)v).value().containsKey("FallbackName"))
							 .map(v -> (IntFile.MapValue)v)
							 .map(mapVal -> new NameDescription(mapVal.get("FallbackName"), mapVal.getOrDefault("FallbackDesc", "")))
							 .sorted(Comparator.comparing(a -> a.name))
							 .toList();
		} else if (!intFiles.isEmpty()) {
			// search int files for objects describing a mutator and related things
			return IndexUtils.readIntFiles(incoming, intFiles)
							 .filter(Objects::nonNull)
							 .flatMap(intFile -> {
								 IntFile.Section section = intFile.section("public");
								 if (section == null) return null;
								 IntFile.ListValue prefs = section.asList("Preferences");
								 return prefs.values().stream();
							 })
							 .filter(Objects::nonNull)
							 .filter(v -> v instanceof IntFile.MapValue
										  && ((IntFile.MapValue)v).value().containsKey("Caption")
										  && ((IntFile.MapValue)v).value().containsKey("Parent"))
							 .map(v -> (IntFile.MapValue)v)
							 .filter(mapVal -> mapVal.get("Parent").equalsIgnoreCase("Game Types"))
							 .map(mapVal -> new NameDescription(mapVal.get("Caption")))
							 .sorted(Comparator.comparing(a -> a.name))
							 .toList();
		}

		return List.of();
	}

	/**
	 * Find mutator definitions within .int (UT99) and .ucl (UT2004) files.
	 * <p>
	 * This only returns a small set of information, borrowed from the full
	 * implementation in {@link MutatorIndexHandler}.
	 *
	 * @param incoming files to search for mutators
	 * @return list of found mutators
	 */
	private List<NameDescription> findMutators(Incoming incoming) {
		Set<Incoming.IncomingFile> uclFiles = incoming.files(FileType.UCL);
		Set<Incoming.IncomingFile> intFiles = incoming.files(FileType.INT);
		Set<Incoming.IncomingFile> iniFiles = incoming.files(FileType.INI);

		// FIXME use mutator indexer
		if (!uclFiles.isEmpty()) {
			// find mutator information via .ucl files (mutator names, descriptions, weapons and vehicles)
			return IndexUtils.readIntFiles(incoming, uclFiles, true)
							 .filter(Objects::nonNull)
							 .flatMap(intFile -> {
								 IntFile.Section section = intFile.section("root");
								 if (section == null) return null;
								 IntFile.ListValue mutator = section.asList("Mutator");
								 return mutator.values().stream();
							 })
							 .filter(Objects::nonNull)
							 .filter(v -> v instanceof IntFile.MapValue && ((IntFile.MapValue)v).value().containsKey("FallbackName"))
							 .map(v -> (IntFile.MapValue)v)
							 .map(mapVal -> new NameDescription(mapVal.get("FallbackName"), mapVal.getOrDefault("FallbackDesc", "")))
							 .sorted(Comparator.comparing(a -> a.name))
							 .toList();
		} else if (!intFiles.isEmpty()) {
			// search int files for objects describing a mutator and related things
			return IndexUtils.readIntFiles(incoming, intFiles)
							 .filter(Objects::nonNull)
							 .flatMap(intFile -> {
								 IntFile.Section section = intFile.section("public");
								 if (section == null) return null;
								 IntFile.ListValue prefs = section.asList("Object");
								 return prefs.values().stream();
							 })
							 .filter(Objects::nonNull)
							 .filter(v -> v instanceof IntFile.MapValue && ((IntFile.MapValue)v).value().containsKey("MetaClass"))
							 .map(v -> (IntFile.MapValue)v)
							 .filter(mapVal -> MutatorClassifier.UT_MUTATOR_CLASS.equalsIgnoreCase(mapVal.get("MetaClass")))
							 .map(mapVal -> new NameDescription(mapVal.get("Description")))
							 .sorted(Comparator.comparing(a -> a.name))
							 .toList();
		} else if (!iniFiles.isEmpty()) {
			return IndexUtils.readIntFiles(incoming, iniFiles)
							 .filter(Objects::nonNull)
							 .flatMap(iniFile -> iniFile.sections().stream().map(name -> {
								 IntFile.Section section = iniFile.section(name);
								 if (section == null) return null;
								 if (name.toLowerCase().endsWith(MutatorClassifier.UT3_MUTATOR_SECTION.toLowerCase())) {
									 // add mutator
									 return MutatorIndexHandler.sectionToNameDesc(section, "Unknown Mutator");
								 }
								 return null;
							 }))
							 .filter(Objects::nonNull)
							 .sorted(Comparator.comparing(a -> a.name))
							 .toList();
		}

		return List.of();
	}

	private List<GameType.GameTypeMap> findMaps(Incoming incoming, GameType gameType, DataStore imageStore) {
		Set<Incoming.IncomingFile> mapFiles = incoming.files(FileType.MAP);

		class FileAndPackage {

			final Incoming.IncomingFile f;
			final Package p;

			public FileAndPackage(Incoming.IncomingFile f, Package p) {
				this.f = f;
				this.p = p;
			}
		}

		// FIXME use map indexer
		return mapFiles.stream()
					   .map(mf -> new FileAndPackage(mf, new Package(new PackageReader(mf.asChannel()))))
					   .map(fp -> {
						   final String mapName = Util.plainName(fp.f.file);
						   String title = "";
						   String author = "";
						   Content.Attachment[] attachment = { null };

						   Collection<ExportedObject> maybeLevelInfo = fp.p.objectsByClassName("LevelInfo");
						   if (maybeLevelInfo != null && !maybeLevelInfo.isEmpty()) {

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

							   try {
								   List<BufferedImage> screenshots = IndexUtils.screenshots(incoming, fp.p, screenshot);
								   if (!screenshots.isEmpty()) {
									   System.out.printf("Storing screenshot for map %s%n", fp.f.fileName());
									   Path imgPath = Files.createTempFile(Util.slug(mapName), ".png");
									   ImageIO.write(screenshots.get(0), "png", imgPath.toFile());

									   Path emptyPath = Paths.get("");
									   imageStore.store(
										   imgPath,
										   emptyPath
											   .relativize(gameType.contentPath(emptyPath)).resolve("maps")
											   .resolve(imgPath.getFileName().toString())
											   .toString(),
										   (url, ex) -> {
											   if (ex == null && url != null) {
												   attachment[0] = new Content.Attachment(
													   Content.AttachmentType.IMAGE, imgPath.getFileName().toString(), url
												   );
											   }
										   });
								   }
							   } catch (Exception e) {
								   System.err.printf("Failed to save screenshot for map %s%n", mapName);
								   e.printStackTrace();
							   }
						   }

						   if (author.isBlank()) author = "Unknown";
						   if (title.isBlank()) title = mapName;

						   return new GameType.GameTypeMap(mapName, title, author, attachment[0]);
					   })
					   .sorted(Comparator.comparing(a -> a.name))
					   .toList();
	}

	private String remotePath(GameType gametype) {
		return String.join("/", REMOTE_ROOT, gametype.game, Util.slug(gametype.name));
	}

}
