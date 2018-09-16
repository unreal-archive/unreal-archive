package net.shrimpworks.unreal.archive.maps;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import javax.imageio.ImageIO;

import net.shrimpworks.unreal.archive.Content;
import net.shrimpworks.unreal.archive.ContentFile;
import net.shrimpworks.unreal.archive.ContentIndexer;
import net.shrimpworks.unreal.archive.Download;
import net.shrimpworks.unreal.archive.Incoming;
import net.shrimpworks.unreal.archive.IndexLog;
import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.Umod;
import net.shrimpworks.unreal.packages.entities.ExportedObject;
import net.shrimpworks.unreal.packages.entities.Import;
import net.shrimpworks.unreal.packages.entities.Named;
import net.shrimpworks.unreal.packages.entities.ObjectReference;
import net.shrimpworks.unreal.packages.entities.objects.Object;
import net.shrimpworks.unreal.packages.entities.objects.Texture;
import net.shrimpworks.unreal.packages.entities.properties.IntegerProperty;
import net.shrimpworks.unreal.packages.entities.properties.ObjectProperty;
import net.shrimpworks.unreal.packages.entities.properties.Property;
import net.shrimpworks.unreal.packages.entities.properties.StringProperty;

public class MapIndexer implements ContentIndexer<Map> {

	public static class MapIndexerFactory implements IndexerFactory<Map> {

		@Override
		public ContentIndexer<Map> get() {
			return new MapIndexer();
		}
	}

	@Override
	public void index(Incoming incoming, IndexLog log, Consumer<Map> completed) {
		// FIXME pass in existing
		Map m = new Map();

		m.sha1 = incoming.originalSha1;
		m.releaseDate = "Unknown";

		try {
			m.fileSize = (int)Files.size(incoming.submission.filePath);
		} catch (IOException e) {
			log.log(IndexLog.EntryType.CONTINUE, "Failed to read file size of package " + incoming.submission.filePath);
		}

		m.files = new ArrayList<>();
		m.otherFiles = 0;
		for (java.util.Map.Entry<String, java.lang.Object> e : incoming.files.entrySet()) {
			if (!KNOWN_FILES.contains(Util.extension(e.getKey()))) {
				m.otherFiles++;
				continue;
			}

			try {
				if (e.getValue() instanceof Path) {
					m.files.add(new ContentFile(
							Util.fileName(e.getKey()),
							(int)Files.size((Path)e.getValue()),
							Util.sha1((Path)e.getValue())
					));

					// take a guess at release date based on file modification time
					if (m.releaseDate.equals("Unknown")) {
						m.releaseDate = Content.RELEASE_DATE_FMT.format(Files.getLastModifiedTime((Path)e.getValue()).toInstant());
					}

				} else if (e.getValue() instanceof Umod.UmodFile) {
					m.files.add(new ContentFile(
							Util.fileName(((Umod.UmodFile)e.getValue()).name),
							((Umod.UmodFile)e.getValue()).size,
							((Umod.UmodFile)e.getValue()).sha1()
					));
				}
			} catch (Exception ex) {
				log.log(IndexLog.EntryType.CONTINUE, "Failed getting data for " + e.getKey(), ex);
			}
		}

		m.downloads = new ArrayList<>();
		if (incoming.submission.sourceUrls != null) {
			for (String sourceUrl : incoming.submission.sourceUrls) {
				m.downloads.add(new Download(sourceUrl, LocalDate.now(), false));
			}
		}

		// populate basic information; the rest of this will be filled in later if possible
		m.name = mapName(incoming);
		m.game = game(incoming);
		m.gametype = gameType(incoming, m.name);
		m.title = m.name;
		m.author = "Unknown";
		m.description = "None";
		m.playerCount = "Unknown";

		try (Package map = map(incoming)) {
			if (map.version <= 68) m.game = "Unreal";
			else if (map.version < 117) m.game = "Unreal Tournament";
			else m.game = "Unreal Tournament 2004";

			// read level info (also in LevelSummary, but missing Screenshot)
			ExportedObject levelInfo = map.objectsByClassName("LevelInfo").iterator().next();

			if (levelInfo == null) throw new IllegalStateException("No LevelInfo in the map?!");

			Object level = levelInfo.object();

			// read some basic level info
			Property author = level.property("Author");
			Property title = level.property("Title");
			Property description = level.property("Description");

			if (author != null) m.author = ((StringProperty)author).value;
			if (title != null) m.title = ((StringProperty)title).value;
			if (description != null) m.description = ((StringProperty)description).value;

			if (map.version < 117) {
				Property idealPlayerCount = level.property("IdealPlayerCount");
				if (idealPlayerCount != null) m.playerCount = ((StringProperty)idealPlayerCount).value;
			} else {
				Property idealPlayerCountMin = level.property("IdealPlayerCountMin");
				Property idealPlayerCountMax = level.property("IdealPlayerCountMax");
				int min = 0;
				int max = 0;
				if (idealPlayerCountMin != null) min = ((IntegerProperty)idealPlayerCountMin).value;
				if (idealPlayerCountMax != null) max = ((IntegerProperty)idealPlayerCountMax).value;

				if (min == max && max > 0) m.playerCount = Integer.toString(max);
				else if (min > 0 && max > 0) m.playerCount = min + "-" + max;
				else if (min > 0 || max > 0) m.playerCount = Integer.toString(Math.max(min, max));
			}

			m.screenshots = new ArrayList<>();

			Property screenshot = level.property("Screenshot");

			// use this opportunity to resolve some version overlap between game versions
			if (screenshot != null && map.version < 117) m.game = "Unreal Tournament";

			List<BufferedImage> screenshots = screenshots(incoming, map, screenshot);
			for (int i = 0; i < screenshots.size(); i++) {
				Path out = Paths.get(String.format("/tmp/%s_shot_%d.png", m.name.replaceAll(" ", "_"), i + 1));
				ImageIO.write(screenshots.get(i), "png", out.toFile());
				m.screenshots.add(out.getFileName().toString());
			}

		} catch (IllegalStateException | IllegalArgumentException | UnsupportedOperationException e) {
			log.log(IndexLog.EntryType.CONTINUE, e.getMessage(), e);
		} catch (IOException e) {
			log.log(IndexLog.EntryType.CONTINUE, "Failed to read map package", e);
		}

		completed.accept(m);
	}

	private Package map(Incoming incoming) throws IOException {
		for (java.util.Map.Entry<String, java.lang.Object> kv : incoming.files.entrySet()) {
			if (kv.getKey().toLowerCase().endsWith(".unr") || kv.getKey().toLowerCase().endsWith(".ut2")) {
				if (kv.getValue() instanceof Path) {
					return new Package((Path)kv.getValue());
				} else if (kv.getValue() instanceof Umod.UmodFile) {
					return new Package(new PackageReader(((Umod.UmodFile)kv.getValue()).read()));
				}
			}
		}
		throw new IllegalStateException("Failed to find a map file...");
	}

	private String mapName(Incoming incoming) {
		String name = incoming.submission.filePath.getFileName().toString();
		for (String k : incoming.files.keySet()) {
			if (k.toLowerCase().endsWith(".unr") || k.toLowerCase().endsWith(".ut2")) {
				name = k.substring(Math.max(0, k.lastIndexOf("/") + 1));
				break;
			}
		}

		return name.substring(0, name.lastIndexOf(".")).replaceAll("/", "");
	}

	private String gameType(Incoming incoming, String name) {
		if (name.toLowerCase().startsWith("dm")) return "Deathmatch";
		if (name.toLowerCase().startsWith("ctf")) return "Capture The Flag";
		if (name.toLowerCase().startsWith("dom")) return "Domination";
		if (name.toLowerCase().startsWith("as")) return "Assault";
		if (name.toLowerCase().startsWith("vctf")) return "Vehicle Capture The Flag";
		return "Unknown";
	}

	private String game(Incoming incoming) {
		for (String k : incoming.files.keySet()) {
			if (k.toLowerCase().endsWith(".unr")) return "Unreal Tournament";
			if (k.toLowerCase().endsWith(".ut2")) return "Unreal Tournament 2004";
		}
		return "Unknown";
	}

	private List<BufferedImage> screenshots(Incoming incoming, Package map, Property screenshot) {
		List<BufferedImage> images = new ArrayList<>();
		if (screenshot != null) {
			ObjectReference shotRef = ((ObjectProperty)screenshot).value;
			Named shotResolved = shotRef.get();

			Package shotPackage = map;

			try {

				Object object = null;

				if (shotResolved instanceof Import) {
					// sigh... its stored in another package
					Named pkg = ((Import)shotResolved).packageName.get();
					try {
						shotPackage = findPackage(incoming, pkg.name().name);
						ExportedObject exp = shotPackage.objectByName(((Import)shotResolved).name);
						object = exp.object();
					} catch (IOException e) {
						// oh well, no screenshots
					}
				} else {
					ExportedObject exp = map.objectByRef(shotRef);
					object = exp.object();
				}

				if (object != null) {
					// get a texture form a UT2003/4 material sequence (they cycle several images in the map preview)
					if (object.className().equals("MaterialSequence")) {
						Property fallbackMaterial = object.property("FallbackMaterial");
						if (fallbackMaterial != null) {
							ExportedObject fallback = shotPackage.objectByRef(((ObjectProperty)fallbackMaterial).value);
							Object fallbackObj = fallback.object();
							if (fallbackObj instanceof Texture) {
								object = fallbackObj;
							}
						} else {
							// just find some textures that look like screenshots
							Collection<ExportedObject> textures = shotPackage.objectsByClassName("Texture");
							for (ExportedObject texture : textures) {
								// FIXME could be smarter, perhaps look for 512x256
								if (texture.name.name.toLowerCase().contains("shot")
									|| texture.name.name.toLowerCase().contains("screen")) {
									object = texture.object();
									break;
								}
							}
						}
					}

					Texture.MipMap[] mipMaps = ((Texture)object).mipMaps();
					BufferedImage bufferedImage = mipMaps[0].get();
					images.add(bufferedImage);
				}
			} finally {
				// cleanup if we spun up an external package for screenshots
				if (shotPackage != map) {
					try {
						shotPackage.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return images;
	}

	private Package findPackage(Incoming incoming, String pkg) throws IOException {
		for (java.util.Map.Entry<String, java.lang.Object> kv : incoming.files.entrySet()) {
			String name = kv.getKey().substring(Math.max(0, kv.getKey().lastIndexOf("/") + 1));
			name = name.substring(0, name.lastIndexOf(".") - 1);
			if (name.equalsIgnoreCase(pkg)) {
				if (kv.getValue() instanceof Path) {
					return new Package((Path)kv.getValue());
				} else if (kv.getValue() instanceof Umod.UmodFile) {
					return new Package(new PackageReader(((Umod.UmodFile)kv.getValue()).read()));
				}
			}
		}
		throw new IllegalStateException("Failed to find package");
	}
}
