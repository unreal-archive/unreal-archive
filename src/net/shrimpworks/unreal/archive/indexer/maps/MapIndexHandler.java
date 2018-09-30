package net.shrimpworks.unreal.archive.indexer.maps;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.indexer.Content;
import net.shrimpworks.unreal.archive.indexer.Incoming;
import net.shrimpworks.unreal.archive.indexer.IndexHandler;
import net.shrimpworks.unreal.archive.indexer.IndexLog;
import net.shrimpworks.unreal.archive.indexer.IndexResult;
import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
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

public class MapIndexHandler implements IndexHandler<Map> {

	private static final String SHOT_NAME = "%s_shot_%d.png";

	private static final Pattern SP_MATCH = Pattern.compile("(.+)?(single ?player|cooperative)([\\s:]+)?yes(\\s+)?", Pattern.CASE_INSENSITIVE);

	public static class MapIndexHandlerFactory implements IndexHandlerFactory<Map> {

		@Override
		public IndexHandler<Map> get() {
			return new MapIndexHandler();
		}
	}

	@Override
	public void index(Incoming incoming, Content content, Consumer<IndexResult<Map>> completed) {
		IndexLog log = incoming.log;
		Map m = (Map)content;

		// TODO find .txt file in content root and scan for dates, authors, etc

		// populate basic information; the rest of this will be filled in later if possible
		m.name = mapName(incoming);
		m.game = game(incoming);
		m.gametype = gameType(incoming, m.name);
		m.title = m.name;

		Set<IndexResult.NewAttachment> attachments = new HashSet<>();

		try (Package map = map(incoming)) {
			// attempt to detect Unreal maps by possible release date
			if (map.version < 68 || (m.releaseDate != null && m.releaseDate.compareTo(RELEASE_UT99) < 0)) m.game = "Unreal";
			// Unreal does not contain a LevelSummary
			if (map.version == 68 && map.objectsByClassName("LevelSummary").isEmpty()) m.game = "Unreal";

			// read level info (also in LevelSummary, but missing Screenshot)
			Collection<ExportedObject> maybeLevelInfo = map.objectsByClassName("LevelInfo");
			if (maybeLevelInfo == null || maybeLevelInfo.isEmpty()) {
				throw new IllegalArgumentException("Could not find LevelInfo in map");
			}
			ExportedObject levelInfo = maybeLevelInfo.iterator().next();

			if (levelInfo == null) throw new IllegalStateException("No LevelInfo in the map?!");

			Object level = levelInfo.object();

			// read some basic level info
			Property author = level.property("Author");
			Property title = level.property("Title");
			Property description = level.property("Description");

			if (author != null) m.author = ((StringProperty)author).value.trim();
			if (title != null) m.title = ((StringProperty)title).value.trim();
			if (description != null) m.description = ((StringProperty)description).value.trim();

			if (map.version < 117) {
				Property idealPlayerCount = level.property("IdealPlayerCount");
				if (idealPlayerCount != null) m.playerCount = ((StringProperty)idealPlayerCount).value.trim();
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

			Property screenshot = level.property("Screenshot");

			// use this opportunity to resolve some version overlap between game versions
			if (screenshot != null && map.version < 117 && !map.objectsByClassName("LevelSummary").isEmpty()) m.game = "Unreal Tournament";

			List<BufferedImage> screenshots = screenshots(incoming, map, screenshot);
			saveImages(SHOT_NAME, m, screenshots, attachments);

		} catch (IOException e) {
			log.log(IndexLog.EntryType.CONTINUE, "Failed to read map package", e);
		} catch (Exception e) {
			log.log(IndexLog.EntryType.CONTINUE, "Caught while parsing map: " + e.getMessage(), e);
		}

		completed.accept(new IndexResult<>(m, attachments));
	}

	private Package map(Incoming incoming) {
		Set<Incoming.IncomingFile> maps = incoming.files(Incoming.FileType.MAP);
		if (maps.isEmpty()) throw new IllegalStateException("Failed to find a map file...");

		return new Package(new PackageReader(maps.iterator().next().asChannel()));
	}

	private String mapName(Incoming incoming) {
		String name = incoming.submission.filePath.getFileName().toString();

		Set<Incoming.IncomingFile> maps = incoming.files(Incoming.FileType.MAP);
		if (!maps.isEmpty()) {
			name = Util.fileName(maps.iterator().next().file);
		}

		return name.substring(0, name.lastIndexOf(".")).replaceAll("/", "").trim().replaceAll("[^\\x20-\\x7E]", "");
	}

	private String gameType(Incoming incoming, String name) {
		if (incoming.submission.override.get("gameType", null) != null) return incoming.submission.override.get("gameType", "DeathMatch");

		if (name.toLowerCase().startsWith("sp")) return "Single Player";
		if (name.toLowerCase().startsWith("dm")) return "DeathMatch";
		if (name.toLowerCase().startsWith("ctf-bt")) return "BunnyTrack";
		if (name.toLowerCase().startsWith("ctf4")) return "Multi-team Capture The Flag";
		if (name.toLowerCase().startsWith("ctfm")) return "Multi-team Capture The Flag";
		if (name.toLowerCase().startsWith("ctf")) return "Capture The Flag";
		if (name.toLowerCase().startsWith("dom")) return "Domination";
		if (name.toLowerCase().startsWith("as")) return "Assault";
		if (name.toLowerCase().startsWith("br")) return "Bombing Run";
		if (name.toLowerCase().startsWith("ons")) return "Onslaught";
		if (name.toLowerCase().startsWith("vctf")) return "Vehicle Capture The Flag";
		if (name.toLowerCase().startsWith("mh")) return "Monster Hunt";
		if (name.toLowerCase().startsWith("ma")) return "Monster Arena";
		if (name.toLowerCase().startsWith("tmh")) return "Team Monster Arena";
		if (name.toLowerCase().startsWith("ra")) return "Rocket Arena";
		if (name.toLowerCase().startsWith("jb")) return "Jailbreak";
		if (name.toLowerCase().startsWith("to")) return "Tactical Ops";
		if (name.toLowerCase().startsWith("inf")) return "Infiltration";
		if (name.toLowerCase().startsWith("bt")) return "BunnyTrack";
		if (name.toLowerCase().startsWith("uw")) return "UnWheel";
		if (name.toLowerCase().startsWith("scr")) return "Soccer";
		if (name.toLowerCase().startsWith("th")) return "Thievery";
		if (name.toLowerCase().startsWith("u4e")) return "Unreal4Ever";
		if (name.toLowerCase().startsWith("unf")) return "Unreal Fortress";

		if (maybeSingleplayer(incoming)) return "Single Player";

		return UNKNOWN;
	}

	private boolean maybeSingleplayer(Incoming incoming) {

		if (incoming.submission.filePath.toString().toLowerCase().contains("SinglePlayer")
			|| incoming.submission.filePath.toString().toLowerCase().contains("COOP")) {
			return true;
		}

		try {
			List<String> lines = textContent(incoming);

			for (String s : lines) {
				Matcher m = SP_MATCH.matcher(s);
				if (m.matches()) return true;
			}
		} catch (Exception e) {
			incoming.log.log(IndexLog.EntryType.CONTINUE, "Couldn't figure out if singleplayer by reading text files", e);
		}

		return false;
	}

	private String game(Incoming incoming) {
		if (incoming.submission.override.get("game", null) != null) return incoming.submission.override.get("game", "Unreal Tournament");

		for (String k : incoming.files.keySet()) {
			if (k.toLowerCase().endsWith(".unr")) return "Unreal Tournament";
			if (k.toLowerCase().endsWith(".ut2")) return "Unreal Tournament 2004";
			if (k.toLowerCase().endsWith(".ut3")) return "Unreal Tournament 3";
		}

		return UNKNOWN;
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
						String parentPkg = pkg instanceof Import ? ((Import)pkg).packageName.get().name().name : "None";
						shotPackage = findPackage(incoming, parentPkg.equals("None") ? pkg.name().name : parentPkg);
						ExportedObject exp = shotPackage.objectByName(((Import)shotResolved).name);
						object = exp.object();
					} catch (Exception e) {
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
								if (texture.name.name.toLowerCase().contains("shot")
									|| texture.name.name.toLowerCase().contains("screen")
									|| texture.name.name.toLowerCase().contains("preview")) {
									object = texture.object();
									break;
								}
							}

							// still not found anything... look for a texture with typical preview dimensions (512x256)
							if (!(object instanceof Texture)) {
								for (ExportedObject texture : textures) {
									Texture tex = (Texture)texture.object();
									Texture.MipMap mip = tex.mipMaps()[0];
									if (mip.width == 512 && mip.height == 256) {
										object = texture.object();
										break;
									}
								}
							}
						}
					}

					if (object instanceof Texture) {
						Texture.MipMap[] mipMaps = ((Texture)object).mipMaps();
						BufferedImage bufferedImage = mipMaps[0].get();
						images.add(bufferedImage);
					}
				}
			} catch (Exception e) {
				incoming.log.log(IndexLog.EntryType.CONTINUE, "Failed to read screenshot from packages", e);
			} finally {
				// cleanup if we spun up an external package for screenshots
				if (shotPackage != map) {
					try {
						shotPackage.close();
					} catch (IOException e) {
						incoming.log.log(IndexLog.EntryType.INFO, "Screenshot cleanup failed", e);
					}
				}
			}
		}

		if (images.isEmpty()) {
			// hmm, no screenshots were found... lets also look in the archive if there's a jpg or something
			try {
				Set<Incoming.IncomingFile> files = incoming.files(Incoming.FileType.IMAGE);
				for (Incoming.IncomingFile img : files) {
					BufferedImage image = ImageIO.read(Channels.newInputStream(Objects.requireNonNull(img.asChannel())));
					if (image != null) images.add(image);
				}
			} catch (Exception e) {
				incoming.log.log(IndexLog.EntryType.CONTINUE, "Failed to load screenshot from archive", e);
			}
		}

		return images;
	}

	private Package findPackage(Incoming incoming, String pkg) {
		Set<Incoming.IncomingFile> files = incoming.files(Incoming.FileType.IMPORTANT);
		for (Incoming.IncomingFile f : files) {
			String name = f.fileName();
			name = name.substring(0, name.lastIndexOf("."));
			if (name.equalsIgnoreCase(pkg)) {
				return new Package(new PackageReader(f.asChannel()));
			}
		}
		throw new IllegalStateException("Failed to find package " + pkg);
	}
}
