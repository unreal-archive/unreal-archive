package net.shrimpworks.unreal.archive.indexer.mappacks;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

import net.shrimpworks.unreal.archive.YAML;
import net.shrimpworks.unreal.archive.indexer.Content;
import net.shrimpworks.unreal.archive.indexer.Incoming;
import net.shrimpworks.unreal.archive.indexer.IndexHandler;
import net.shrimpworks.unreal.archive.indexer.IndexLog;
import net.shrimpworks.unreal.archive.indexer.IndexResult;
import net.shrimpworks.unreal.archive.indexer.Indexer;
import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.ExportedObject;
import net.shrimpworks.unreal.packages.entities.Import;
import net.shrimpworks.unreal.packages.entities.Named;
import net.shrimpworks.unreal.packages.entities.ObjectReference;
import net.shrimpworks.unreal.packages.entities.objects.Object;
import net.shrimpworks.unreal.packages.entities.objects.Texture;
import net.shrimpworks.unreal.packages.entities.properties.ObjectProperty;
import net.shrimpworks.unreal.packages.entities.properties.Property;

public class MapPackIndesHandler implements IndexHandler<MapPack> {

	private static final String SHOT_NAME = "%s_shot_%d.png";

	public static class MapPackIndesHandlerFactory implements IndexHandler.IndexHandlerFactory<MapPack> {

		@Override
		public IndexHandler<MapPack> get() {
			return new MapPackIndesHandler();
		}
	}

	@Override
	public void index(Incoming incoming, Content content, Consumer<IndexResult<MapPack>> completed) {
		IndexLog log = incoming.log;
		MapPack m = (MapPack)content;

		m.name = packName(m.name);

		Set<Incoming.IncomingFile> maps = incoming.files(Incoming.FileType.MAP);

		boolean gameOverride = false;
		if (incoming.submission.override.get("game", null) != null) {
			gameOverride = true;
			m.game = incoming.submission.override.get("game", "Unreal Tournament");
		} else {
			m.game = game(maps.iterator().next());
		}

		try (Package map = map(maps.iterator().next())) {
			if (!gameOverride) {
				// attempt to detect Unreal maps by possible release date
				if (map.version < 68 || (m.releaseDate != null && m.releaseDate.compareTo(RELEASE_UT99) < 0)) m.game = "Unreal";
				// Unreal does not contain a LevelSummary
				if (map.version == 68 && map.objectsByClassName("LevelSummary").isEmpty()) m.game = "Unreal";
			}
		} catch (IOException e) {
			log.log(IndexLog.EntryType.CONTINUE, "Failed to read map package", e);
		} catch (Exception e) {
			log.log(IndexLog.EntryType.CONTINUE, "Caught while parsing map pack: " + e.getMessage(), e);
		}

		m.maps.clear();
		Set<IndexResult.NewAttachment> attachments = new HashSet<>();
		for (Incoming.IncomingFile map : maps) {
			try {
				m.maps.add(addMap(map, images -> {
					try {
						IndexHandler.saveImages(SHOT_NAME, m, images, attachments);
					} catch (IOException e) {
						log.log(IndexLog.EntryType.CONTINUE, "Failed saving images for map pack map", e);
					}
				}));
			} catch (Exception e) {
				log.log(IndexLog.EntryType.CONTINUE, "Reading map failed", e);
			}
		}

		try {
			System.out.println(YAML.toString(m));
		} catch (IOException e) {
			e.printStackTrace();
		}

		completed.accept(new IndexResult<>(m, Collections.emptySet()));
	}

	private MapPack.PackMap addMap(Incoming.IncomingFile map, Consumer<List<BufferedImage>> listConsumer) throws IOException {
		try (Package pkg = map(map)) {
			listConsumer.accept(Collections.emptyList());
			return null;
		}
	}

	private String game(Incoming.IncomingFile incoming) {
		if (incoming.fileName().toLowerCase().endsWith(".unr")) return "Unreal Tournament";
		if (incoming.fileName().toLowerCase().endsWith(".ut2")) return "Unreal Tournament 2004";
		if (incoming.fileName().toLowerCase().endsWith(".ut3")) return "Unreal Tournament 3";
		if (incoming.fileName().toLowerCase().endsWith(".un2")) return "Unreal 2";

		return UNKNOWN;
	}

	private String packName(String name) {
		// Cool_name_bro -> Cool Name Bro
		// cool-name-bro -> Cool Name Bro

		String[] words = name.replaceAll("([-_.])", " ").trim().split("\\s");
		String[] res = new String[words.length];

		for (int i = 0; i < words.length; i++) {
			if (words[i].length() == 1) res[i] = words[i];
			else res[i] = Character.toUpperCase(words[i].charAt(0)) + words[i].substring(1);
		}

		return String.join(" ", res);
	}

	private Package map(Incoming.IncomingFile file) {
		return new Package(new PackageReader(file.asChannel()));
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
						shotPackage = IndexHandler.findPackage(incoming, parentPkg.equals("None") ? pkg.name().name : parentPkg);
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

		return images;
	}

}