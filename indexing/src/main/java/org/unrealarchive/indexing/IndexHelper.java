package org.unrealarchive.indexing;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.packages.IntFile;
import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.Import;
import net.shrimpworks.unreal.packages.entities.Named;
import net.shrimpworks.unreal.packages.entities.objects.Polys;

import org.unrealarchive.Main;
import org.unrealarchive.common.ArchiveUtil;
import org.unrealarchive.common.CLI;
import org.unrealarchive.common.Util;
import org.unrealarchive.content.Download;
import org.unrealarchive.content.FileType;
import org.unrealarchive.content.Games;
import org.unrealarchive.content.addons.Addon;
import org.unrealarchive.content.addons.GameType;
import org.unrealarchive.content.addons.GameTypeRepository;
import org.unrealarchive.content.addons.Map;
import org.unrealarchive.content.addons.MapGameTypes;
import org.unrealarchive.content.addons.MapPack;
import org.unrealarchive.content.addons.MapThemes;
import org.unrealarchive.content.addons.Model;
import org.unrealarchive.content.addons.Mutator;
import org.unrealarchive.content.addons.SimpleAddonRepository;
import org.unrealarchive.content.addons.Skin;
import org.unrealarchive.content.addons.Voice;
import org.unrealarchive.content.managed.Managed;
import org.unrealarchive.content.managed.ManagedContentRepository;
import org.unrealarchive.indexing.maps.MapIndexHandler;
import org.unrealarchive.mirror.LocalMirrorClient;
import org.unrealarchive.storage.DataStore;

import static org.unrealarchive.content.addons.Addon.UNKNOWN;

/**
 * Implements various quick and dirty helper/cleanup processes
 * for working with the contents of the addon index via the
 * content manager.
 */
public class IndexHelper {

	private static String ROOT = "./unreal-archive-data";

	public static void main(String[] args) throws IOException {
//		fixDDOMMaps();
//		reassignUT2003();
//		fixCDOMMaps();
//		reindexMapsWithThemes(args[0], args[1], args[2]);
//		removeGamefrontOnlineLinks();
//		removeDeadLinks();
//		attachmentMove();
//		attachmentGametypeMove();
//		removeB2Attachments();
//		removeB2Links();
//		fixDirectDownloads();
//		findUnrealPlayground();
//		moveAll();
//		removeWasabiLinks();
//		findPopularTextures("Unreal Tournament 2004", "MAP", "/home/shrimp/tmp/files/UnrealTournament2004/Maps");
//		findGametypes(args[0]);
//		checkPathing(args[0], args[1]);
//		contentDependencies(args[0], args[1], args[2]);
//		fixUnknownAuthors(args[0], args[1], args[2]);
//		umodDependencies(args[0]);
//		ukxDependencies();
//		fixMissingModels(args[0]);
//		fixModelNames(args[0]);
//		dedupeModelsSkinsNames(args[0]);
//		fixDuplicateMapPics(args[0], args[1]);
//		fixMapGametypes(args[0]);
//		fixMissingMapPics(args[0]);
//		fixMonterHuntSnipersParadise();
//		fixGreedMaps();
//		setMapPackGametypes();
//		findAutoIndexLinks(args[0], args[1], Integer.parseInt(args[2]));
//		findDupeFiles();
//		dedupeExtraFiles();
//		fixDoubleSlashLinks();
//		relinkMedor();
//		removeDuplicateEntries();
//		removeDuplicateFiles();
//		fixUt3PlayerCounts();

		fixVariations();

//		gc();
	}

	private static void gc() throws IOException {
		System.out.println(repo().gc());
	}

	private static SimpleAddonRepository repo() throws IOException {
		return new SimpleAddonRepository.FileRepository(Paths.get(ROOT).resolve("content"));
	}

	private static GameTypeRepository gametypeRepo() throws IOException {
		return new GameTypeRepository.FileRepository(Paths.get(ROOT).resolve("gametypes"));
	}

	private static ManagedContentRepository managedRepo() throws IOException {
		return new ManagedContentRepository.FileRepository(Paths.get(ROOT).resolve("managed"));
	}

	private static ContentManager manager() throws IOException {
		return new ContentManager(repo(), DataStore.NOP, DataStore.NOP);
	}

	private static GameTypeManager gametypes() throws IOException {
		return new GameTypeManager(gametypeRepo(), DataStore.NOP, DataStore.NOP);
	}

	private static ManagedContentManager managed() throws IOException {
		return new ManagedContentManager(managedRepo(), DataStore.NOP);
	}

	private static void maybeCheckin(ContentManager cm, Addon co, boolean changed) throws IOException {
		if (changed) checkinChange(cm, co);
	}

	private static void checkinChange(ContentManager cm, Addon co) throws IOException {
		if (cm.checkin(new IndexResult<>(co, Collections.emptySet()), null)) {
			System.out.println("Stored changes for " + String.join(" / ", co.game, co.contentType(), co.name));
		} else {
			System.out.println("Failed to apply for " + String.join(" / ", co.game, co.contentType(), co.name, co.hash));
		}
	}

	public static void reassignUT2003() throws IOException {
		ContentManager cm = manager();

		final String dateFrom = "2002-09-30"; // UT2003 release date
		final String dateTo = "2004-02-28"; // ~UT2004 release date (2004-02, to be safe)

		Collection<Addon> search = cm.repo()
									 .search("Unreal Tournament 2004", null, null, null)
									 .stream()
									 .filter(c ->
												 c.originalFilename.toLowerCase().contains("ut2k3") ||
												 c.originalFilename.toLowerCase().contains("ut2003") ||
												 (c.releaseDate.compareTo(dateFrom) > 0 && c.releaseDate.compareTo(dateTo) < 0))
									 .collect(Collectors.toSet()); ;
		for (Addon c : search) {
			Addon co = cm.checkout(c.hash);
			co.game = "Unreal Tournament 2003";
			checkinChange(cm, co);
			//System.out.printf("Move to UT2003: %s%n", String.join(" / ", co.game, co.contentType(), co.name, co.releaseDate, co.hash));
		}
	}

	public static void fixVariations() throws IOException {
		ContentManager cm = manager();

		Collection<Addon> search = cm.repo().search("Unreal Tournament 2003", null, null, null)
									 .stream().sorted(Comparator.comparing(Addon::addedDate))
									 .toList();
		for (Addon c : search) {
			Addon cur = cm.repo().forHash(c.hash);
			Addon existing = cm.repo().search(c.game, c.contentType,
											  c.name, c.author)
							   .stream()
							   .filter(m -> !Objects.equals(m.hash, c.hash))
							   .filter(m -> !Objects.equals(m.hash, cur.variationOf))
							   .filter(m -> !Objects.equals(m.variationOf, c.hash))
							   .max(Comparator.comparing(a -> a.releaseDate))
							   .orElse(null);
			if (existing != null) {
				// get current representation: may have been reassigned
				if (existing.variationOf == null && existing.releaseDate.compareTo(c.releaseDate) < 0) {
					Addon variation = cm.checkout(existing.hash);
					variation.variationOf = c.hash;
					checkinChange(cm, variation);
					System.out.printf("Flagging original content %s as variation of %s%n", existing.name(), c.name());
				} else if (cur.variationOf == null && existing.releaseDate.compareTo(c.releaseDate) > 0) {
					Addon variation = cm.checkout(c.hash);
					variation.variationOf = existing.hash;
					checkinChange(cm, variation);
					System.out.printf("Flagging %s as variation of %s%n", c.name(), existing.name());
				} else if (existing.variationOf == null && existing.firstIndex.isBefore(c.firstIndex)) {
					Addon variation = cm.checkout(existing.hash);
					variation.variationOf = c.hash;
					checkinChange(cm, variation);
					System.out.printf("Flagging content %s as variation of %s%n", existing.name(), c.name());
				}
			}
		}
	}

	public static void attachmentGametypeMove() throws IOException {
		final CLI cli = CLI.parse();
		try (DataStore imageStore = Main.store(DataStore.StoreContent.IMAGES, cli)) {
			GameTypeManager gm = gametypes();
			Collection<GameType> search = gm.repo().all().stream()
											.filter(g -> !g.maps.isEmpty())
											.filter(g -> g.maps.stream().anyMatch(m -> m.screenshot != null &&
																					   m.screenshot.url.contains("f002.backblazeb2.com")))
											.collect(Collectors.toSet());

			System.out.println("Found " + search.size());

			AtomicInteger counter = new AtomicInteger(0);
			search.parallelStream().forEach(orig -> {
				if (counter.incrementAndGet() % 100 == 0) System.out.printf("%d/%d%n", counter.get(), search.size());

				GameType co = gm.checkout(orig);

				final boolean[] changed = { false };

				co.maps.parallelStream()
					   .filter(m -> m.screenshot != null)
					   .filter(m -> m.screenshot.url.contains("f002.backblazeb2.com"))
//					   .filter(
//						   m -> orig.maps.stream()
//										 .filter(o -> o.screenshot != null)
//										 .noneMatch(o -> Util.fileName(m.screenshot.url).equals(Util.fileName(o.screenshot.url))
//														 && !o.screenshot.url.equals(m.screenshot.url)
//										 ))
					   .forEach(m -> {
						   try {
							   Util.urlRequest(m.screenshot.url, (imgCon) -> {
								   try {
									   Path base = Paths.get("");
									   Path uploadPath = co.contentPath(base);
									   String uploadName = base.relativize(uploadPath.resolve(m.screenshot.name)).toString();

									   long length = imgCon.getContentLength();
									   if (length <= 0) throw new RuntimeException("Dunno size");

									   imageStore.store(imgCon.getInputStream(), length, uploadName, (newUrl, ex) -> {
										   if (ex != null) System.err.printf("Failed[3]: %s - %s: %s%n", m.name, uploadName, ex);
										   if (newUrl != null) {
//											   m.screenshot = new Addon.Attachment(Addon.AttachmentType.IMAGE, m.screenshot.name, newUrl);
											   changed[0] = true;
										   }
									   });
								   } catch (IOException e) {
									   System.err.printf("Failed[2]: %s - %s: %s%n", m.name, m.screenshot.url, e);
								   }
							   });
						   } catch (IOException e) {
							   System.err.printf("Failed[1]: %s - %s: %s%n", m.name, m.screenshot.url, e);
						   }
					   });
				try {
					if (changed[0]) {
						gm.checkin(co);
					}
				} catch (Exception e) {
					System.out.println("Checkin failed " + orig.name + ": " + e.getMessage());
				}
			});
		}
	}

	public static void attachmentMove() throws IOException {
		final CLI cli = CLI.parse();
		try (DataStore imageStore = Main.store(DataStore.StoreContent.IMAGES, cli)) {
			ContentManager cm = manager();
			Collection<Addon> search = cm.repo().all().stream()
										 .filter(c -> !c.attachments.isEmpty())
										 .filter(c -> c.attachments.stream().anyMatch(a -> a.url.contains("f002.backblazeb2.com")))
										 .collect(Collectors.toSet());

			System.out.println("Found " + search.size());

			AtomicInteger counter = new AtomicInteger(0);
			search.parallelStream().forEach(orig -> {
				if (counter.incrementAndGet() % 100 == 0) System.out.printf("%d/%d%n", counter.get(), search.size());

				Addon co = cm.checkout(orig.hash);

				final boolean[] changed = { false };

				orig.attachments.parallelStream()
								.filter(a -> a.url.contains("f002.backblazeb2.com"))
								.filter(a -> orig.attachments.stream().noneMatch(o -> a.name.equals(o.name) && !o.url.equals(a.url)))
								.forEach(a -> {
									try {
										Util.urlRequest(a.url, (imgCon) -> {
											try {
												Path base = Paths.get("");
												Path uploadPath = co.contentPath(base);
												String uploadName = base.relativize(uploadPath.resolve(a.name)).toString();
//												System.out.println(uploadName);

												long length = imgCon.getContentLength();
												if (length <= 0) throw new RuntimeException("Dunno size");

												imageStore.store(imgCon.getInputStream(), length, uploadName, (newUrl, ex) -> {
													if (ex != null) System.err.printf("Failed[3]: %s - %s: %s%n", a.name, uploadName, ex);
													if (newUrl != null
														&& orig.attachments.stream().noneMatch(o -> o.url.equalsIgnoreCase(newUrl))) {
														co.attachments.add(
															new Addon.Attachment(Addon.AttachmentType.IMAGE, a.name, newUrl));
														changed[0] = true;
													}
												});
											} catch (IOException e) {
												System.err.printf("Failed[2]: %s - %s: %s%n", a.name, a.url, e);
											}
										});
									} catch (IOException e) {
										System.err.printf("Failed[1]: %s - %s: %s%n", a.name, a.url, e);
									}
								});
				try {
					maybeCheckin(cm, co, changed[0]);
				} catch (Exception e) {
					System.out.println("Checkin failed " + orig.name + ": " + e.getMessage());
				}
			});
		}
	}

	public static void removeB2Attachments() throws IOException {
		ContentManager cm = manager();
		Collection<Addon> search = cm.repo().all().stream()
									 .filter(c -> !c.attachments.isEmpty())
									 .filter(c -> c.attachments.stream().anyMatch(a -> a.url.contains("f002.backblazeb2.com")))
									 .filter(c -> c.attachments.stream().anyMatch(a -> a.url.contains("s3.us-west-002.backblazeb2.com")))
									 .collect(Collectors.toSet());

		System.out.println("Found " + search.size());

		AtomicInteger counter = new AtomicInteger(0);
		search.parallelStream().forEach(orig -> {
			if (counter.incrementAndGet() % 100 == 0) System.out.printf("%d/%d%n", counter.get(), search.size());

			Addon co = cm.checkout(orig.hash);

			try {
				maybeCheckin(cm, co, co.attachments.removeIf(a -> a.url.contains("f002.backblazeb2.com")));
			} catch (Exception e) {
				System.out.println("Checkin failed " + orig.name + ": " + e.getMessage());
			}
		});
	}

	public static void removeB2Links() throws IOException {
		ContentManager cm = manager();
		Collection<Addon> search = cm.repo().all();
		for (Addon c : search) {
			Addon co = cm.checkout(c.hash);
			boolean keep = co.downloads.stream().noneMatch(d -> d.url.contains("unreal-archive-files-s3.s3.us-west-002.backblazeb2.com"));
			if (keep) continue;

			maybeCheckin(cm, co, co.downloads.removeIf(d -> d.url.contains("f002.backblazeb2.com")));
		}

		GameTypeManager gm = gametypes();
		Set<GameType> gtSearch = gm.repo().all();
		for (GameType g : gtSearch) {
			GameType co = gm.checkout(g);
			boolean changed = false;
			boolean keep = false;
			for (GameType.Release r : co.releases) {
				if (r.deleted) continue;
				for (GameType.ReleaseFile f : r.files) {
					if (f.deleted) continue;
					keep = f.downloads.stream().noneMatch(d -> d.url.contains("unreal-archive-files-s3.s3.us-west-002.backblazeb2.com"));
					if (keep) {
						System.out.println("Gametype has not been mirrored: " + co.name);
						break;
					}
					changed = f.downloads.removeIf(d -> d.url.contains("f002.backblazeb2.com"));
				}
				if (keep) break;
			}

			if (!keep && changed) gm.checkin(co);
		}

		ManagedContentManager mm = managed();
		Collection<Managed> mSearch = mm.repo().all();
		for (Managed m : mSearch) {
			Managed co = mm.checkout(m);
			boolean changed = false;
			boolean keep = false;

			for (Managed.ManagedFile d : co.downloads) {
				if (d.deleted) continue;
				keep = d.downloads.stream().noneMatch(f -> f.url.contains("unreal-archive-files-s3.s3.us-west-002.backblazeb2.com"));
				if (keep) {
					System.out.println("Managed has not been mirrored: " + co.title);
					break;
				}
				changed = d.downloads.removeIf(f -> f.url.contains("f002.backblazeb2.com"));
			}

			if (!keep && changed) mm.checkin(co);
		}
	}

	private static boolean isDirect(String url) {
		return url.contains("backblaze")
			   || url.contains("linodeobjects")
			   || url.contains("vohzd")
			   || url.contains("blob.core.windows.net");
	}

	public static void fixDirectDownloads() throws IOException {
		ContentManager cm = manager();
		Collection<Addon> search = cm.repo().all();
		for (Addon c : search) {
			Addon co = cm.checkout(c.hash);
			co.downloads.forEach(d -> d.direct = isDirect(d.url));
			maybeCheckin(cm, co, true);
		}

		GameTypeManager gm = gametypes();
		Set<GameType> gtSearch = gm.repo().all().stream().filter(GameType::isVariation).collect(Collectors.toSet());
		for (GameType g : gtSearch) {
			GameType co = gm.checkout(g);
			for (GameType.Release r : co.releases) {
				for (GameType.ReleaseFile f : r.files) {
					f.downloads.forEach(d -> d.direct = isDirect(d.url));
				}
			}
			gm.checkin(co);
		}

		ManagedContentManager mm = managed();
		Collection<Managed> mSearch = mm.repo().all();
		for (Managed m : mSearch) {
			Managed co = mm.checkout(m);
			for (Managed.ManagedFile f : co.downloads) {
				f.downloads.forEach(d -> d.direct = isDirect(d.url));
			}
			mm.checkin(co);
		}
	}

	public static void relinkMedor() throws IOException {
		ContentManager cm = manager();
		Collection<Addon> search = cm.repo().all();

		final Pattern file = Pattern.compile(".*file=(.*)");

		for (Addon c : search) {
			Addon co = cm.checkout(c.hash);
			boolean changed = false;
			for (Download dl : co.downloads) {
				if (dl.url.contains("http://medor.no-ip.org/")) {
					Matcher m = file.matcher(dl.url);
					if (m.find()) {
						dl.url = "http://medor.no-ip.org/index.php?dir=&search_mode=f&search=" + m.group(1);
//						System.out.println(dl.url);
						changed = true;
					}
				}
			}

			maybeCheckin(cm, co, changed);
		}

	}

	public static void fixDoubleSlashLinks() throws IOException {
		ContentManager cm = manager();
		Collection<Addon> search = cm.repo().all();
		for (Addon c : search) {
			Addon co = cm.checkout(c.hash);
			boolean changed = false;
			for (Download dl : co.downloads) {

				if (dl.url.matches(".*[A-Za-z]//.*")) {
					dl.url = dl.url.replaceAll("([A-Za-z])//", "$1/");
					System.out.println(dl.url);
					changed = true;
				}
			}

			maybeCheckin(cm, co, changed);
		}

	}

	private static void trimNames() throws IOException {
		ContentManager cm = manager();

		Collection<Addon> search = cm.repo().search(null, null, null, null);
		for (Addon c : search) {
			if (!c.author.trim().equalsIgnoreCase(c.author)) {
				Addon fix = cm.checkout(c.hash);
				fix.author = fix.author.trim();
				if (cm.checkin(new IndexResult<>(fix, Collections.emptySet()), null)) {
					System.out.println("Stored changes for " + String.join(" / ", fix.game, fix.name));
				} else {
					System.out.println("Failed to apply");
				}
			}
		}
	}

	public static void findDupeFiles() throws IOException {
		final java.util.Map<String, Path> all = new HashMap<>();
//		final List<Path> reallyAll = new HashSet<>();
		Files.walkFileTree(Paths.get("unreal-archive-data/content/"), new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//				reallyAll.add(String.format());
				all.put(String.format("%s/%s", file.getParent().toString().toLowerCase(), file.getFileName()), file);
//				String s = file.getFileName().toString();
//				Path current = all.get(s.toLowerCase());
//				if (s.toLowerCase().equals(s)) all.put(s, file);
//				else if (current != null && current.getParent().equals(file.getParent())) {
//					System.out.println(file);
//				}
				return super.visitFile(file, attrs);
			}
		});

		for (String s : all.keySet()) {
			if (s.toLowerCase().equals(s)) continue;

			if (all.containsKey(s.toLowerCase())) {
				System.out.println(all.get(s));
				Files.deleteIfExists(all.get(s));
			}
		}

//		for (String s : reallyAll) {
//			if (s.toLowerCase().equals(s)) continue;
//
//			if ()
//		}
	}

	public static void dedupeExtraFiles() throws IOException {
		ContentManager cm = manager();
		Collection<Addon> search = cm.repo().all().stream()
									 .filter(m -> m.files.size() > 1)
									 .toList();

		for (Addon c : search) {
			Addon co = cm.checkout(c.hash);
			co.files = new ArrayList<>(new HashSet<>(co.files));
			if (co.files.size() < c.files.size()) {
				checkinChange(cm, co);
			}
		}
	}

	public static void dedupeModelsSkinsNames(String game) throws IOException {
		ContentManager cm = manager();
		Collection<Model> search = cm.repo().search(game, "MODEL", null, null).stream()
									 .filter(m -> m instanceof Model)
									 .map(m -> (Model)m)
									 .filter(m -> m.skins.size() > 1 || m.models.size() > 1)
									 .toList();
		for (Model c : search) {
			Model co = (Model)(cm.checkout(c.hash));
			co.models = new ArrayList<>(new HashSet<>(co.models));
			co.skins = new ArrayList<>(new HashSet<>(co.skins));
			if (co.models.size() < c.models.size() || co.skins.size() < c.skins.size()) {
				checkinChange(cm, co);
			}
		}

	}

	private static void fixMapGametypes(String gameTypeName) throws IOException {
		MapGameTypes.MapGameType gameType = MapGameTypes.byName(gameTypeName);
		assert gameType != null;

		ContentManager cm = manager();

		for (String mapPrefix : gameType.mapPrefixes) {
			Collection<Addon> search = cm.repo().search(null, "MAP", mapPrefix, null);
			for (Addon c : search) {
				if (c instanceof Map && !((Map)c).gametype.equalsIgnoreCase(gameType.name)
					&& c.name.toLowerCase().startsWith(mapPrefix.toLowerCase())) {
					Map map = (Map)cm.checkout(c.hash);
					map.gametype = gameType.name;
					if (cm.checkin(new IndexResult<>(map, Collections.emptySet()), null)) {
						System.out.println("Stored changes for " + String.join(" / ", map.game, map.gametype, map.name));
					} else {
						System.out.println("Failed to apply");
					}
				}
			}

		}
	}

	private static void fixUt3PlayerCounts() throws IOException {
		ContentManager cm = manager();

		Pattern playerCount = Pattern.compile("(\\d+(\\s?((up )?to|-)\\s?\\d+)?).*");
		Pattern author = Pattern.compile(".+?(by:\\s?|:\\s+?|by\\s)(.+)");

		Collection<Addon> search = cm.repo().search("Unreal Tournament 3", "MAP", null, null);
		for (Addon c : search) {
			if (c instanceof Map) {
				Map map = (Map)cm.checkout(c.hash);

				final String orig = map.playerCount;

				Matcher pc = playerCount.matcher(orig);
				boolean changed = false;
				if (pc.matches()) {
					map.playerCount = pc.group(1);
					changed = true;
				}

				if (map.author.equals("Unknown")) {
					Matcher am = author.matcher(orig);
					if (am.matches()) {
						map.author = am.group(2);
						changed = true;
					}
				}

				if (changed && cm.checkin(new IndexResult<>(map, Collections.emptySet()), null)) {
					System.out.println("Stored changes for " + String.join(" / ", map.game, map.gametype, map.name));
				} else {
					System.out.println("Failed to apply");
				}
			}
		}
	}

	private static void fixCDOMMaps() throws IOException {
		ContentManager cm = manager();

		Collection<Addon> search = cm.repo().search("Unreal Tournament 3", "MAP", "CDOM-", null);
		for (Addon c : search) {
			if (c instanceof Map
				&& c.name.startsWith("CDOM")
				&& !(((Map)c).gametype.equalsIgnoreCase("Domination"))) {
				Map map = (Map)cm.checkout(c.hash);
				map.gametype = "Domination";
				if (cm.checkin(new IndexResult<>(map, Collections.emptySet()), null)) {
					System.out.println("Stored changes for " + String.join(" / ", map.game, map.gametype, map.name));
				} else {
					System.out.println("Failed to apply");
				}
			}
		}
	}

	private static void fixDDOMMaps() throws IOException {
		ContentManager cm = manager();

		Collection<Addon> maps = cm.repo().search("Unreal Tournament 2004", "MAP", "DOM-", null);
		for (Addon c : maps) {
			if (c instanceof Map
				&& c.name.startsWith("DOM")
				&& !(((Map)c).gametype.equalsIgnoreCase("Double Domination"))) {
				Map map = (Map)cm.checkout(c.hash);
				map.gametype = "Double Domination";
				if (cm.checkin(new IndexResult<>(map, Collections.emptySet()), null)) {
					System.out.println("Stored changes for " + String.join(" / ", map.game, map.gametype, map.name));
				} else {
					System.out.println("Failed to apply");
				}
			}
		}

		Collection<Addon> packs = cm.repo().search("Unreal Tournament 2004", "MAP_PACK", null, null);
		for (Addon c : packs) {
			if (c instanceof MapPack
				&& (((MapPack)c).gametype.equalsIgnoreCase("Domination"))) {
				MapPack map = (MapPack)cm.checkout(c.hash);
				map.gametype = "Double Domination";
				if (cm.checkin(new IndexResult<>(map, Collections.emptySet()), null)) {
					System.out.println("Stored changes for " + String.join(" / ", map.game, map.gametype, map.name));
				} else {
					System.out.println("Failed to apply");
				}
			}
		}
	}

	private static void fixMonterHuntSnipersParadise() throws IOException {
		ContentManager cm = manager();

		Collection<Addon> search = cm.repo().search("Unreal Tournament", "MAP", "MH-[SP]", null);
		for (Addon c : search) {
			if (c instanceof Map && c.name.toLowerCase().startsWith("mh-[sp]".toLowerCase())) {
				Map map = (Map)cm.checkout(c.hash);
				map.game = "Unreal";
				if (cm.checkin(new IndexResult<>(map, Collections.emptySet()), null)) {
					System.out.println("Stored changes for " + String.join(" / ", map.game, map.gametype, map.name));
				} else {
					System.out.println("Failed to apply");
				}
			}
		}
	}

	private static void setMapPackGametypes() throws IOException {
		ContentManager cm = manager();
		Collection<MapPack> search = cm.repo().get(MapPack.class);
		for (MapPack mp : search) {
			if (!mp.gametype.equalsIgnoreCase(UNKNOWN)) continue;

			MapPack mapPack = (MapPack)cm.checkout(mp.hash);

			mapPack.gametype = UNKNOWN;
			for (MapPack.PackMap map : mapPack.maps) {
				MapGameTypes.MapGameType gt = MapGameTypes.forMap(Games.byName(mapPack.game()), map.name);
				if (gt == null) continue;

				if (mapPack.gametype.equalsIgnoreCase(UNKNOWN)) {
					mapPack.gametype = gt.name;
				} else if (!mapPack.gametype.equalsIgnoreCase(gt.name)) {
					mapPack.gametype = "Mixed";
					break;
				}
			}

			if (mapPack.gametype.equalsIgnoreCase(UNKNOWN)) continue;

			if (cm.checkin(new IndexResult<>(mapPack, Collections.emptySet()), null)) {
				System.out.printf("Set gametype for %s to %s%n", mapPack.name, mapPack.gametype);
			} else {
				System.out.println("Failed to apply");
			}
		}
	}

	private static void fixUnknownAuthors(String game, String type, String localFiles) throws IOException {
		final Path root = Paths.get(localFiles);

		ContentManager cm = manager();

		final java.util.Map<String, Path> fileHashes = new HashMap<>();
		Files.walkFileTree(root, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (ArchiveUtil.isArchive(file)) {
					fileHashes.put(Util.hash(file), file);
				}
				return super.visitFile(file, attrs);
			}
		});
		System.out.printf("Cached %d file hashes%n", fileHashes.size());

		Collection<Addon> search = cm.repo().search(game, type.toUpperCase(), null, null);
		final Path tmpDir = Files.createTempDirectory("ua-authors");

		List<Addon> contents = search.stream()
									 .filter(c -> !c.deleted)
									 .filter(c -> c.author.equalsIgnoreCase("unknown"))
									 .filter(c -> c.otherFiles > 0)
									 .sorted(Comparator.comparingInt(a -> a.fileSize))
									 .toList();

		System.out.printf("Processing %d contents%n", contents.size());

		for (int i = 0; i < contents.size(); i++) {
			if (i % 100 == 0) System.out.printf("%d/%d%n", i, contents.size());

			Addon co = cm.checkout(contents.get(i).hash);

			Path[] downloaded = { null };

			try {
				Path existing = fileHashes.get(co.hash);
				if (existing == null) {
					System.out.printf("Downloading %s (%dKB)%n", co.originalFilename, co.fileSize / 1024);
					new LocalMirrorClient.Downloader(co, tmpDir, d -> {
						System.out.printf("Downloaded %s%n", d.destination);
						downloaded[0] = d.destination;
					}).run();
				}

				Path file = downloaded[0] != null ? downloaded[0] : existing;

				Submission sub = new Submission(file);
				IndexLog log = new IndexLog();

				try (Incoming incoming = new Incoming(sub, log).prepare()) {
					co.author = IndexUtils.findAuthor(incoming);
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (!co.author.equalsIgnoreCase("unknown")) {
					checkinChange(cm, co);
				}
			} catch (Throwable e) {
				//
			} finally {
				if (downloaded[0] != null) {
					Files.deleteIfExists(downloaded[0]);
				}
			}
		}
	}

	private static void contentDependencies(String game, String type, String localFiles) throws IOException {
		final Path root = Paths.get(localFiles);

		ContentManager cm = manager();

		final java.util.Map<String, Path> fileHashes = new HashMap<>();
		Files.walkFileTree(root, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (ArchiveUtil.isArchive(file)) {
					fileHashes.put(Util.hash(file), file);
				}
				return super.visitFile(file, attrs);
			}
		});
		System.out.printf("Cached %d file hashes%n", fileHashes.size());

		Collection<Addon> search = cm.repo().search(game, type.toUpperCase(), null, null);
		final Path tmpDir = Files.createTempDirectory("ua-deps");

		List<Addon> contents = search.stream()
									 .filter(c -> !c.deleted)
//									   .filter(c -> c.dependencies.size() > 0)
//									   .filter(c -> c.dependencies.isEmpty())
									 .filter(c -> c.dependencies.isEmpty() || c.dependencies.values().stream()
																							.flatMap(Collection::stream)
																							.anyMatch(d -> d.status ==
																										   Addon.DependencyStatus.MISSING)
									 )
									 .sorted(Comparator.comparingInt(a -> a.fileSize))
									 .toList();

		System.out.printf("Processing %d contents%n", contents.size());

		AtomicInteger counter = new AtomicInteger(0);
		contents.parallelStream().forEach(content -> {
			if (counter.incrementAndGet() % 100 == 0) System.out.printf("%d/%d%n", counter.get(), contents.size());

			Addon co = cm.checkout(content.hash);

			Path[] downloaded = { null };

			try {
				Path existing = fileHashes.get(co.hash);
				if (existing == null) {
					System.out.printf("Downloading %s (%dKB)%n", co.originalFilename, co.fileSize / 1024);
					new LocalMirrorClient.Downloader(co, tmpDir, d -> {
						System.out.printf("Downloaded %s%n", d.destination);
						downloaded[0] = d.destination;
					}).run();
				}

				Path file = downloaded[0] != null ? downloaded[0] : existing;

				Submission sub = new Submission(file);
				IndexLog log = new IndexLog();
				try (Incoming incoming = new Incoming(sub, log).prepare()) {
					co.dependencies = IndexUtils.dependencies(Games.byName(co.game), incoming);
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (!co.dependencies.isEmpty()) {
					checkinChange(cm, co);
				}
			} catch (Throwable e) {
				//
			} finally {
				if (downloaded[0] != null) {
					try {
						Files.deleteIfExists(downloaded[0]);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		});

//		for (int i = 0; i < contents.size(); i++) {
//			if (i % 100 == 0) System.out.printf("%d/%d%n", i, contents.size());
//		}
	}

	private static void umodDependencies(String game) throws IOException {
		ContentManager cm = manager();

		Collection<Addon> search = cm.repo().search(game, null, null, null);
		final Path tmpDir = Files.createTempDirectory("ua-deps");

		Set<String> umods = Set.of("umod", "ut2mod", "ut4mod");
		List<Addon> contents = search.stream()
									 .filter(c -> !c.deleted)
									 .filter(c -> c.files.stream().anyMatch(d -> umods.contains(Util.extension(d.name).toLowerCase())))
									 .sorted(Comparator.comparingInt(a -> a.fileSize))
									 .toList();

		System.out.printf("Processing %d contents%n", contents.size());

		for (int i = 0; i < contents.size(); i++) {
			if (i % 10 == 0) System.out.printf("%d/%d%n", i, contents.size());

			Addon co = cm.checkout(contents.get(i).hash);

			new LocalMirrorClient.Downloader(co, tmpDir, d -> {
				System.out.printf("Downloaded %s%n", d.destination);
				try {
					Submission sub = new Submission(d.destination);
					IndexLog log = new IndexLog();
					try (Incoming incoming = new Incoming(sub, log).prepare()) {
						co.dependencies = IndexUtils.dependencies(Games.byName(co.game), incoming);

						co.files = new ArrayList<>();
						for (Incoming.IncomingFile f : incoming.files(FileType.ALL)) {
							if (!FileType.important(f.file)) continue;
							co.files.add(new Addon.ContentFile(f.fileName(), f.fileSize(), f.hash()));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

					checkinChange(cm, co);
				} catch (Throwable e) {
					//
				} finally {
					try {
						Files.deleteIfExists(d.destination);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).run();
		}
	}

	private static void ukxDependencies() throws IOException {
		ContentManager cm = manager();

		Collection<Addon> search = cm.repo().search(null, null, null, null);
		final Path tmpDir = Files.createTempDirectory("ua-deps");

		List<Addon> contents = search.stream()
									 .filter(c -> !c.deleted)
									 .filter(c -> c.files.stream().anyMatch(d -> Util.extension(d.name).equalsIgnoreCase("ukx")))
									 .sorted(Comparator.comparingInt(a -> a.fileSize))
									 .toList();

		System.out.printf("Processing %d contents%n", contents.size());

		for (int i = 0; i < contents.size(); i++) {
			if (i % 10 == 0) System.out.printf("%d/%d%n", i, contents.size());

			Addon co = cm.checkout(contents.get(i).hash);

			new LocalMirrorClient.Downloader(co, tmpDir, d -> {
				System.out.printf("Downloaded %s%n", d.destination);
				try {
					Submission sub = new Submission(d.destination);
					IndexLog log = new IndexLog();
					try (Incoming incoming = new Incoming(sub, log).prepare()) {
						co.dependencies = IndexUtils.dependencies(Games.byName(co.game), incoming);

						co.files = new ArrayList<>();
						for (Incoming.IncomingFile f : incoming.files(FileType.ALL)) {
							if (!FileType.important(f.file)) continue;
							co.files.add(new Addon.ContentFile(f.fileName(), f.fileSize(), f.hash()));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

					checkinChange(cm, co);
				} catch (Throwable e) {
					//
				} finally {
					try {
						Files.deleteIfExists(d.destination);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).run();
		}
	}

	private static void fixMissingModels(String game) throws IOException {
		ContentManager cm = manager();

		final Indexer indexer = new Indexer(cm.repo(), cm, new Indexer.IndexerEvents() {
			@Override
			public void starting(int foundFiles) {}

			@Override
			public void progress(int indexed, int total, Path currentFile) {}

			@Override
			public void indexed(Submission submission, Optional<IndexResult<? extends Addon>> indexed, IndexLog log) {}

			@Override
			public void completed(int indexedFiles, int errorCount) {}
		}, new Indexer.IndexerPostProcessor() {
			@Override
			public void indexed(Submission sub, Addon before, IndexResult<? extends Addon> result) {
				// do not let some things get reassigned during this process
				if (before != null) {
					result.content.game = before.game;
					if (!before.author.equals("Unknown")) result.content.author = before.author;
					result.content.variationOf = before.variationOf;
					result.content.attachments = before.attachments;
				}

				// in this process, we don't want to change files
				for (IndexResult.NewAttachment file : result.files) {
					try {
						Files.deleteIfExists(file.path());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				result.files.clear();
			}
		});

		Collection<Addon> search = cm.repo().search(game, "MODEL", null, null);
		final Path tmpDir = Files.createTempDirectory("ua-fix");

		List<Addon> contents = search.stream()
									 .filter(c -> !c.deleted)
//									   .filter(c -> c instanceof Model && ((Model)c).models.isEmpty())
									 .sorted(Comparator.comparingInt(a -> a.fileSize))
									 .toList();

		for (Addon c : contents) {
			try {
				new LocalMirrorClient.Downloader(c, tmpDir, d -> {
					System.out.printf("Downloaded %s%n", d.destination);
					try {
						indexer.index(true, false, 2, null, null, d.destination);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						try {
							Files.deleteIfExists(d.destination);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}).run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void fixModelNames(String game) throws IOException {
		ContentManager cm = manager();

		final Indexer indexer = new Indexer(cm.repo(), cm, new Indexer.IndexerEvents() {
			@Override
			public void starting(int foundFiles) {}

			@Override
			public void progress(int indexed, int total, Path currentFile) {}

			@Override
			public void indexed(Submission submission, Optional<IndexResult<? extends Addon>> indexed, IndexLog log) {}

			@Override
			public void completed(int indexedFiles, int errorCount) {}
		}, new Indexer.IndexerPostProcessor() {
			@Override
			public void indexed(Submission sub, Addon before, IndexResult<? extends Addon> result) {
				// do not let some things get reassigned during this process
				if (before != null) {
					result.content.game = before.game;
					result.content.author = before.author;
					result.content.variationOf = before.variationOf;
					result.content.attachments = before.attachments;

					System.out.println("Model named " + before.name + " is now " + result.content.name);
				}

				// in this process, we don't want to change files
				for (IndexResult.NewAttachment file : result.files) {
					try {
						Files.deleteIfExists(file.path());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				result.files.clear();
			}
		});

		Collection<Addon> search = cm.repo().search(game, "MODEL", null, null);
		final Path tmpDir = Files.createTempDirectory("ua-models");

		List<Addon> contents = search.stream()
									 .filter(c -> !c.deleted)
									 .filter(c -> c instanceof Model && ((Model)c).models.isEmpty())
									 .sorted(Comparator.comparingInt(a -> a.fileSize))
									 .toList();

		for (Addon c : contents) {
			try {
				new LocalMirrorClient.Downloader(c, tmpDir, d -> {
					System.out.printf("Downloaded %s%n", d.destination);
					try {
						indexer.index(true, false, 2, null, null, d.destination);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						try {
							Files.deleteIfExists(d.destination);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}).run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void fixDuplicateMapPics(String game, String type) throws IOException {
		final CLI cli = CLI.parse();
		final DataStore imageStore = Main.store(DataStore.StoreContent.IMAGES, cli);
		final DataStore contentStore = Main.store(DataStore.StoreContent.CONTENT, cli);
//		final DataStore imageStore = DataStore.NOP;
//		final DataStore attachmentStore = DataStore.NOP;
//		final DataStore contentStore = DataStore.NOP;

		final ContentManager cm = new ContentManager(repo(), contentStore, imageStore);

		final Indexer indexer = new Indexer(cm.repo(), cm, new Indexer.IndexerEvents() {
			@Override
			public void starting(int foundFiles) {}

			@Override
			public void progress(int indexed, int total, Path currentFile) {}

			@Override
			public void indexed(Submission submission, Optional<IndexResult<? extends Addon>> indexed, IndexLog log) {}

			@Override
			public void completed(int indexedFiles, int errorCount) {}
		}, new Indexer.IndexerPostProcessor() {
			@Override
			public void indexed(Submission sub, Addon before, IndexResult<? extends Addon> result) {
				// do not let some things get reassigned during this process
				if (before != null) {
					result.content.game = before.game;
					result.content.author = before.author;
					result.content.variationOf = before.variationOf;
//					result.content.attachments = before.attachments;
				}

				// in this process, we don't want to change files
//				for (IndexResult.NewAttachment file : result.files) {
//					try {
//						Files.deleteIfExists(file.path);
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}
//				result.files.clear();
			}
		});

		Collection<Addon> search = cm.repo().search(game, type.toUpperCase(), null, null);
		final Path tmpDir = Files.createTempDirectory("ua-dupe-maps");

		final java.util.Map<String, List<Addon>> contents = search.stream()
																  .filter(c -> !c.deleted)
																  .filter(c -> !c.attachments.isEmpty())
																  .collect(Collectors.groupingBy(c -> c.name.toLowerCase()))
																  .entrySet()
																  .stream()
																  .filter(e -> e.getValue().size() > 1)
																  .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()),
																		   HashMap::putAll);

		for (Addon c : contents.values().stream()
							   .flatMap(Collection::stream)
							   .sorted(Comparator.comparingInt(c -> c.fileSize))
							   .toList()
		) {
			try {
				new LocalMirrorClient.Downloader(c, tmpDir, d -> {
					System.out.printf("Downloaded %s%n", d.destination);
					try {
						indexer.index(true, false, 2, null, null, d.destination);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						try {
							Files.deleteIfExists(d.destination);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}).run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void checkPathing(String game, String localFiles) throws IOException {
		final Path root = Paths.get(localFiles);

		ContentManager cm = manager();

		final java.util.Map<String, Path> fileHashes = new HashMap<>();
		Files.walkFileTree(root, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (ArchiveUtil.isArchive(file)) {
					fileHashes.put(Util.hash(file), file);
				}
				return super.visitFile(file, attrs);
			}
		});

		System.out.printf("Cached %d file hashes%n", fileHashes.size());

		Collection<Addon> search = cm.repo().search(game, "MAP", null, null);
		final Path tmpDir = Files.createTempDirectory("ua-bots");

		List<Map> maps = search.stream()
							   .filter(c -> !c.deleted && c instanceof Map)
							   .map(c -> (Map)c)
							   .filter(c -> !c.bots)
//							   .filter(c -> c.hash.equalsIgnoreCase("9a236ea0398b1111f831959629b0420ac1a1de2c"))
							   .toList();

		System.out.printf("Processing %d maps%n", maps.size());

		AtomicInteger counter = new AtomicInteger(0);
		maps.parallelStream().forEach(c -> {
			if (counter.incrementAndGet() % 100 == 0) System.out.printf("%d/%d%n", counter.get(), maps.size());

			Path[] downloaded = { null };

			try {
				Addon co = cm.checkout(c.hash);
				boolean was = ((Map)co).bots;

				Path existing = fileHashes.get(c.hash);
				if (existing == null) {
					new LocalMirrorClient.Downloader(c, tmpDir, d -> {
						System.out.printf("Downloaded %s%n", d.destination);
						downloaded[0] = d.destination;
					}).run();
				}

				Path file = downloaded[0] != null ? downloaded[0] : existing;

				Submission sub = new Submission(file);
				IndexLog log = new IndexLog();
				try (Incoming incoming = new Incoming(sub, log).prepare()) {
					if (!incoming.files(FileType.MAP).isEmpty()) {
						try (Package pkg = new Package(
							new PackageReader(incoming.files(FileType.MAP).stream().findFirst().get().asChannel()))) {
							((Map)co).bots = MapIndexHandler.botSupport(pkg);
						} catch (Exception e) {
							//
						}
					}
				} catch (Exception e) {
					//
				}

				if (((Map)co).bots != was) {
					checkinChange(cm, co);
				} else {
					System.out.println("No change for " + String.join(" / ", co.game, co.name));
				}
			} catch (Throwable e) {
				//
			} finally {
				if (downloaded[0] != null) {
					try {
						Files.deleteIfExists(downloaded[0]);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});

	}

	public static void findGametypes(String searchPath) throws IOException {
		Path root = Paths.get(searchPath);
		Files.walkFileTree(root, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (!ArchiveUtil.isArchive(file)) return super.visitFile(file, attrs);

				Submission sub = new Submission(file);
				IndexLog log = new IndexLog();
				List<String> gametypes = new ArrayList<>();
				try (Incoming incoming = new Incoming(sub, log).prepare()) {
					IndexUtils.readIntFiles(incoming, incoming.files(FileType.INT))
							  .filter(Objects::nonNull)
							  .forEach(intFile -> {
								  IntFile.Section section = intFile.section("public");
								  if (section == null) return;

								  IntFile.ListValue objects = section.asList("Object");
								  for (IntFile.Value value : objects.values()) {
									  if (!(value instanceof IntFile.MapValue)) continue;
									  IntFile.MapValue mapVal = (IntFile.MapValue)value;

									  if (!mapVal.containsKey("MetaClass")) continue;

									  if (mapVal.get("MetaClass").toLowerCase().contains("tournamentgameinfo")) {
										  //gametypes.computeIfAbsent(file, k -> new ArrayList<>()).add(mapVal.getOrDefault("Name", "Dunno"));
										  gametypes.add(mapVal.getOrDefault("Name", "Dunno"));
									  }
								  }
							  });
					if (!gametypes.isEmpty()) {
						System.out.printf("%n%s: %s%n", file, String.join(", ", gametypes));
					} else System.out.print(".");
				} catch (Exception e) {
					e.printStackTrace();
				}
				return super.visitFile(file, attrs);
			}
		});

		//gametypes.forEach((k, v) -> System.out.printf("%s: %s%n", k, String.join(", ", v)));
	}

	public static void findUnrealPlayground() throws IOException {
		ContentManager cm = manager();
		Collection<Addon> search = cm.repo().search("Unreal Tournament", "MAP_PACK", null, null);
		search.stream().map(c -> (MapPack)c)
			  .sorted()
			  .forEach(c -> {
				  for (Download dl : c.downloads) {
					  if (dl.url.contains("unrealplayground")) {
						  System.out.printf("[url=https://unrealarchive.org/%s.html]%s[/url] - %d maps, by %s%n",
											c.slugPath(Paths.get("")),
											c.name, c.maps.size(), c.author);
						  break;
					  }
				  }

			  });
//		for (Content co : search) {
//			MapPack c = (MapPack)co;
//		}

	}

	public static void removeGamefrontOnlineLinks() throws IOException {
		ContentManager cm = manager();
		Collection<Addon> search = cm.repo().all();
		for (Addon c : search) {
			Addon co = cm.checkout(c.hash);
			boolean changed = false;
			for (Download dl : co.downloads) {

				if (dl.url.contains("gamefront.online")) {
					dl.state = Download.DownloadState.MISSING;
					changed = true;
				}
			}
			maybeCheckin(cm, co, changed);
		}

	}

	public static void removeUnrealPlaygroundLinks() throws IOException {
		ContentManager cm = manager();
		Collection<Addon> search = cm.repo().all();
		for (Addon c : search) {
			Addon co = cm.checkout(c.hash);
			boolean changed = false;
			for (Download dl : co.downloads) {
				if (dl.url.contains("unrealplayground") && dl.state == Download.DownloadState.OK) {
					dl.state = Download.DownloadState.MISSING;
					changed = true;
				}
			}
			if (changed) {
				if (cm.checkin(new IndexResult<>(co, Collections.emptySet()), null)) {
					System.out.println("Stored changes for " + String.join(" / ", co.game, co.name));
				} else {
//					System.out.println("Failed to apply for " + String.join(" / ", co.game, co.name, co.hash));
				}
			}
		}

	}

	public static void removeDeadLinks() throws IOException {
		ContentManager cm = manager();
		Collection<Addon> search = cm.repo().all();
		for (Addon c : search) {
			Addon co = cm.checkout(c.hash);
			boolean changed = co.downloads.removeIf(d -> d.state == Download.DownloadState.MISSING);
//			boolean changed = false;
//			for (Content.Download dl : co.downloads) {
//				if (dl.url.contains("gamefront") && dl.state == Content.DownloadState.OK) {
//					dl.state = Content.DownloadState.MISSING;
//					changed = true;
//				}
//			}
			maybeCheckin(cm, co, changed);
		}

	}

	public static void removeWasabiLinks() throws IOException {
		ContentManager cm = manager();
		Collection<Addon> search = cm.repo().all();
		for (Addon c : search) {
			Addon co = cm.checkout(c.hash);
			boolean changed = co.downloads.removeIf(d -> d.url.contains("eu-central-1.wasabisys.com"));
			maybeCheckin(cm, co, changed);
		}

	}

	public static void moveAll() throws IOException {
		ContentManager cm = manager();
		ContentManager cm2 = new ContentManager(new SimpleAddonRepository.FileRepository(Paths.get("/home/shrimp/tmp/unreal-archive-data")),
												DataStore.NOP,
												DataStore.NOP);
		Collection<Addon> search = cm.repo().all();
		for (Addon c : search) {
			Addon co = cm.checkout(c.hash);
			checkinChange(cm2, co);
		}
	}

	public static void reindexMapsWithThemes(String game, String type, String localFiles) throws IOException {
		ContentManager cm = manager();

		final Path root = Paths.get(localFiles);
		final java.util.Map<String, Path> fileHashes = new HashMap<>();
		Files.walkFileTree(root, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				fileHashes.put(Util.hash(file), file);
				return super.visitFile(file, attrs);
			}
		});

		System.out.printf("Cached %d file hashes", fileHashes.size());

		final Indexer indexer = new Indexer(cm.repo(), cm, new Indexer.IndexerEvents() {
			@Override
			public void starting(int foundFiles) {}

			@Override
			public void progress(int indexed, int total, Path currentFile) {}

			@Override
			public void indexed(Submission submission, Optional<IndexResult<? extends Addon>> indexed, IndexLog log) {}

			@Override
			public void completed(int indexedFiles, int errorCount) {}
		}, new Indexer.IndexerPostProcessor() {
			@Override
			public void indexed(Submission sub, Addon before, IndexResult<? extends Addon> result) {
				// do not let some things get reassigned during this process
				if (before != null) {
					result.content.game = before.game;
					result.content.author = before.author;
					result.content.variationOf = before.variationOf;
					result.content.attachments = before.attachments;
				}

				// in this process, we don't want to change files
				for (IndexResult.NewAttachment file : result.files) {
					try {
						Files.deleteIfExists(file.path());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				result.files.clear();

				// do not let gametype get reassigned
				if (before instanceof Map && result.content instanceof Map) {
					((Map)result.content).gametype = ((Map)before).gametype;
				}
				if (before instanceof MapPack && result.content instanceof MapPack) {
					((MapPack)result.content).gametype = ((MapPack)before).gametype;
				}
			}
		});

		Collection<Addon> search = cm.repo().search(game, type, null, null);
		final Path tmpDir = Files.createTempDirectory("ua-themes");

		for (Addon c : search) {
			if (c instanceof Map && !((Map)c).themes.isEmpty()) continue;
			if (c instanceof MapPack && !((MapPack)c).themes.isEmpty()) continue;

			Path existing = fileHashes.get(c.hash);
			try {
				if (existing != null) {
					System.out.printf("Indexing %s%n", existing);
					try {
						indexer.index(true, false, 2, null, null, existing);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					new LocalMirrorClient.Downloader(c, tmpDir, d -> {
						System.out.printf("Downloaded %s%n", d.destination);
						try {
							indexer.index(true, false, 2, null, null, d.destination);
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							try {
								Files.deleteIfExists(d.destination);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}).run();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void findPopularTextures(String game, String type, String localFiles) throws IOException {
		ContentManager cm = manager();

		final Path root = Paths.get(localFiles);
		final java.util.Map<String, Path> fileHashes = new HashMap<>();

		System.out.println("Finding existing files...");
		Files.walkFileTree(root, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				fileHashes.put(Util.hash(file), file);
				return super.visitFile(file, attrs);
			}
		});
		System.out.printf("Cached %d file hashes%n", fileHashes.size());

		Collection<Addon> search = cm.repo().search(game, type, null, null);
		final Path tmpDir = Files.createTempDirectory("ua-themes");

		final java.util.Map<String, Integer> textures = new HashMap<>();

		for (Addon c : search) {
			try {
				Path existing = fileHashes.get(c.hash);
				if (existing != null) {
					System.out.print(".");
					Submission sub = new Submission(existing);
					IndexLog log = new IndexLog();
					try (Incoming incoming = new Incoming(sub, log).prepare()) {
						try (Package pkg = new Package(new PackageReader(
							incoming.files(FileType.MAP).stream().findFirst().get().asChannel()
						))) {
							themes(pkg).forEach(t -> textures.compute(t, (k, count) -> (count == null) ? 1 : count + 1));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
//					new LocalMirrorClient.Downloader(c, tmpDir, d -> {
//						System.out.printf("Downloaded %s%n", d.destination);
//						Submission sub = new Submission(d.destination);
//						IndexLog log = new IndexLog(sub);
//						try (Incoming incoming = new Incoming(sub, log).prepare()) {
//							try (Package pkg = new Package(new PackageReader(
//									incoming.files(Incoming.FileType.MAP).stream().findFirst().get().asChannel()
//							))) {
//								themes(pkg).forEach(t -> textures.compute(t, (k, count) -> (count == null) ? 1 : count + 1));
//							}
//						} catch (Exception e) {
//							e.printStackTrace();
//						} finally {
//							try {
//								Files.deleteIfExists(d.destination);
//							} catch (IOException e) {
//								e.printStackTrace();
//							}
//						}
//					}).run();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		textures.entrySet().stream()
				.sorted(Collections.reverseOrder(java.util.Map.Entry.comparingByValue()))
				.forEach(e -> System.out.printf("%d\t:\t%s%n", e.getValue(), e.getKey()));
	}

	public static Set<String> themes(Package pkg) {
		final java.util.Map<String, Integer> foundThemes = new HashMap<>();

		// this can also work using "Models", but there are issues parsing those for UE2 maps
		pkg.objectsByClassName("Polys").forEach(o -> {
			Polys polys = (Polys)o.object();
			polys.polys.stream()
					   .map(p -> p.texture.get())
					   .filter(n -> n instanceof Import)
					   .map(n -> (Import)n)
					   .forEach(i -> {
						   // find the package a texture came from, which can be allocated to a theme
						   Import current = i;
						   Named parent = i.packageIndex.get();
						   while (parent instanceof Import) {
							   current = (Import)parent;
							   parent = current.packageIndex.get();
						   }

						   foundThemes.compute(current.name.name, (k, v) -> v == null ? 1 : ++v);
					   });
		});

		// for the top 5 themes, give them a percentage value of the total themeable content
		double totalScore = foundThemes.values().stream().mapToInt(e -> e).sum();
		return foundThemes.entrySet()
						  .stream()
						  .filter(e -> ((double)e.getValue() / totalScore) > MapThemes.MIN_THRESHOLD)
						  .collect(Collectors.toMap(java.util.Map.Entry::getKey,
													v -> BigDecimal.valueOf((double)v.getValue() / totalScore)
																   .setScale(1, RoundingMode.HALF_UP).doubleValue()
						  )).keySet();
	}

	private static void fixMissingMapPics(String game) throws IOException {
		final CLI cli = CLI.parse();
		final DataStore imageStore = Main.store(DataStore.StoreContent.IMAGES, cli);
		final DataStore contentStore = Main.store(DataStore.StoreContent.CONTENT, cli);

		final ContentManager cm = new ContentManager(repo(), contentStore, imageStore);

		final Indexer indexer = new Indexer(cm.repo(), cm, new Indexer.IndexerEvents() {
			@Override
			public void starting(int foundFiles) {}

			@Override
			public void progress(int indexed, int total, Path currentFile) {}

			@Override
			public void indexed(Submission submission, Optional<IndexResult<? extends Addon>> indexed, IndexLog log) {}

			@Override
			public void completed(int indexedFiles, int errorCount) {}
		}, new Indexer.IndexerPostProcessor() {
			@Override
			public void indexed(Submission sub, Addon before, IndexResult<? extends Addon> result) {
				// do not let some things get reassigned during this process
				if (before != null) {
					result.content.game = before.game;
					result.content.author = before.author;
					result.content.variationOf = before.variationOf;
				}
			}
		});

		Collection<Addon> search = cm.repo().search(game, null, null, null);
		final Path tmpDir = Files.createTempDirectory("ua-pics-maps");

		final List<Addon> contents = search.stream()
										   .filter(c -> !c.deleted)
										   .filter(c -> c.attachments.isEmpty())
										   .sorted(Comparator.comparingInt(c -> c.fileSize))
										   .toList();

		System.out.println(contents.size());

		for (Addon c : contents) {
			try {
				System.out.printf("Downloading %s%n", c.name);
				new LocalMirrorClient.Downloader(c, tmpDir, d -> {
					System.out.printf("Downloaded %s%n", d.destination);
					try {
						indexer.index(true, false, 1, null, null, d.destination);
					} catch (Throwable e) {
						e.printStackTrace();
					} finally {
						try {
							Files.deleteIfExists(d.destination);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}).run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void removeDuplicateEntries() throws IOException {
		ContentManager cm = manager();
		Collection<Addon> search = cm.repo().search(null, null, null, null);
		for (Addon c : search) {
			Addon co = cm.checkout(c.hash);
			if (co instanceof Voice thing) {
				int was = thing.voices.hashCode();
				thing.voices = thing.voices.stream().distinct().map(s -> {
					if (s.startsWith("\"")) s = s.substring(1);
					if (s.endsWith("\"")) s = s.substring(0, s.length() - 1);
					return s;
				}).toList();
				maybeCheckin(cm, thing, was != thing.voices.hashCode());
			} else if (co instanceof Skin thing) {
				int was = thing.faces.hashCode() + thing.skins.hashCode();
				thing.faces = thing.faces.stream().distinct().map(s -> {
					if (s.startsWith("\"")) s = s.substring(1);
					if (s.endsWith("\"")) s = s.substring(0, s.length() - 1);
					return s;
				}).toList();
				thing.skins = thing.skins.stream().distinct().map(s -> {
					if (s.startsWith("\"")) s = s.substring(1);
					if (s.endsWith("\"")) s = s.substring(0, s.length() - 1);
					return s;
				}).toList();
				maybeCheckin(cm, thing, was != (thing.faces.hashCode() + thing.skins.hashCode()));
			} else if (co instanceof Model thing) {
				int was = thing.models.hashCode() + thing.skins.hashCode();
				thing.models = thing.models.stream().distinct().map(s -> {
					if (s.startsWith("\"")) s = s.substring(1);
					if (s.endsWith("\"")) s = s.substring(0, s.length() - 1);
					return s;
				}).toList();
				thing.skins = thing.skins.stream().distinct().map(s -> {
					if (s.startsWith("\"")) s = s.substring(1);
					if (s.endsWith("\"")) s = s.substring(0, s.length() - 1);
					return s;
				}).toList();
				maybeCheckin(cm, thing, was != (thing.models.hashCode() + thing.skins.hashCode()));
			} else if (co instanceof Mutator thing) {
				int was = thing.mutators.hashCode() + thing.weapons.hashCode() + thing.vehicles.hashCode();
				thing.mutators = thing.mutators.stream().distinct().toList();
				thing.weapons = thing.weapons.stream().distinct().toList();
				thing.vehicles = thing.vehicles.stream().distinct().toList();
				maybeCheckin(cm, thing, was != (thing.mutators.hashCode() + thing.weapons.hashCode() + thing.vehicles.hashCode()));
			}
		}
	}

	private static void removeDuplicateFiles() throws IOException {
		ContentManager cm = manager();
		Collection<Addon> search = cm.repo().search(null, null, null, null);
		for (Addon c : search) {
			Addon co = cm.checkout(c.hash);
			int was = co.files.hashCode();
			co.files = co.files.stream().distinct().toList();
			maybeCheckin(cm, co, was != co.files.hashCode());
		}
	}
}
