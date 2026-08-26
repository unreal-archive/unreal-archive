package org.unrealarchive.indexing;

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
import java.util.stream.Stream;
import javax.imageio.ImageIO;

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

import org.unrealarchive.common.Util;
import org.unrealarchive.content.Authors;
import org.unrealarchive.content.FileType;
import org.unrealarchive.content.Games;
import org.unrealarchive.content.addons.Addon;

import static org.unrealarchive.content.addons.Addon.DependencyStatus.*;
import static org.unrealarchive.content.addons.Addon.UNKNOWN;

public class IndexUtils {

	public static final String RELEASE_UT99 = "1999-11";

	/**
	 * The keyword must be a word of its own - "STANDBY", "nearby" and "Derby" are not attributions.
	 * <p>
	 * The name must also end the line (the expression is used with {@link Matcher#matches()}).
	 * That requirement is load-bearing: a real attribution ends its line, and relaxing this to
	 * allow trailing commentary floods the candidate set with mid-sentence prose whose "by"
	 * happens to sit early on the line, displacing genuine trailing attributions.
	 */
	public static final Pattern AUTHOR_MATCH = Pattern.compile("(.+)?\\b(authors?|by)\\b(\\(s\\))?([\\s:]+)?(.{4,35})(\\s+)?",
															   Pattern.CASE_INSENSITIVE);
	public static final Pattern PLAYER_MATCH = Pattern.compile("(.+)?(player)(s| count)?([\\s:]+)?([A-Za-z0-9 \\-]{1,16})(\\s+)?",
															   Pattern.CASE_INSENSITIVE);

	/**
	 * A readme lays its metadata out in columns, so padding or an ellipsis run ends the name
	 * ("Adapt            adaptadapt", "Luger...........great stuff!!").
	 * <p>
	 * Three or more spaces, since two may be a typo inside a real name ("Cheney  Lansard").
	 */
	private static final Pattern COLUMN = Pattern.compile("(?<=\\S)\\s{3,}|\\t|(?<=\\w)\\.{3,}(?=\\w)");
	/**
	 * A column-separated tail which is more attribution must survive: a contribution ("OZ   *
	 * Modified by RUSH*", "DGunman   Original UT99:  (ASC)Djinn") or a co-author, which announces
	 * itself with a conjunction ("...$M]![L3   /  ù)v(indz-$L@Y3R").
	 */
	private static final Pattern CONTRIBUTION = Pattern.compile(
		"^\\s*[/&+]|\\b(and|modified|imported|converted|conversion|original|remix(ed)?|edit(ed)?|by)\\b",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern COPYRIGHT = Pattern.compile("\\(c\\)\\s*\\d{2,4}|\u00a9\\s*\\d{2,4}", Pattern.CASE_INSENSITIVE);
	/**
	 * An unterminated HTML tag is a leaked markup fragment ("&lt;A", "&lt;font color=..."), but only
	 * for recognised tag names - "&lt;Quest-, Paul Fahss, ..." and "k&lt;doubLe" are handles.
	 */
	private static final Pattern UNTERMINATED_TAG = Pattern.compile(
		"</?(a|b|i|p|br|img|font|td|tr|table|center|span|div|body|html)\\b[^>]*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern LEADING_LABEL = Pattern.compile("^((made|created|done|compiled)\\s+)?(by|authors?)\\s*:?\\s+",
																 Pattern.CASE_INSENSITIVE);
	private static final Pattern CLAUSE = Pattern.compile(
		",\\s*(for|using|with|to|in|from|so|if|feel|please|originally|mostly|while|when|after|before|though|but)\\b.*$",
		Pattern.CASE_INSENSITIVE);
	/**
	 * Characters a stylised handle decorates itself with. Brackets are absent deliberately: those are
	 * balanced instead, so a matched pair is never mistaken for decoration ("ScorpioN &lt;No Name&gt;").
	 */
	private static final String DECORATION = "-=~*_:;!.,|/\\^?%\u00a9\u00b0\u00b1#+'\"`";
	/** Sentence punctuation, which never wraps a name, so it always goes ("Sam Plate." -> "Sam Plate"). */
	private static final String PUNCTUATION = ".,";
	/** A single letter closed by a full stop is an initial or an acronym piece ("H.O.L.", "Ryan F."). */
	private static final Pattern INITIAL = Pattern.compile("(^|[\\s.])\\p{L}\\.$");
	/** Matching pairs at the ends only - a "()" inside "-=G|-|()ST=-" spells an O. */
	private static final Pattern EMPTY_PAIR = Pattern.compile("^(\\(\\s*\\)|\\[\\s*])\\s*|\\s*(\\(\\s*\\)|\\[\\s*])$");
	private static final Pattern DATE_TAIL = Pattern.compile(
		"[\\s,.\\-\u00b4]+((jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*([/-][a-z]+)?[\\s,]*)?"
		+ "(\\d{1,2}[/-]\\d{2,4}|(19|20)\\d\\d)$",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern MONTH_TAIL = Pattern.compile(
		"[\\s,.\\-]+\\d{1,2}\\s+(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern QUOTED = Pattern.compile("^[\"'`](.+)[\"'`]$");
	private static final String LEADING_SEPARATORS = " \t,;:";
	private static final Pattern SOME_NAME = Pattern.compile("[\\p{L}\\p{N}]");
	private static final Pattern NAME_START = Pattern.compile("^[A-Za-z0-9(\\[<.\\-=]");
	private static final Pattern MARKUP = Pattern.compile("<[^>]*>|&[a-z]+;|&#\\d+;", Pattern.CASE_INSENSITIVE);

	/** A value which is entirely one of these holds no name at all. */
	private static final Set<String> NOT_A_NAME = Set.of(
		"etc", "others", "other", "me", "myself", "above", "below", "default", "unknown", "various", "him", "her",
		"them", "anyone", "someone", "yourself", "author", "authors", "the", "email", "e-mail", "info", "information",
		"comments", "none", "n/a", "using", "pressing", "any", "those", "his", "skin", "skins", "name", "names",
		"credit", "credits", "mr", "mrs", "dr");

	/**
	 * A lowercase one of these opening the value means what was captured is mid-sentence prose
	 * ("of these skins", "and let me know!") rather than a name - but only when the following word
	 * is also lowercase, since real handles hide behind some of them ("the CreaTurE", "the DJ",
	 * "me: Miceal \"Blaze\" Kelley").
	 */
	private static final Set<String> PROSE_STARTERS = Set.of(
		"of", "and", "for", "or", "a", "an", "me", "us", "him", "her", "them", "my", "your", "his", "its",
		"this", "that", "these", "those", "it", "is", "was", "are", "in", "on", "at", "to", "from", "with",
		"the", "not", "about", "using", "creating", "pressing", "any", "some", "all", "one", "if", "so", "as",
		"i", "we", "you", "they", "he", "she", "but", "just", "also", "thanks", "thanx", "thx", "credit", "credits");

	/**
	 * Beyond this many characters ahead of the keyword the line is prose rather than an attribution;
	 * across the archive genuine attributions sit within the first 50 or so characters.
	 */
	private static final int MAX_AUTHOR_OFFSET = 150;

	public static final Pattern UT3_SCREENSHOT_MATCH = Pattern.compile("<Images:([^.]*)\\.(.*\\.)?([^>]+)>", Pattern.CASE_INSENSITIVE);

	public static final String SHOT_NAME = "%s_shot_%s_%d.png";

	public static Games game(Incoming incoming) {
		if (incoming.submission.override.get("game", null) != null) {
			return Games.byName(incoming.submission.override.get("game", Games.UNREAL_TOURNAMENT.name));
		}

		Set<Incoming.IncomingFile> files = incoming.files(FileType.PACKAGES);
		if (files.isEmpty()) return Games.UNKNOWN;

		// sometimes people name the packages after 227
		if (incoming.submission.filePath.getFileName().toString().contains("227")) return Games.UNREAL;
		if (incoming.submission.filePath.getFileName().toString().contains("ut2003")) return Games.UNREAL_TOURNAMENT_2003;
		if (incoming.submission.filePath.getFileName().toString().contains("ut2k3")) return Games.UNREAL_TOURNAMENT_2003;

		if (!incoming.files(FileType.PLAYER).isEmpty()) return Games.UNREAL_TOURNAMENT_2004;
		if (!incoming.files(FileType.PACKAGE).isEmpty()) return Games.UNREAL_TOURNAMENT_3;

		if (files.stream().anyMatch(f -> Util.extension(f.file).equalsIgnoreCase("usx"))) return Games.UNREAL_TOURNAMENT_2004;
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
			readIntFiles(incoming, incoming.files(FileType.INI)).findFirst().ifPresent(ini -> ini.sections().forEach(s -> {
				IntFile.Value shot = ini.section(s).value("PreviewImageMarkup");
				if (shot instanceof IntFile.SimpleValue) {
					Matcher matcher = IndexUtils.UT3_SCREENSHOT_MATCH.matcher(((IntFile.SimpleValue)shot).value());
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
			}));
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
		String shotTemplate, Addon content, List<BufferedImage> screenshots, Set<IndexResult.NewAttachment> attachments
	) throws IOException {
		for (BufferedImage screenshot : screenshots) {
			String shotName = String.format(shotTemplate, Util.slug(content.name), content.hash.substring(0, 8), attachments.size() + 1);
			Path out = Paths.get(System.getProperty("java.io.tmpdir")).resolve(shotName);
			ImageIO.write(screenshot, "png", out.toFile());
			attachments.add(new IndexResult.NewAttachment(Addon.AttachmentType.IMAGE, shotName, out));
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
		Set<Incoming.IncomingFile> files = incoming.files(FileType.PACKAGES);
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
		Set<Incoming.IncomingFile> files = incoming.files(FileType.IMAGE);
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
	public static List<String> textContent(Incoming incoming, FileType... fileTypes) throws IOException {
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
		// markup in an HTML readme is not content - "<FONT COLOR="#FFFFFF">by <A HREF="...">House"
		final boolean markup = FileType.HTML.matches(file.fileName());
		while (!encodings.isEmpty()) {
			Charset encoding = encodings.removeFirst();
			try (BufferedReader br = new BufferedReader(Channels.newReader(file.asChannel(), encoding))) {
				if (markup) return br.lines().map(l -> MARKUP.matcher(l).replaceAll(" ")).toList();
				return (br.lines().toList());
			} catch (MalformedInputException | UncheckedIOException ex) {
				if (encodings.isEmpty()) {
					incoming.log.log(IndexLog.EntryType.CONTINUE, "Could not read file file as " + encoding.name() + ", giving up");
					break;
				}

				// try another encoding if available
				incoming.log.log(IndexLog.EntryType.CONTINUE,
								 "Could not read file file as " + encoding.name() + ", trying " + encodings.getFirst().name());
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
		FileType[] types = searchIntFiles
			? new FileType[] { FileType.TEXT, FileType.HTML, FileType.INT }
			: new FileType[] { FileType.TEXT, FileType.HTML };

		try {
			List<String> lines = IndexUtils.textContent(incoming, types);

			String maybeAuthor = findAuthor(lines);
			if (maybeAuthor != null) return maybeAuthor;
		} catch (IOException e) {
			incoming.log.log(IndexLog.EntryType.CONTINUE, "Failed attempt to read author", e);
		}

		return UNKNOWN;
	}

	public static String findAuthor(List<String> lines) {
		String best = null;
		int bestOffset = Integer.MAX_VALUE;

		for (String s : lines) {
			// contact details trailing the name would otherwise be captured as part of it, or
			// push the name past the length the expression will match at all
			Matcher m = AUTHOR_MATCH.matcher(Authors.stripContacts(s));
			if (!m.matches() || m.group(5).isBlank()) continue;

			// the nearer the keyword sits to the start of the line, the more the line reads as an
			// attribution rather than as prose which happens to mention an author
			int offset = m.group(1) == null ? 0 : m.group(1).length();
			if (offset > MAX_AUTHOR_OFFSET || offset >= bestOffset) continue;

			String author = authorName(m.group(5));
			if (author == null) continue;

			best = author;
			bestOffset = offset;

			// a labelled line ("Author: ...") cannot be bettered
			if (offset == 0) break;
		}

		return best;
	}

	/**
	 * Reduce a matched author expression to a name, or null if what was captured cannot be one.
	 */
	private static String authorName(String captured) {
		String name = cleanAuthor(captured);
		if (name.equals(UNKNOWN) || !NAME_START.matcher(name).find()) return null;

		// the captured line still carries the "by"/"made by" lead-in and any conversion credit,
		// neither of which belongs in a name, so those come off and the result is cleaned again
		name = cleanAuthor(Authors.cleanName(name));
		return name.equals(UNKNOWN) ? null : name;
	}

	/**
	 * Reduce an author value to just the name, or {@code Unknown} when no name survives.
	 * <p>
	 * The same rules serve a freshly matched name and a value already stored in the index: contact
	 * details, column padding, copyright markers, date tails, one-sided decoration, truncated
	 * parentheticals and markup fragments all come off, and a value holding no name at all
	 * (", etc", "of these skins") reduces to {@code Unknown}.
	 * <p>
	 * Deliberately NOT applied here: {@link Authors#cleanName(String)}'s by/modified/converted
	 * stripping. "X Modified by Y" is contributor information parsed out of the stored string, and
	 * must survive - author extraction strips it from the line it captured instead.
	 */
	public static String cleanAuthor(String author) {
		String a = author.strip().replace("-->", " ");
		a = UNTERMINATED_TAG.matcher(a).replaceAll("");
		a = Authors.stripContacts(a);

		// a value which was entirely a single email address keeps its username part, which may
		// itself still carry a URL ("[www.Heldsite.de]Heldsite@gmx.net")
		if (a.replace(" ", "").length() < 3) {
			String orig = author.strip();
			if (orig.indexOf('@') > 0 && !orig.contains(" ")) a = Authors.stripContacts(orig.substring(0, orig.indexOf('@')));
		}

		// the structural cuts run once; the trims loop to a fixed point, since each can expose
		// more work for the others ("(c) 2000 - Gene Ostrowski -" sheds its marker, then its dash)
		a = CLAUSE.matcher(a).replaceFirst("");
		String[] columns = COLUMN.split(a.strip(), 2);
		if (columns.length < 2 || !CONTRIBUTION.matcher(columns[1]).find()) a = columns[0];
		a = COPYRIGHT.matcher(a).replaceAll("");

		String prev = null;
		while (!a.equals(prev)) {
			prev = a;
			a = a.strip();
			a = trimLeadingSeparators(a);
			a = LEADING_LABEL.matcher(a).replaceFirst("");
			a = trimLeadingDecoration(a);
			a = EMPTY_PAIR.matcher(a).replaceAll("");
			a = trimUnbalancedClosers(a);
			// a truncated trailing parenthetical is cut; a leading clan "(" is part of the name
			if (count(a, '(') > count(a, ')') && a.lastIndexOf('(') > 0) a = a.substring(0, a.lastIndexOf('(')).strip();
			a = DATE_TAIL.matcher(a).replaceFirst("");
			a = MONTH_TAIL.matcher(a).replaceFirst("");
			a = trimTrailingDecoration(a);

			Matcher quoted = QUOTED.matcher(a);
			if (quoted.matches()) a = quoted.group(1).strip();
		}

		if (isNameless(a) || NOT_A_NAME.contains(a.toLowerCase())) return UNKNOWN;

		String[] words = a.split("[\\s:,.;]+", 3);
		if (PROSE_STARTERS.contains(words[0])  // lowercase members only, so this is a case-sensitive test
			&& (words.length < 2 || words[1].isEmpty() || !Character.isUpperCase(words[1].codePointAt(0)))) {
			return UNKNOWN;
		}

		return a;
	}

	/**
	 * True when a value holds no name at all, being decoration all the way down (".:..:", "???").
	 * <p>
	 * Unreadable, but for a value already in the index it is what the author called themselves,
	 * so the caller may prefer to leave it exactly as stored rather than reduce it to unknown.
	 */
	public static boolean isNameless(String value) {
		return SOME_NAME.matcher(value).results().count() < 2;
	}

	/**
	 * Drop trailing decoration, unless it pairs with something earlier in the value.
	 * <p>
	 * A stylised handle wraps itself: "?3rror?", "^~[Lohc]~Opa~^", "=iNi=bRiaN=-", "*NoReMoRsE***",
	 * "... CTF4:-=Musc@t=-". In each the closing decoration also appears further back, so it is part
	 * of the name. Where it does not - "Fira?", "Peter Muller!", "TR==" - it is emphasis or debris.
	 * Whitespace is not considered: it is stripped separately, so "*NoReMoRsE* " cannot masquerade
	 * as a two-character run and take the star with it.
	 */
	private static String trimTrailingDecoration(String a) {
		int end = a.length();
		while (end > 0 && DECORATION.indexOf(a.charAt(end - 1)) >= 0) end--;

		// nothing decorative at the end, or nothing but decoration in the whole value
		if (end == a.length() || end == 0) return a;

		// a full stop closing an initial or an acronym is part of the name, not sentence punctuation
		if (a.endsWith(".") && INITIAL.matcher(a).find()) return a;

		// only a lone full stop or comma is sentence punctuation; a run of them is decoration
		// ("Failsuicide... / Nobody..."), so it answers to the pairing test like anything else
		boolean lone = a.length() - end == 1;

		String head = a.substring(0, end);
		for (int i = end; i < a.length(); i++) {
			char c = a.charAt(i);
			if (lone && PUNCTUATION.indexOf(c) >= 0) continue;
			if (head.indexOf(c) >= 0) return a;
		}
		return head.strip();
	}

	/**
	 * Drop leading separators, unless one recurs later and so opens a tag (":SOLO:[EL]Darkchlor1",
	 * ":LoH:Lord McNeiL"). A lone leading separator is capture debris (", etc").
	 */
	private static String trimLeadingSeparators(String a) {
		int start = 0;
		while (start < a.length() && LEADING_SEPARATORS.indexOf(a.charAt(start)) >= 0) start++;
		if (start == 0 || start == a.length()) return a;

		String tail = a.substring(start);
		for (int i = 0; i < start; i++) {
			char c = a.charAt(i);
			if (!Character.isWhitespace(c) && tail.indexOf(c) >= 0) return a;
		}
		return tail;
	}

	/**
	 * Drop a leading decoration run followed by whitespace ("- Gene Ostrowski", "= [ IndecisioN ]",
	 * "... some inSane Genius"), unless it recurs later and so belongs to the name.
	 * <p>
	 * Only a run of one repeated character is a bullet. A run mixing characters is a clan tag
	 * (".:..: Remix/Add by gopostal", "-=AoR- ..."), and the tag is the author. The whitespace
	 * matters too: "-GhostXC" and "-=Musc@t=-" wear their decoration flush against the name.
	 */
	private static String trimLeadingDecoration(String a) {
		int start = 0;
		while (start < a.length() && DECORATION.indexOf(a.charAt(start)) >= 0) start++;
		if (start == 0 || start >= a.length() || !Character.isWhitespace(a.charAt(start))) return a;
		if (a.chars().limit(start).distinct().count() > 1) return a;

		String tail = a.substring(start).strip();
		for (int i = 0; i < start; i++) {
			if (tail.indexOf(a.charAt(i)) >= 0) return a;
		}
		return tail;
	}

	private static String trimUnbalancedClosers(String a) {
		while (!a.isEmpty()) {
			char last = a.charAt(a.length() - 1);
			char open = last == ')' ? '(' : last == ']' ? '[' : last == '>' ? '<' : 0;
			if (open == 0 || count(a, last) <= count(a, open)) break;
			a = a.substring(0, a.length() - 1).strip();
		}
		return a;
	}

	private static int count(String s, char c) {
		int n = 0;
		for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++;
		return n;
	}

	/**
	 * Attempt to find a player count for a map, based on included text files.
	 *
	 * @param incoming content being indexed
	 * @return an author if found, or unknown
	 * @throws IOException failed to read files
	 */
	public static String findPlayerCount(Incoming incoming) throws IOException {
		FileType[] types = new FileType[] { FileType.TEXT, FileType.HTML };

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
			Charset encoding = encodings.removeFirst();
			try {
				return new IntFile(intFile.asChannel(), syntheticRoots, encoding);
			} catch (MalformedInputException ex) {
				if (encodings.isEmpty()) throw ex;

				// try another encoding if available
				incoming.log.log(IndexLog.EntryType.CONTINUE,
								 "Could not read int file as " + encoding.name() + ", trying " + encodings.getFirst().name());
			}
		}
		throw new IOException("Failed to load int file");
	}

	/**
	 * Clean up a string read out of a package property for storage and display.
	 * <p>
	 * UT2003/2004 levels embed colour markup in strings as an ESC character followed by an RGB
	 * triplet; other control characters occasionally show up as well. Neither is useful to us.
	 *
	 * @param s string as read from a package
	 * @return the string without markup or control characters
	 */
	public static String cleanString(String s) {
		StringBuilder out = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == 0x1B) i += 3; // ESC, followed by R, G and B
			else if (c >= 0x20 || c == '\n' || c == '\t') out.append(c);
		}
		return out.toString().strip();
	}

	/**
	 * Clean the strings every content type carries, whichever handler and source they came from -
	 * package properties, .int/.ucl files and readme scraping all produce the same markup.
	 *
	 * @param content freshly indexed content
	 */
	public static void cleanStrings(Addon content) {
		content.name = cleanString(content.name);
		content.author = cleanString(content.author);
		content.description = cleanString(content.description);
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

	public static Map<String, List<Addon.Dependency>> dependencies(Addon content, Incoming incoming) {
		return dependencies(Games.byName(content.game), incoming);
	}

	public static Map<String, List<Addon.Dependency>> dependencies(Games game, Incoming incoming) {
		ShippedPackages shippedPackages = switch (game) {
			case UNREAL -> ShippedPackages.UNREAL_GOLD;
			case UNREAL_TOURNAMENT_2004 -> ShippedPackages.UNREAL_TOURNAMENT_2004;
			case UNREAL_TOURNAMENT_3 -> ShippedPackages.UNREAL_TOURNAMENT_3;
			case RUNE -> ShippedPackages.RUNE;
			default -> ShippedPackages.UNREAL_TOURNAMENT;
		};

		Map<String, List<Addon.Dependency>> dependencies = new HashMap<>();
		try {
			DependencyResolver resolver = new DependencyResolver(incoming.contentRoot, NativePackages.DEFAULT, e -> {
				incoming.log.log(IndexLog.EntryType.CONTINUE, "Dependency resolution error for " + e.file.toString(), e);
			});

			for (Incoming.IncomingFile file : incoming.files(FileType.CODE, FileType.MAP, FileType.TEXTURE,
															 FileType.STATICMESH, FileType.ANIMATION)) {
				List<Addon.Dependency> depList = new ArrayList<>();
				try {
					Map<String, Set<Resolved>> resolved = resolver.resolve(Util.plainName(file.fileName()));
					resolved.forEach((k, v) -> {
						if (!shippedPackages.contains(k)) {
							depList.add(new Addon.Dependency(resolveDependency(v), k, null));
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

	private static Addon.DependencyStatus resolveDependency(Set<Resolved> resolved) {
		Addon.DependencyStatus result = null;
		for (Resolved r : resolved) {
			if (!r.children.isEmpty()) {
				Addon.DependencyStatus childResult = resolveDependency(r.children);
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
