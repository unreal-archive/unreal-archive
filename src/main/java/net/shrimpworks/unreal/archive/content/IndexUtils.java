package net.shrimpworks.unreal.archive.content;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.dependencies.DependencyResolver;
import net.shrimpworks.unreal.dependencies.NativePackages;
import net.shrimpworks.unreal.dependencies.Resolved;
import net.shrimpworks.unreal.dependencies.ShippedPackages;
import net.shrimpworks.unreal.packages.IntFile;
import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.ExportedObject;
import net.shrimpworks.unreal.packages.entities.Import;
import net.shrimpworks.unreal.packages.entities.Name;
import net.shrimpworks.unreal.packages.entities.Named;
import net.shrimpworks.unreal.packages.entities.ObjectReference;
import net.shrimpworks.unreal.packages.entities.objects.Object;
import net.shrimpworks.unreal.packages.entities.objects.Texture;
import net.shrimpworks.unreal.packages.entities.objects.Texture2D;
import net.shrimpworks.unreal.packages.entities.properties.ArrayProperty;
import net.shrimpworks.unreal.packages.entities.properties.ObjectProperty;
import net.shrimpworks.unreal.packages.entities.properties.Property;

import static net.shrimpworks.unreal.archive.content.Content.DependencyStatus.*;

public class IndexUtils {

	public static final String UNKNOWN = "Unknown";
	public static final String RELEASE_UT99 = "1999-11";

	public static final Pattern AUTHOR_MATCH = Pattern.compile("(.+)?(author|by)(\\(s\\))?([\\s:]+)?([A-Za-z0-9 _\\-\"']{4,35})(\\s+)?",
															   Pattern.CASE_INSENSITIVE);
	public static final Pattern PLAYER_MATCH = Pattern.compile("(.+)?(player)(s| count)?([\\s:]+)?([A-Za-z0-9 \\-]{1,16})(\\s+)?",
															   Pattern.CASE_INSENSITIVE);

	public static final Pattern UT3_SCREENSHOT_MATCH = Pattern.compile("<Images:([^.]*)\\.(.*\\.)?([^>]+)>", Pattern.CASE_INSENSITIVE);

	public static final String SHOT_NAME = "%s_shot_%s_%d.png";

	public static Games game(Incoming incoming) {
		if (incoming.submission.override.get("game", null) != null) {
			return Games.byName(incoming.submission.override.get("game", Games.UNREAL_TOURNAMENT.name));
		}

		Set<Incoming.IncomingFile> files = incoming.files(Incoming.FileType.PACKAGES);
		if (files.isEmpty()) return Games.UNKNOWN;

		if (!incoming.files(Incoming.FileType.PLAYER, Incoming.FileType.STATICMESH).isEmpty()) return Games.UNREAL_TOURNAMENT_2004;
		if (!incoming.files(Incoming.FileType.PACKAGE).isEmpty()) return Games.UNREAL_TOURNAMENT_3;

		if (files.stream().anyMatch(f -> Util.extension(f.file).equalsIgnoreCase("ut2"))) return Games.UNREAL_TOURNAMENT_2004;
		if (files.stream().anyMatch(f -> Util.extension(f.file).equalsIgnoreCase("ut3"))) return Games.UNREAL_TOURNAMENT_3;
		if (files.stream().anyMatch(f -> Util.extension(f.file).equalsIgnoreCase("run"))) return Games.RUNE;
		if (files.stream().anyMatch(f -> Util.extension(f.file).equalsIgnoreCase("un2"))) return Games.UNREAL_2;

		for (Incoming.IncomingFile file : files) {
			try (Package pkg = new Package(new PackageReader(file.asChannel()))) {
				if (pkg.version < 68) return Games.UNREAL;
				// FIXME Rune uses version 69 it seems, which overlaps with UT
				else if (pkg.version < 117) return Games.UNREAL_TOURNAMENT;
				else if (pkg.version < 200) return Games.UNREAL_TOURNAMENT_2004;
				else return Games.UNREAL_TOURNAMENT_3;
			} catch (Exception e) {
				incoming.log.log(IndexLog.EntryType.CONTINUE, "Could not determine game from file " + file.fileName(), e);
			}
		}

		incoming.log.log(IndexLog.EntryType.CONTINUE, "Could not determine game for content");
		return Games.UNKNOWN;
	}

	/**
	 * Extract preview images/screenshots from a map package.
	 * <p>
	 * It tries hard.
	 *
	 * @param incoming   the package being indexed
	 * @param map        the map package
	 * @param screenshot screenshot property collected from the map
	 * @return list of images read from the map
	 */
	public static List<BufferedImage> screenshots(Incoming incoming, Package map, Property screenshot) {
		return screenshots(incoming, map, screenshot, true);
	}

	/**
	 * Extract preview images/screenshots from a map package.
	 * <p>
	 * It tries hard.
	 *
	 * @param incoming       the package being indexed
	 * @param map            the map package
	 * @param screenshot     screenshot property collected from the map
	 * @param scrapeFallback if no screenshot is found within the map try to find associated graphics
	 *                       files within the incoming files collection
	 * @return list of images read from the map
	 */
	public static List<BufferedImage> screenshots(Incoming incoming, Package map, Property screenshot, boolean scrapeFallback) {
		List<BufferedImage> images = new ArrayList<>();
		if (screenshot != null) {
			ObjectReference shotRef = ((ObjectProperty)screenshot).value;
			Named shotResolved = shotRef.get();

			Package shotPackage = map;

			try {
				Object object = null;

				if (shotResolved instanceof Import) {
					// sigh... its stored in another package
					Named pkg = ((Import)shotResolved).packageIndex.get();
					try {
						String parentPkg = pkg instanceof Import ? ((Import)pkg).packageIndex.get().name().name : "None";
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
					BufferedImage image = screenshotFromObject(shotPackage, object);
					if (image != null) images.add(image);
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
		} else {
			// there's no Screenshot property, lets hunt through the package for possible screenshots
			if (scrapeFallback) images.addAll(scrapeScreenshots(incoming, map));
		}

		return images;
	}

	private static List<BufferedImage> scrapeScreenshots(Incoming incoming, Package map) {
		List<BufferedImage> images = new ArrayList<>();

		// maybe it's a UT3 map
		if (map.version > 200) {
			readIntFiles(incoming, incoming.files(Incoming.FileType.INI)).findFirst().ifPresent(ini -> {
				ini.sections().forEach(s -> {
					IntFile.Value shot = ini.section(s).value("PreviewImageMarkup");
					if (shot instanceof IntFile.SimpleValue) {
						Matcher matcher = IndexUtils.UT3_SCREENSHOT_MATCH.matcher(((IntFile.SimpleValue)shot).value);
						if (matcher.find()) {
							ExportedObject export = map.objectByName(new Name(matcher.group(3)));
							if (export == null) return;

							Object object = export.object();

							if (object instanceof Texture2D) images.add(screenshotFromObject(map, object));

							// UT3 maps may use a Material to hold multiple screenshots
							if (object.className().equals("Material") && object.property("ReferencedTextures") instanceof ArrayProperty) {
								((ArrayProperty)object.property("ReferencedTextures")).values.forEach(t -> {
									if (t instanceof ObjectProperty) {
										Object tex = map.objectByRef(((ObjectProperty)t).value).object();
										if (tex instanceof Texture2D) {
											images.add(screenshotFromObject(map, tex));
										}
									}
								});
							}
						}
					}
				});
			});
		}

		// we found our screenshot, so we can end here
		if (!images.isEmpty()) return images;

		Stream.concat(map.exportsByClassName("Texture").stream(),
					  map.exportsByClassName("Texture2D").stream())
			  .filter(t -> t.name.name.toLowerCase().startsWith("screen") || t.name.name.toLowerCase().contains("shot"))
			  .map(t -> map.objectByName(t.name))
			  .filter(Objects::nonNull)
			  .map(ExportedObject::object)
			  .filter(Objects::nonNull)
			  .map(o -> screenshotFromObject(map, o))
			  .filter(Objects::nonNull)
			  .forEach(images::add);

		return images;
	}

	public static BufferedImage screenshotFromObject(Package shotPackage, Object object) {
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

		// UE1 has simple textures
		if (object instanceof Texture) return ((Texture)object).mipMaps()[0].get();

		// UE3 also has simple textures
		if (object instanceof Texture2D) return ((Texture2D)object).mipMaps()[0].get();

		return null;
	}

	/**
	 * Write out a collection of {@link BufferedImage}s as files and collect
	 * them as content attachments.
	 *
	 * @param shotTemplate template for filenames, should contain %s and %d
	 * @param content      the content
	 * @param screenshots  images to save
	 * @param attachments  attachment collection to populate
	 * @throws IOException failed to write files
	 */
	public static void saveImages(
		String shotTemplate, Content content, List<BufferedImage> screenshots, Set<IndexResult.NewAttachment> attachments
	) throws IOException {
		for (BufferedImage screenshot : screenshots) {
			String shotName = String.format(shotTemplate, Util.slug(content.name), content.hash.substring(0, 8), attachments.size() + 1);
			Path out = Paths.get(shotName);
			ImageIO.write(screenshot, "png", out.toFile());
			attachments.add(new IndexResult.NewAttachment(Content.AttachmentType.IMAGE, shotName, out));
		}
	}

	/**
	 * Search for a package within the indexed content.
	 *
	 * @param incoming content being indexed
	 * @param pkg      package to find
	 * @return a package
	 */
	public static Package findPackage(Incoming incoming, String pkg) {
		Set<Incoming.IncomingFile> files = incoming.files(Incoming.FileType.PACKAGES);
		for (Incoming.IncomingFile f : files) {
			String name = f.fileName();
			name = name.substring(0, name.lastIndexOf("."));
			if (name.equalsIgnoreCase(pkg)) {
				return new Package(new PackageReader(f.asChannel()));
			}
		}
		throw new IllegalStateException("Failed to find package " + pkg);
	}

	/**
	 * Find and return all image files within content being indexed.
	 *
	 * @param incoming content being indexed
	 * @return found images
	 */
	public static List<BufferedImage> findImageFiles(Incoming incoming) {
		List<BufferedImage> images = new ArrayList<>();
		Set<Incoming.IncomingFile> files = incoming.files(Incoming.FileType.IMAGE);
		for (Incoming.IncomingFile img : files) {
			try {
				BufferedImage image = ImageIO.read(Channels.newInputStream(Objects.requireNonNull(img.asChannel())));
				if (image != null) images.add(image);
			} catch (Exception e) {
				incoming.log.log(IndexLog.EntryType.CONTINUE, "Failed to load image from archive", e);
			}
		}

		return images;
	}

	/**
	 * Read and return all text content within content being indexed.
	 *
	 * @param incoming content being indexed
	 * @return all lines from plain text content
	 * @throws IOException failed to read files
	 */
	public static List<String> textContent(Incoming incoming, Incoming.FileType... fileTypes) throws IOException {
		List<String> lines = new ArrayList<>();
		for (Incoming.IncomingFile f : incoming.files(fileTypes)) {
			lines.addAll(textContent(incoming, f,
									 new ArrayList<>(List.of(StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1, Charset.forName("Cp1252"),
															 StandardCharsets.US_ASCII))
			));
		}

		return lines;
	}

	/**
	 * Attempts to read the contents of a text file using one of the given file encodings, in order.
	 */
	private static List<String> textContent(Incoming incoming, Incoming.IncomingFile file, List<Charset> encodings) throws IOException {
		while (!encodings.isEmpty()) {
			Charset encoding = encodings.remove(0);
			try (BufferedReader br = new BufferedReader(Channels.newReader(file.asChannel(), encoding))) {
				return (br.lines().collect(Collectors.toList()));
			} catch (MalformedInputException | UncheckedIOException ex) {
				if (encodings.isEmpty()) {
					incoming.log.log(IndexLog.EntryType.CONTINUE, "Could not read file file as " + encoding.name() + ", giving up");
					break;
				}

				// try another encoding if available
				incoming.log.log(IndexLog.EntryType.CONTINUE,
								 "Could not read file file as " + encoding.name() + ", trying " + encodings.get(0).name());
			}
		}
		return List.of();
	}

	/**
	 * Attempt to find the author of some content, based on included
	 * text files.
	 *
	 * @param incoming content being indexed
	 * @return an author if found, or unknown
	 */
	public static String findAuthor(Incoming incoming) {
		return findAuthor(incoming, false);
	}

	/**
	 * Attempt to find the author of some content, based on included
	 * text files.
	 *
	 * @param incoming       content being indexed
	 * @param searchIntFiles also search within .int file content
	 * @return an author if found, or unknown
	 */
	public static String findAuthor(Incoming incoming, boolean searchIntFiles) {
		Incoming.FileType[] types = searchIntFiles
			? new Incoming.FileType[] { Incoming.FileType.TEXT, Incoming.FileType.HTML, Incoming.FileType.INT }
			: new Incoming.FileType[] { Incoming.FileType.TEXT, Incoming.FileType.HTML };

		try {
			List<String> lines = IndexUtils.textContent(incoming, types);

			for (String s : lines) {
				Matcher m = AUTHOR_MATCH.matcher(s);
				if (m.matches() && !m.group(5).trim().isEmpty()) {
					return m.group(5).trim();
				}
			}
		} catch (IOException e) {
			incoming.log.log(IndexLog.EntryType.CONTINUE, "Failed attempt to read author", e);
		}

		return UNKNOWN;
	}

	/**
	 * Attempt to find a player count for a map, based on included text files.
	 *
	 * @param incoming content being indexed
	 * @return an author if found, or unknown
	 * @throws IOException failed to read files
	 */
	public static String findPlayerCount(Incoming incoming) throws IOException {
		Incoming.FileType[] types = new Incoming.FileType[] { Incoming.FileType.TEXT, Incoming.FileType.HTML };

		List<String> lines = IndexUtils.textContent(incoming, types);

		for (String s : lines) {
			Matcher m = PLAYER_MATCH.matcher(s);
			if (m.matches() && !m.group(5).trim().isEmpty()) {
				return m.group(5).trim();
			}
		}

		return UNKNOWN;
	}

	public static Stream<IntFile> readIntFiles(Incoming incoming, Set<Incoming.IncomingFile> intFiles) {
		return readIntFiles(incoming, intFiles, false);
	}

	public static Stream<IntFile> readIntFiles(Incoming incoming, Set<Incoming.IncomingFile> intFiles, boolean syntheticRoots) {
		return intFiles.stream()
					   .map(f -> {
						   try {
							   return readIntFile(
								   incoming, f, syntheticRoots,
								   new ArrayList<>(List.of(StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1, Charset.forName("Cp1252"),
														   StandardCharsets.US_ASCII))
							   );
						   } catch (IOException e) {
							   incoming.log.log(IndexLog.EntryType.CONTINUE, "Couldn't load INT file " + f.fileName(), e);
							   return null;
						   }
					   });
	}

	/**
	 * Attempts to read the contents of an int file using one of the given file encodings, in order.
	 */
	private static IntFile readIntFile(Incoming incoming, Incoming.IncomingFile intFile, boolean syntheticRoots,
									   List<Charset> encodings) throws IOException {
		while (!encodings.isEmpty()) {
			Charset encoding = encodings.remove(0);
			try {
				return new IntFile(intFile.asChannel(), syntheticRoots, encoding);
			} catch (MalformedInputException ex) {
				if (encodings.isEmpty()) throw ex;

				// try another encoding if available
				incoming.log.log(IndexLog.EntryType.CONTINUE,
								 "Could not read int file as " + encoding.name() + ", trying " + encodings.get(0).name());
			}
		}
		throw new IOException("Failed to load int file");
	}

	public static String friendlyName(String name) {
		// Cool_name_bro -> Cool Name Bro
		// cool-name-bro -> Cool Name Bro

		String[] words = name.replaceAll("([-_.])", " ").trim().split("\\s");
		String[] res = new String[words.length];

		for (int i = 0; i < words.length; i++) {
			if (words[i].length() <= 1) res[i] = words[i];
			else res[i] = Character.toUpperCase(words[i].charAt(0)) + words[i].substring(1);
		}

		return String.join(" ", res);
	}

	public static Map<String, List<Content.Dependency>> dependencies(Content content, Incoming incoming) {
		return dependencies(Games.byName(content.game), incoming);
	}

	public static Map<String, List<Content.Dependency>> dependencies(Games game, Incoming incoming) {
		ShippedPackages shippedPackages;
		switch (game) {
			case UNREAL:
				shippedPackages = ShippedPackages.UNREAL_GOLD; break;
			case UNREAL_TOURNAMENT_2004:
				shippedPackages = ShippedPackages.UNREAL_TOURNAMENT_2004; break;
			case UNREAL_TOURNAMENT_3:
				shippedPackages = ShippedPackages.UNREAL_TOURNAMENT_3; break;
			case RUNE:
				shippedPackages = ShippedPackages.RUNE; break;
			case UNREAL_TOURNAMENT:
			default:
				shippedPackages = ShippedPackages.UNREAL_TOURNAMENT;
		}

		Map<String, List<Content.Dependency>> dependencies = new HashMap<>();
		try {
			DependencyResolver resolver = new DependencyResolver(incoming.contentRoot, NativePackages.DEFAULT, e -> {
				incoming.log.log(IndexLog.EntryType.CONTINUE, "Dependency resolution error for " + e.file.toString(), e);
			});

			for (Incoming.IncomingFile file : incoming.files(Incoming.FileType.CODE, Incoming.FileType.MAP, Incoming.FileType.TEXTURE,
															 Incoming.FileType.STATICMESH, Incoming.FileType.ANIMATION)) {
				List<Content.Dependency> depList = new ArrayList<>();
				try {
					Map<String, Set<Resolved>> resolved = resolver.resolve(Util.plainName(file.fileName()));
					resolved.forEach((k, v) -> {
						if (!shippedPackages.contains(k)) {
							depList.add(new Content.Dependency(resolveDependency(v), k, null));
						}
					});
				} catch (Throwable e) {
					incoming.log.log(IndexLog.EntryType.CONTINUE, "Dependency resolution error for " + file.fileName(), e);
				}

				if (!depList.isEmpty()) dependencies.put(file.fileName(), depList);
			}
		} catch (IOException e) {
			incoming.log.log(IndexLog.EntryType.CONTINUE, "Dependency resolution failed for " + incoming.submission.filePath, e);
		}
		return dependencies;
	}

	private static Content.DependencyStatus resolveDependency(Set<Resolved> resolved) {
		Content.DependencyStatus result = null;
		for (Resolved r : resolved) {
			if (!r.children.isEmpty()) {
				Content.DependencyStatus childResult = resolveDependency(r.children);
				if (result == null) result = childResult;
				else if (result == OK && childResult == MISSING) result = PARTIAL;
			}

			if (r.resolved == null && result == null) result = MISSING;
			else if (r.resolved != null && result == null) result = OK;
			else if (r.resolved == null && result == OK) result = PARTIAL;
			else if (r.resolved != null && result == MISSING) result = PARTIAL;
			else if (r.resolved != null) result = OK;
		}
		return result == null ? MISSING : result;
	}
}
