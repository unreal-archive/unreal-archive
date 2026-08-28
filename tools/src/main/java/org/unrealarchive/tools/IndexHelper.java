package org.unrealarchive.tools;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;

import net.shrimpworks.unreal.packages.IntFile;
import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.Import;
import net.shrimpworks.unreal.packages.entities.Named;
import net.shrimpworks.unreal.packages.entities.objects.Polys;

import org.unrealarchive.common.ArchiveUtil;
import org.unrealarchive.common.CLI;
import org.unrealarchive.common.Util;
import org.unrealarchive.common.YAML;
import org.unrealarchive.content.Author;
import org.unrealarchive.content.AuthorRepository;
import org.unrealarchive.content.Authors;
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
import org.unrealarchive.content.addons.SimpleAddonType;
import org.unrealarchive.content.addons.Skin;
import org.unrealarchive.content.addons.Voice;
import org.unrealarchive.content.managed.Managed;
import org.unrealarchive.content.managed.ManagedContentRepository;
import org.unrealarchive.indexing.AddonClassifier;
import org.unrealarchive.indexing.AuthorNameUtils;
import org.unrealarchive.indexing.ContentManager;
import org.unrealarchive.indexing.GameTypeManager;
import org.unrealarchive.indexing.Incoming;
import org.unrealarchive.indexing.IndexLog;
import org.unrealarchive.indexing.IndexResult;
import org.unrealarchive.indexing.IndexUtils;
import org.unrealarchive.indexing.Indexer;
import org.unrealarchive.indexing.ManagedContentManager;
import org.unrealarchive.indexing.Submission;
import org.unrealarchive.mirror.LocalMirrorClient;
import org.unrealarchive.storage.DataStore;

import static org.unrealarchive.content.addons.Addon.UNKNOWN;
import static org.unrealarchive.storage.DataStore.store;

/**
 * Implements various quick and dirty helper/cleanup processes
 * for working with the contents of the addon index via the
 * content manager.
 */
public class IndexHelper {

	private static final String ROOT = "./unreal-archive-data";

	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println("Running index helper");
//		fixMissingScreenshots();
//		fixDDOMMaps();
//		reassignUT2003();
//		fixFileSize(args[0], args[1]);
//		fixCorruptStrings();
//		fixCtf4Maps();
//		reindexMapsWithThemes(args[0], args[1], args[2]);
//		removeGamefrontOnlineLinks();
//		removeVohzdUnrealLinks();
//		removeDeadLinks();
//		attachmentMove();
//		attachmentGametypeMove();
//		removeB2Attachments();
//		removeB2Links();
//		dedupeAttachments();
//		fixDirectDownloads();
//		fixDownloadEncoding();
//		findUnrealPlayground();
//		moveAll();
//		removeWasabiLinks();
//		findPopularTextures("Unreal Tournament 2004", "MAP", "/home/shrimp/tmp/files/UnrealTournament2004/Maps");
//		findGametypes(args[0]);
//		checkPathing(args[0], args[1]);
//		contentDependencies(args[0], args[1], args[2]);
//		fixUnknownAuthors(args[0], args[1], args[2]);
		reviewAuthors(args[0]);
//		cleanStoredAuthors(args.length > 0 ? args[0] : null, args.length > 1 ? args[1].toUpperCase() : null);
//		attributeUnknownAuthors();
//		umodDependencies(args[0]);
//		ukxDependencies();
//		fixMissingModels(args[0]);
//		fixModelNames(args[0]);
//		dedupeModelsSkinsNames(args[0]);
//		fixDuplicateMapPics(args[0], args[1]);
//		fixMapGametypes(args[0]);
//		fixMissingMapPics(args[0]);
//		fixMonsterHuntSnipersParadise();
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
//		fixVariations();

//		gc();
	}

	private static void gc() throws IOException {
		System.out.println(repo().gc());
	}

	public static SimpleAddonRepository repo() throws IOException {
		return new SimpleAddonRepository.FileRepository(Paths.get(ROOT).resolve("content"));
	}

	public static GameTypeRepository gametypeRepo() throws IOException {
		return new GameTypeRepository.FileRepository(Paths.get(ROOT).resolve("gametypes"));
	}

	public static ManagedContentRepository managedRepo() throws IOException {
		return new ManagedContentRepository.FileRepository(Paths.get(ROOT).resolve("managed"));
	}

	/**
	 * Indexing content which carries authors (map packs, gametypes) needs the static author
	 * repository in place, otherwise author lookups blow up.
	 *
	 * @return the repository, for callers which also read or write author configs
	 */
	public static AuthorRepository initAuthors() throws IOException {
		Path authorsPath = Paths.get(ROOT).resolve("authors");
		AuthorRepository authors = new AuthorRepository.FileRepository(authorsPath);
		Authors.setRepository(authors, authorsPath);
		return authors;
	}

	public static ContentManager manager() throws IOException {
		return new ContentManager(repo(), DataStore.NOP, DataStore.NOP);
	}

	public static GameTypeManager gametypes() throws IOException {
		return new GameTypeManager(gametypeRepo(), DataStore.NOP, DataStore.NOP);
	}

	public static ManagedContentManager managed() throws IOException {
		return new ManagedContentManager(managedRepo(), DataStore.NOP);
	}

	private static void maybeCheckin(ContentManager cm, Addon co, boolean changed) throws IOException {
		if (changed) checkinChange(cm, co);
	}

	private static boolean checkinChange(ContentManager cm, Addon co) throws IOException {
		if (cm.checkin(new IndexResult<>(co, Collections.emptySet()), null)) {
			System.out.println("Stored changes for " + String.join(" / ", co.game, co.contentType(), co.name));
			return true;
		} else {
			System.out.println("Failed to apply for " + String.join(" / ", co.game, co.contentType(), co.name, co.hash));
			return false;
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
									 .collect(Collectors.toSet());
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
		try (DataStore imageStore = store(DataStore.StoreContent.IMAGES, cli)) {
			GameTypeManager gm = gametypes();
			Collection<GameType> search = gm.repo().all().stream()
											.filter(g -> !g.maps.isEmpty())
											.filter(g -> g.maps.stream().anyMatch(m -> m.screenshot != null &&
																					   m.screenshot.url.contains(
																						   "ua-img.s3.us-west-002.backblazeb2.com")))
											.collect(Collectors.toSet());

			System.out.println("Found " + search.size());

			AtomicInteger counter = new AtomicInteger(0);
			search.parallelStream().forEach(orig -> {
				if (counter.incrementAndGet() % 10 == 0) System.out.printf("%d/%d%n", counter.get(), search.size());

				GameType co = gm.checkout(orig);

				final boolean[] changed = { false };

				co.maps.stream()
					   .filter(m -> m.screenshot != null)
					   .filter(m -> m.screenshot.url.contains("ua-img.s3.us-west-002.backblazeb2.com"))
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
											   m.screenshot = new Addon.Attachment(Addon.AttachmentType.IMAGE, m.screenshot.name, newUrl);
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
		try (DataStore imageStore = store(DataStore.StoreContent.IMAGES, cli)) {
			ContentManager cm = manager();
			Collection<Addon> search = cm.repo().all().stream()
										 .filter(c -> !c.attachments.isEmpty())
										 .filter(c -> c.attachments.stream().anyMatch(
											 a -> a.url.contains("ua-img.s3.us-west-002.backblazeb2.com")))
										 .filter(c -> c.attachments.stream().noneMatch(
											 a -> a.url.contains("unreal-archive-img.s3.sgp.io.cloud.ovh.net")))
										 .collect(Collectors.toSet());

			System.out.println("Found " + search.size());

			AtomicInteger counter = new AtomicInteger(0);
			search.parallelStream().forEach(orig -> {
				if (counter.incrementAndGet() % 100 == 0) System.out.printf("%d/%d%n", counter.get(), search.size());

				Addon co = cm.checkout(orig.hash);

				final boolean[] changed = { false };

				orig.attachments.stream()
								.filter(a -> a.url.contains("ua-img.s3.us-west-002.backblazeb2.com"))
								.filter(a -> orig.attachments.stream().noneMatch(o -> a.name.equals(o.name) && !o.url.equals(a.url)))
								.forEach(a -> {
									try {
										Util.urlRequest(a.url, (imgCon) -> {
											try {
												Path base = Paths.get("");
												Path uploadPath = co.contentPath(base);

												String shotName = String.format(IndexUtils.SHOT_NAME,
																				Util.slug(orig.name), orig.hash.substring(0, 8),
																				co.attachments.size() + 1);

												String uploadName = base.relativize(uploadPath.resolve(shotName)).toString();

												long length = imgCon.getContentLength();
												if (length <= 0) throw new RuntimeException("Dunno size");

												imageStore.store(imgCon.getInputStream(), length, uploadName, (newUrl, ex) -> {
													if (ex != null) System.err.printf("Failed[3]: %s - %s: %s%n", a.name, uploadName, ex);
													if (newUrl != null
														&& orig.attachments.stream().noneMatch(o -> o.url.equalsIgnoreCase(newUrl))) {
														co.attachments.add(
															new Addon.Attachment(Addon.AttachmentType.IMAGE, shotName, newUrl)
														);
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

	public static void dedupeAttachments() throws IOException {
		ContentManager cm = manager();
		Collection<Addon> search = cm.repo().all().stream()
									 .filter(c -> !c.attachments.isEmpty())
									 .filter(c -> c.attachments.size() > 1)
									 .collect(Collectors.toSet());
		search.parallelStream().forEach(orig -> {
			int wasCount = orig.attachments.size();
			Addon co = cm.checkout(orig.hash);
			Set<Addon.Attachment> attachments = new HashSet<>(co.attachments);
			co.attachments = new ArrayList<>(attachments);
			try {
				maybeCheckin(cm, co, wasCount != co.attachments.size());
			} catch (Exception e) {
				System.out.println("Checkin failed " + orig.name + ": " + e.getMessage());
			}
		});
	}

	public static void removeB2Attachments() throws IOException {
		ContentManager cm = manager();
		Collection<Addon> search = cm.repo().all().stream()
									 .filter(c -> !c.attachments.isEmpty())
									 .filter(
										 c -> c.attachments.stream().anyMatch(a -> a.url.contains("ua-img.s3.us-west-002.backblazeb2.com")))
									 .filter(c -> c.attachments.stream().anyMatch(
										 a -> a.url.contains("unreal-archive-img.s3.sgp.io.cloud.ovh.net")))
									 .collect(Collectors.toSet());

		System.out.println("Found " + search.size());

		AtomicInteger counter = new AtomicInteger(0);
		search.parallelStream().forEach(orig -> {
			if (counter.incrementAndGet() % 100 == 0) System.out.printf("%d/%d%n", counter.get(), search.size());

			Addon co = cm.checkout(orig.hash);

			try {
				maybeCheckin(cm, co, co.attachments.removeIf(a -> a.url.contains("ua-img.s3.us-west-002.backblazeb2.com")));
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
			boolean keep = co.downloads.stream().noneMatch(d -> d.url.contains("unreal-archive-files-na.s3.ca-east-tor.io.cloud.ovh.net"));
			if (keep) continue;

			maybeCheckin(cm, co, co.downloads.removeIf(d -> d.url.contains("unreal-archive-files-s3.s3.us-west-002.backblazeb2.com")));
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
					keep = f.downloads.stream().noneMatch(d -> d.url.contains("unreal-archive-files-na.s3.ca-east-tor.io.cloud.ovh.net"));
					if (keep) {
						System.out.println("Gametype has not been mirrored: " + co.name);
						break;
					}
					changed = f.downloads.removeIf(d -> d.url.contains("unreal-archive-files-s3.s3.us-west-002.backblazeb2.com"));
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
				keep = d.downloads.stream().noneMatch(f -> f.url.contains("unreal-archive-files-na.s3.ca-east-tor.io.cloud.ovh.net"));
				if (keep) {
					System.out.println("Managed has not been mirrored: " + co.title);
					break;
				}
				changed = d.downloads.removeIf(f -> f.url.contains("unreal-archive-files-s3.s3.us-west-002.backblazeb2.com"));
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

	public static void fixDownloadEncoding() throws IOException {
		ContentManager cm = manager();
		Collection<Addon> search = cm.repo().all();
		for (Addon c : search) {
			Addon co = cm.checkout(c.hash);
			boolean changed = false;
			for (Download dl : co.downloads) {
				final String from = "Single%2520Player";
				final String to = from.replaceAll("%2520", "%20");
				if (dl.url.contains(from)) {
					System.out.println(dl.url);
					dl.url = dl.url.replaceAll(from, to);
					System.out.println(dl.url);
					changed = true;
				}
			}

			maybeCheckin(cm, co, changed);
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

		for (String mapPrefix : gameType.mapPrefixes()) {
			Collection<Addon> search = cm.repo().search(null, "MAP", mapPrefix, null);
			for (Addon c : search) {
				if (c instanceof Map && !((Map)c).gametype.equalsIgnoreCase(gameType.name())
					&& c.name.toLowerCase().startsWith(mapPrefix.toLowerCase())) {
					Map map = (Map)cm.checkout(c.hash);
					map.gametype = gameType.name();
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

	private static void fixCtf4Maps() throws IOException {
		ContentManager cm = manager();

		Collection<Addon> search = cm.repo().search("Unreal Tournament", "MAP", "CTF-[4]", null);
		for (Addon c : search) {
			if (c instanceof Map
				&& c.name.startsWith("CTF-[4]")
				&& !(((Map)c).gametype.equalsIgnoreCase("Multi-Team CTF"))) {
				Map map = (Map)cm.checkout(c.hash);
				map.gametype = "Multi-Team CTF";
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

	private static void fixMonsterHuntSnipersParadise() throws IOException {
		ContentManager cm = manager();

		Collection<Addon> search = cm.repo().search("Unreal", "MAP", "MH-", null);
		for (Addon c : search) {
			if (c instanceof Map && c.name.toLowerCase().startsWith("mh-".toLowerCase())) {
				Map map = (Map)cm.checkout(c.hash);
				map.gametype = "Sniper's Paradise Monster Hunt";
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
					mapPack.gametype = gt.name();
				} else if (!mapPack.gametype.equalsIgnoreCase(gt.name())) {
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
		final Path localRoot = Paths.get(localFiles).toAbsolutePath();

		ContentManager cm = manager();

		final java.util.Map<String, Path> fileHashes = new HashMap<>();
		final Path hashIndex = localRoot.resolve("index");
		if (Files.exists(hashIndex)) {
			fileHashes.putAll(YAML.fromFile(hashIndex, new TypeReference<java.util.Map<String, Path>>() {}));
		} else {
			System.out.printf("Loading file hashes from %s%n", localRoot);
			List<Path> allFiles = new ArrayList<>();
			Files.walkFileTree(localRoot, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (ArchiveUtil.isArchive(file)) {
						allFiles.add(file);
					}
					return super.visitFile(file, attrs);
				}
			});
			System.out.printf("Found %d files%n%n", allFiles.size());
			allFiles.stream().parallel()
					.forEach(file -> {
						try {
							fileHashes.put(Util.hash(file), file);
						} catch (IOException e) {
							System.out.printf("Failed to hash file %s%n", file.toString());
						}
						if (fileHashes.size() % 1000 == 0) System.out.printf("Hashed %d files...\r", fileHashes.size());
					});
			Files.write(hashIndex, YAML.toBytes(fileHashes));
		}
		System.out.printf("%nCached %d file hashes%n", fileHashes.size());

		Collection<Addon> search = cm.repo().search(game, type.equals("*") ? null : type.toUpperCase(), null, null);
		final Path tmpDir = Files.createTempDirectory("ua-authors");

		List<Addon> contents = search.stream()
									 .filter(c -> !c.deleted)
									 .filter(c -> c.author.equalsIgnoreCase("unknown"))
									 .filter(c -> c.otherFiles > 0)
									 .sorted(Comparator.comparingLong(a -> a.fileSize))
									 .toList();

		System.out.printf("Processing %d contents%n", contents.size());

		// a review sheet, written as results land so an interrupted sweep keeps what it found
		Path sheet = Paths.get(String.format("authors-%s-%s.tsv", Util.slug(game), type.toLowerCase()));
		Files.writeString(sheet, String.join("\t", "hash", "path", "game", "type", "name", "author", "source") + "\n",
						  StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		int found = 0;

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

				String source = "";
				try (Incoming incoming = new Incoming(sub, log).prepare()) {
					co.author = IndexUtils.findAuthor(incoming);
					if (!co.author.equalsIgnoreCase(UNKNOWN)) source = sourceLine(incoming, co.author);
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (!co.author.equalsIgnoreCase(UNKNOWN) && checkinChange(cm, co)) {
					Files.writeString(sheet, reviewRow(cm, co, source), StandardOpenOption.APPEND);
					found++;
				}
			} catch (Throwable e) {
				//
			} finally {
				if (downloaded[0] != null) {
					Files.deleteIfExists(downloaded[0]);
				}
			}
		}

		System.out.printf("%nFound authors for %d of %d contents; review sheet: %s%n", found, contents.size(), sheet);
	}

	/**
	 * The readme line an author was taken from, as review evidence. Matched on letters and digits
	 * alone, since cleanup reshapes the captured text, and empty where no line still holds the name.
	 * <p>
	 * Contacts come off the line first, exactly as extraction saw it: a name captured either side of
	 * an email or bare domain ("by Frankiler - frankiler.freeservers.com - (April 2009)") is not a
	 * substring of the raw line at all.
	 */
	private static String sourceLine(Incoming incoming, String author) {
		String needle = squashed(author);
		if (needle.isEmpty()) return "";
		try {
			for (String line : IndexUtils.textContent(incoming, FileType.TEXT, FileType.HTML)) {
				if (squashed(Authors.stripContacts(line)).contains(needle)) return line;
			}
		} catch (IOException e) {
			// evidence is best effort; the author was still found without it
		}
		return "";
	}

	private static String squashed(String s) {
		StringBuilder out = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (Character.isLetterOrDigit(c)) out.append(Character.toLowerCase(c));
		}
		return out.toString();
	}

	/**
	 * One review row: enough to revert, amend or alias the value without opening the archive.
	 */
	private static String reviewRow(ContentManager cm, Addon co, String source) {
		Path cwd = Paths.get("").toAbsolutePath();
		Path yml = co.contentPath(cm.repo().path())
					 .resolve(String.format("%s_[%s].yml", Util.slug(co.name), co.hash.substring(0, 8)))
					 .toAbsolutePath().normalize();
		return String.join("\t", co.hash, cell(yml.startsWith(cwd) ? cwd.relativize(yml).toString() : yml.toString()),
						   cell(co.game), cell(co.contentType()), cell(co.name), cell(co.author), cell(source)) + "\n";
	}

	/**
	 * A tab or newline would break the row, and no field needs its interior whitespace preserved.
	 */
	private static String cell(String s) {
		return s == null ? "" : s.replaceAll("\\s+", " ").strip();
	}

	// -- begin author review sheet

	private static final String REVIEW_KEEP = ".";

	private record ReviewEntry(String hash, String name, String source) {}

	/**
	 * Curated author names, by repository key and by letters and digits alone.
	 */
	private record Curated(java.util.Map<String, String> byKey, java.util.Map<String, String> byLoose) {}

	/**
	 * Words which never sit inside a real name, so finding one marks the value as a captured
	 * phrase. Used only to order the sheet - nothing is rewritten on the strength of it.
	 */
	private static final Set<String> PHRASE_WORDS = Set.of(
		"the", "a", "an", "and", "of", "for", "in", "on", "at", "to", "by", "with", "from", "all",
		"it", "its", "as", "is", "was", "this", "that", "you", "your", "my", "me", "aka", "circa");

	/**
	 * Work through a sweep's TSV output by distinct author value rather than by entry, since one
	 * decision usually covers several entries and grouping variant spellings needs them side by side.
	 * <p>
	 * The first call writes {@code <sheet>.review}: one editable line per distinct value, worst
	 * first, carrying the readme lines the value came from and any other value which looks like the
	 * same author. Editing the action column and calling again applies the whole file in one pass
	 * over the index, then keeps it as {@code .done}.
	 */
	public static void reviewAuthors(String sheetFile) throws IOException {
		Path sheet = Paths.get(sheetFile);
		Path review = Paths.get(sheetFile + ".review");

		java.util.Map<String, List<ReviewEntry>> byAuthor = readSheet(sheet);
		System.out.printf("%s: %d entries, %d distinct authors%n",
						  sheet.getFileName(), byAuthor.values().stream().mapToInt(List::size).sum(), byAuthor.size());

		AuthorRepository authors = initAuthors();
		if (Files.exists(review)) applyReview(review, byAuthor, authors);
		else writeReview(review, byAuthor, authors);
	}

	private static java.util.Map<String, List<ReviewEntry>> readSheet(Path sheet) throws IOException {
		List<String> lines = Files.readAllLines(sheet);
		if (lines.isEmpty()) throw new IOException("Empty sheet: " + sheet);

		List<String> header = List.of(lines.getFirst().split("\t"));
		int hash = header.indexOf("hash"), name = header.indexOf("name");
		int author = header.indexOf("author"), source = header.indexOf("source");
		if (hash < 0 || author < 0 || name < 0) throw new IOException("Not an author sweep sheet: " + sheet);

		java.util.Map<String, List<ReviewEntry>> byAuthor = new HashMap<>();
		for (String line : lines.subList(1, lines.size())) {
			if (line.isBlank()) continue;
			String[] cols = line.split("\t", -1);
			if (cols.length <= author) continue;
			byAuthor.computeIfAbsent(cols[author], _ -> new ArrayList<>())
					.add(new ReviewEntry(cols[hash], cols[name], source >= 0 && source < cols.length ? cols[source] : ""));
		}
		return byAuthor;
	}

	private static void writeReview(Path review, java.util.Map<String, List<ReviewEntry>> byAuthor,
									AuthorRepository authors) throws IOException {
		Curated curated = curatedAuthors(authors);

		List<String> values = new ArrayList<>(byAuthor.keySet());
		values.sort(Comparator.<String>comparingInt(v -> -phraseScore(v, byAuthor.get(v)))
							  .thenComparingInt(v -> -byAuthor.get(v).size())
							  .thenComparing(Comparator.naturalOrder()));

		StringBuilder out = new StringBuilder();
		out.append("# ").append(review.getFileName()).append(" - ").append(byAuthor.size()).append(" values, ")
		   .append(byAuthor.values().stream().mapToInt(List::size).sum()).append(" entries\n#\n");
		out.append("# Edit the first column of each value line, then run again to apply:\n");
		out.append("#   ").append(REVIEW_KEEP).append("          keep as indexed\n");
		out.append("#   x          revert to Unknown\n");
		out.append("#   = Name     alias this value to Name; stored values stay, author pages merge\n");
		out.append("#   Any Name   replace the value with this name, on every entry counted\n");
		out.append("#\n# '>' is the readme line the value came from, '~' a note. Likeliest junk first.\n");

		for (String value : values) {
			List<ReviewEntry> entries = byAuthor.get(value);
			out.append('\n');
			for (String note : notes(value, entries, byAuthor, curated)) out.append("#\t~ ").append(note).append('\n');
			entries.stream().map(ReviewEntry::source).filter(s -> !s.isBlank()).distinct().limit(3)
				   .forEach(s -> out.append("#\t> ").append(s).append('\n'));
			out.append(REVIEW_KEEP).append('\t').append(entries.size()).append('\t').append(value).append('\n');
		}

		Files.writeString(review, out.toString());
		System.out.printf("Wrote %s - edit the action column and run again to apply%n", review);
	}

	private static void applyReview(Path review, java.util.Map<String, List<ReviewEntry>> byAuthor,
									AuthorRepository authors) throws IOException {
		ContentManager cm = manager();

		java.util.Map<String, Set<String>> aliases = new HashMap<>();
		int kept = 0, reverted = 0, replaced = 0, unmatched = 0;

		for (String line : Files.readAllLines(review)) {
			if (line.isBlank() || line.startsWith("#")) continue;

			String[] cols = line.split("\t", -1);
			if (cols.length < 3) {
				System.out.printf("Skipping line, expected 'action<tab>count<tab>value': %s%n", line);
				continue;
			}

			String action = cols[0].strip();
			String value = cols[2];
			List<ReviewEntry> entries = byAuthor.get(value);
			if (entries == null) {
				System.out.printf("No sheet entry holds the value '%s', skipping it%n", value);
				unmatched++;
			} else if (action.isEmpty() || action.equals(REVIEW_KEEP)) {
				kept++;
			} else if (action.equalsIgnoreCase("x")) {
				for (ReviewEntry entry : entries) if (setAuthor(cm, entry, UNKNOWN)) reverted++;
			} else if (action.startsWith("=")) {
				String canonical = action.substring(1).strip();
				if (canonical.isEmpty()) System.out.printf("Alias action with no name for '%s'%n", value);
				else aliases.computeIfAbsent(canonical, _ -> new HashSet<>()).add(value);
			} else {
				for (ReviewEntry entry : entries) if (setAuthor(cm, entry, action)) replaced++;
			}
		}

		for (java.util.Map.Entry<String, Set<String>> alias : aliases.entrySet()) {
			mergeAliases(authors, alias.getKey(), alias.getValue());
		}

		Path done = review.resolveSibling(review.getFileName() + ".done");
		Files.move(review, done, StandardCopyOption.REPLACE_EXISTING);
		System.out.printf("%nkept %d, reverted %d, replaced %d, alias groups %d, unmatched %d%nApplied file kept as %s%n",
						  kept, reverted, replaced, aliases.size(), unmatched, done.getFileName());
	}

	private static boolean setAuthor(ContentManager cm, ReviewEntry entry, String author) throws IOException {
		Addon co = cm.checkout(entry.hash());
		if (co == null) {
			System.out.printf("No content found for %s (%s)%n", entry.name(), entry.hash());
			return false;
		}
		if (author.equals(co.author)) return false;
		co.author = author;
		return checkinChange(cm, co);
	}

	/**
	 * Aliases are merged, never replaced: an author's existing spellings are not ours to discard.
	 */
	private static void mergeAliases(AuthorRepository authors, String canonical, Set<String> values) throws IOException {
		Author existing = authors.byName(canonical);
		Author author = existing == null || existing.equals(AuthorRepository.UNKNOWN)
						|| existing.equals(AuthorRepository.VARIOUS)
			? new Author(canonical)
			: existing;

		author.aliases.add(canonical);
		author.aliases.addAll(values);
		authors.put(author, false);
		System.out.printf("Author '%s' aliases: %s%n", author.name, author.aliases);
	}

	private static Curated curatedAuthors(AuthorRepository authors) {
		java.util.Map<String, String> byKey = new HashMap<>();
		java.util.Map<String, String> byLoose = new HashMap<>();
		for (Author author : authors.allDefined()) {
			Set<String> spellings = new HashSet<>(author.aliases);
			spellings.add(author.name);
			for (String name : spellings) {
				byKey.put(AuthorRepository.authorKey(name), author.name);
				byLoose.put(looseKey(name), author.name);
			}
		}
		return new Curated(byKey, byLoose);
	}

	private static List<String> notes(String value, List<ReviewEntry> entries,
									  java.util.Map<String, List<ReviewEntry>> byAuthor, Curated curated) {
		List<String> notes = new ArrayList<>();
		String key = AuthorRepository.authorKey(value);
		String loose = looseKey(value);

		String sameKey = curated.byKey().get(key);
		String sameLoose = curated.byLoose().get(loose);
		if (sameKey != null && !sameKey.equals(value)) notes.add("the index already reads this as '" + sameKey + "'");
		else if (sameKey == null && sameLoose != null) notes.add("curated author '" + sameLoose + "' is spelt differently - alias?");

		List<String> siblings = new ArrayList<>();
		for (String other : byAuthor.keySet()) {
			if (other.equals(value) || loose.isEmpty()) continue;
			String otherLoose = looseKey(other);
			if (otherLoose.isEmpty() || AuthorRepository.authorKey(other).equals(key)) continue;
			if (loose.contains(otherLoose) || otherLoose.contains(loose)) {
				siblings.add(String.format("'%s' (%d)", other, byAuthor.get(other).size()));
			}
		}
		if (!siblings.isEmpty()) notes.add("same author? " + String.join(", ", siblings));

		if (entries.stream().allMatch(e -> e.source().isBlank())) notes.add("no readme line names this");
		if (entries.size() > 1) {
			String names = entries.stream().map(ReviewEntry::name).distinct().limit(6).collect(Collectors.joining(", "));
			notes.add("on: " + names);
		}
		return notes;
	}

	/**
	 * Letters and digits alone, which spots spellings the repository key still keeps apart.
	 */
	private static String looseKey(String value) {
		StringBuilder out = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (Character.isLetterOrDigit(c)) out.append(Character.toLowerCase(c));
		}
		return out.toString();
	}

	private static int phraseScore(String value, List<ReviewEntry> entries) {
		int score = 0;
		String[] words = value.split("\\s+");
		for (int i = 0; i < words.length; i++) {
			String word = words[i].replaceAll("^\\W+|\\W+$", "");
			if (word.isEmpty() || !word.equals(word.toLowerCase())) continue;
			if (PHRASE_WORDS.contains(word)) score += 2;
			else if (i > 0) score += 1;
		}
		if (words.length >= 4) score++;
		if (entries.stream().allMatch(e -> e.source().isBlank())) score++;
		return score;
	}

	// -- end author review sheet

	// -- begin stored author cleanup

	/**
	 * Clean junk out of stored author values in place, without re-reading any content archives.
	 * <p>
	 * These are the same guarded rules author extraction applies to freshly matched names
	 * ({@link AuthorNameUtils#cleanAuthor(String)}), applied to values already in the index - contact
	 * details, column padding, copyright markers, dates, one-sided decoration, truncated
	 * parentheticals and markup fragments. A value which holds no name at all ("of these skins",
	 * ", etc") is reset to Unknown, which also returns it to the {@link #fixUnknownAuthors}
	 * candidate pool for re-extraction.
	 * <p>
	 * Map packs name an author per map as well, which reach author pages through
	 * {@code HasAuthors} and carry exactly the same junk, so they are cleaned in the same pass.
	 */
	public static void cleanStoredAuthors(String game, String type) throws IOException {
		ContentManager cm = manager();
		Collection<Addon> search = cm.repo().search(game, type, null, null);

		int scanned = 0, cleaned = 0, rejected = 0, nested = 0;
		for (Addon c : search) {
			if (c.deleted) continue;

			String fixed = c.author;
			if (Authors.isSomeone(c.author)) {
				scanned++;
				fixed = cleanStored(c.author);
			}

			boolean packJunk = false;
			if (c instanceof MapPack pack) {
				for (MapPack.PackMap m : pack.maps) {
					if (!Authors.isSomeone(m.author)) continue;
					scanned++;
					if (!cleanStored(m.author).equals(m.author)) packJunk = true;
				}
			}

			if (fixed.equals(c.author) && !packJunk) continue;

			Addon co = cm.checkout(c.hash);

			if (!fixed.equals(co.author)) {
				if (fixed.equals(UNKNOWN)) rejected++;
				else cleaned++;
				System.out.printf("%s [%s / %s / %s]%n  [%s] -> [%s]%n",
								  fixed.equals(UNKNOWN) ? "reject" : "clean ", co.game, co.contentType(), co.name,
								  escaped(co.author), escaped(fixed));
				co.author = fixed;
			}

			if (co instanceof MapPack pack) {
				for (MapPack.PackMap m : pack.maps) {
					if (!Authors.isSomeone(m.author)) continue;
					String mapFixed = cleanStored(m.author);
					if (mapFixed.equals(m.author)) continue;
					nested++;
					System.out.printf("%s [%s / %s / %s : %s]%n  [%s] -> [%s]%n",
									  mapFixed.equals(UNKNOWN) ? "reject" : "clean ", co.game, co.contentType(),
									  co.name, m.name, escaped(m.author), escaped(mapFixed));
					m.author = mapFixed;
				}
			}

			checkinChange(cm, co);
		}

		System.out.printf("%nScanned %d, cleaned %d, rejected to Unknown %d, pack map authors changed %d%n",
						  scanned, cleaned, rejected, nested);
	}

	/**
	 * Clean a stored author value, leaving one which never held a name exactly as stored.
	 * <p>
	 * ".:..:" and "???" are unreadable, but they are what the author called themselves, so they
	 * are kept as-is rather than reduced to Unknown.
	 */
	private static String cleanStored(String author) {
		String fixed = AuthorNameUtils.cleanAuthor(author);
		return fixed.equals(UNKNOWN) && AuthorNameUtils.isNameless(author) ? author : fixed;
	}

	// -- end stored author cleanup

	// -- begin author attribution from the index

	private static final String[] ATTRIBUTION_TIERS = { "identical files", "variation link", "same name", "pack maps" };

	/**
	 * Attribute unknown authors from evidence already in the index, downloading nothing.
	 * <p>
	 * Four sources, in descending order of confidence, the first hit winning:
	 * <ol>
	 * <li>content files byte-identical to those of a known-author entry - the same content
	 * repackaged (a zip and a 7z of one skin), which shares no submission hash</li>
	 * <li>a {@code variationOf} link to a known-author entry, in either direction</li>
	 * <li>the same name, game and type as exactly one known author</li>
	 * <li>for a map pack, the authors of the maps it holds - one distinct author is that author,
	 * several are Various</li>
	 * </ol>
	 * A source offering more than one distinct author is passed over rather than guessed at, the
	 * pack case excepted, where several authors is itself the answer. Derived values are cleaned
	 * before use, so junk in the source entry does not propagate.
	 */
	public static void attributeUnknownAuthors() throws IOException {
		ContentManager cm = manager();

		int[] counts = new int[ATTRIBUTION_TIERS.length];

		// a variation chain resolves one link per pass, and an entry attributed by one pass is
		// evidence for the next, so passes repeat until nothing further resolves. Only a stored
		// change counts as progress, so an entry the content manager refuses cannot loop forever
		for (int pass = 1; ; pass++) {
			int attributed = attributionPass(cm, counts);
			System.out.printf("-- pass %d attributed %d%n%n", pass, attributed);
			if (attributed == 0) break;
		}

		int total = 0;
		for (int i = 0; i < ATTRIBUTION_TIERS.length; i++) {
			System.out.printf("%-16s %d%n", ATTRIBUTION_TIERS[i], counts[i]);
			total += counts[i];
		}
		System.out.printf("%-16s %d%n", "attributed", total);
	}

	private static int attributionPass(ContentManager cm, int[] counts) throws IOException {
		Collection<Addon> all = cm.repo().all();

		final java.util.Map<String, Addon> byHash = new HashMap<>();
		final java.util.Map<String, Set<String>> variants = new HashMap<>();
		final java.util.Map<String, Set<String>> byFiles = new HashMap<>();
		final java.util.Map<String, Set<String>> byName = new HashMap<>();

		for (Addon c : all) {
			byHash.put(c.hash, c);
			if (c.variationOf != null) variants.computeIfAbsent(c.variationOf, _ -> new HashSet<>()).add(c.hash);

			String author = attributableAuthor(c);
			if (author == null) continue;
			if (!c.files.isEmpty()) byFiles.computeIfAbsent(filesKey(c), _ -> new HashSet<>()).add(author);
			byName.computeIfAbsent(nameKey(c), _ -> new HashSet<>()).add(author);
		}

		int attributed = 0;
		for (Addon c : all) {
			// only a genuine gap is filled: "Various" is already an answer, not a missing one
			if (c.deleted || !isUnattributed(c.author)) continue;

			String found = null;
			int tier = -1;

			if (!c.files.isEmpty()) {
				Set<String> sameFiles = byFiles.get(filesKey(c));
				if (sameFiles != null && sameFiles.size() == 1) {
					found = sameFiles.iterator().next();
					tier = 0;
				}
			}

			if (found == null) {
				Set<String> linked = new HashSet<>();
				linkedAuthor(byHash.get(c.variationOf), linked);
				for (String h : variants.getOrDefault(c.hash, Set.of())) linkedAuthor(byHash.get(h), linked);
				if (linked.size() == 1) {
					found = linked.iterator().next();
					tier = 1;
				}
			}

			if (found == null) {
				Set<String> sameName = byName.get(nameKey(c));
				if (sameName != null && sameName.size() == 1) {
					found = sameName.iterator().next();
					tier = 2;
				}
			}

			if (found == null && c instanceof MapPack pack) {
				Set<String> mapAuthors = pack.maps.stream()
												  .filter(m -> Authors.isSomeone(m.author))
												  .map(m -> AuthorNameUtils.cleanAuthor(m.author))
												  .filter(a -> !a.equals(UNKNOWN))
												  .collect(Collectors.toSet());
				if (!mapAuthors.isEmpty()) {
					found = mapAuthors.size() == 1 ? mapAuthors.iterator().next() : AuthorRepository.VARIOUS.name;
					tier = 3;
				}
			}

			if (found == null) continue;

			Addon co = cm.checkout(c.hash);
			System.out.printf("%-16s [%s / %s / %s]%n  -> [%s]%n",
							  ATTRIBUTION_TIERS[tier], co.game, co.contentType(), co.name, escaped(found));
			co.author = found;
			if (!checkinChange(cm, co)) continue;
			counts[tier]++;
			attributed++;
		}

		return attributed;
	}

	/**
	 * True where an entry names no author at all, as opposed to naming Various.
	 */
	private static boolean isUnattributed(String author) {
		return author == null || author.isBlank() || author.equalsIgnoreCase(UNKNOWN);
	}

	/**
	 * The cleaned author of an entry, or null where it does not name one worth copying.
	 */
	private static String attributableAuthor(Addon c) {
		if (c.deleted || !Authors.isSomeone(c.author)) return null;
		String author = AuthorNameUtils.cleanAuthor(c.author);
		return author.equals(UNKNOWN) ? null : author;
	}

	private static void linkedAuthor(Addon linked, Set<String> found) {
		if (linked == null) return;
		String author = attributableAuthor(linked);
		if (author != null) found.add(author);
	}

	private static String filesKey(Addon c) {
		return c.files.stream().map(f -> f.hash).sorted().collect(Collectors.joining("|"));
	}

	private static String nameKey(Addon c) {
		return String.join("|", c.game, c.contentType(), c.name.toLowerCase());
	}

	// -- end author attribution from the index

	private static void contentDependencies(String game, String type, String localFiles) throws IOException {
		final Path localRoot = Paths.get(localFiles);

		ContentManager cm = manager();

		final java.util.Map<String, Path> fileHashes = new HashMap<>();
		final Path hashIndex = localRoot.resolve("index");
		if (Files.exists(hashIndex)) {
			fileHashes.putAll(YAML.fromFile(hashIndex, new TypeReference<java.util.Map<String, Path>>() {}));
		} else {
			System.out.printf("Loading file hashes from %s%n%n", localRoot);
			Files.walkFileTree(localRoot, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (ArchiveUtil.isArchive(file)) {
						fileHashes.put(Util.hash(file), file);
						if (fileHashes.size() % 1000 == 0) System.out.printf("Hashed %d files...\r", fileHashes.size());
					}
					return super.visitFile(file, attrs);
				}
			});
			Files.write(hashIndex, YAML.toBytes(fileHashes));
		}
		System.out.printf("%nCached %d file hashes%n", fileHashes.size());

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
									 .sorted(Comparator.comparingLong(a -> a.fileSize))
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
									 .sorted(Comparator.comparingLong(a -> a.fileSize))
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
									 .sorted(Comparator.comparingLong(a -> a.fileSize))
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
									 .sorted(Comparator.comparingLong(a -> a.fileSize))
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
									 .sorted(Comparator.comparingLong(a -> a.fileSize))
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
		final DataStore imageStore = store(DataStore.StoreContent.IMAGES, cli);
		final DataStore contentStore = store(DataStore.StoreContent.CONTENT, cli);
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
							   .sorted(Comparator.comparingLong(c -> c.fileSize))
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

	/**
	 * Commented 2026-03 to remove dependency on Indexings MapIndexHandler.
	 */
//	private static void checkPathing(String game, String localFiles) throws IOException {
//		final Path root = Paths.get(localFiles);
//
//		ContentManager cm = manager();
//
//		final java.util.Map<String, Path> fileHashes = new HashMap<>();
//		Files.walkFileTree(root, new SimpleFileVisitor<>() {
//			@Override
//			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//				if (ArchiveUtil.isArchive(file)) {
//					fileHashes.put(Util.hash(file), file);
//				}
//				return super.visitFile(file, attrs);
//			}
//		});
//
//		System.out.printf("Cached %d file hashes%n", fileHashes.size());
//
//		Collection<Addon> search = cm.repo().search(game, "MAP", null, null);
//		final Path tmpDir = Files.createTempDirectory("ua-bots");
//
//		List<Map> maps = search.stream()
//							   .filter(c -> !c.deleted && c instanceof Map)
//							   .map(c -> (Map)c)
//							   .filter(c -> !c.bots)

	/// /							   .filter(c -> c.hash.equalsIgnoreCase("9a236ea0398b1111f831959629b0420ac1a1de2c"))
//							   .toList();
//
//		System.out.printf("Processing %d maps%n", maps.size());
//
//		AtomicInteger counter = new AtomicInteger(0);
//		maps.parallelStream().forEach(c -> {
//			if (counter.incrementAndGet() % 100 == 0) System.out.printf("%d/%d%n", counter.get(), maps.size());
//
//			Path[] downloaded = { null };
//
//			try {
//				Addon co = cm.checkout(c.hash);
//				boolean was = ((Map)co).bots;
//
//				Path existing = fileHashes.get(c.hash);
//				if (existing == null) {
//					new LocalMirrorClient.Downloader(c, tmpDir, d -> {
//						System.out.printf("Downloaded %s%n", d.destination);
//						downloaded[0] = d.destination;
//					}).run();
//				}
//
//				Path file = downloaded[0] != null ? downloaded[0] : existing;
//
//				Submission sub = new Submission(file);
//				IndexLog log = new IndexLog();
//				try (Incoming incoming = new Incoming(sub, log).prepare()) {
//					if (!incoming.files(FileType.MAP).isEmpty()) {
//						try (Package pkg = new Package(
//							new PackageReader(incoming.files(FileType.MAP).stream().findFirst().get().asChannel()))) {
//							((Map)co).bots = MapIndexHandler.botSupport(pkg);
//						} catch (Exception e) {
//							//
//						}
//					}
//				} catch (Exception e) {
//					//
//				}
//
//				if (((Map)co).bots != was) {
//					checkinChange(cm, co);
//				} else {
//					System.out.println("No change for " + String.join(" / ", co.game, co.name));
//				}
//			} catch (Throwable e) {
//				//
//			} finally {
//				if (downloaded[0] != null) {
//					try {
//						Files.deleteIfExists(downloaded[0]);
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}
//			}
//		});
//	}
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
			maybeCheckin(cm, co, changed);
		}

	}

	/**
	 * Unreal lings all appear dead.
	 */
	public static void removeVohzdUnrealLinks() throws IOException, InterruptedException {
		ContentManager cm = manager();
		Collection<Addon> search = cm.repo().all();
		for (Addon c : search) {
			Addon co = cm.checkout(c.hash);
			final boolean[] changed = { false };
			for (Download dl : co.downloads) {
				Thread.sleep((long)(250 * Math.random()));
				if (dl.url.contains("files.vohzd.com")
					&& (co.game().equalsIgnoreCase("unreal") || co.game().equalsIgnoreCase("unreal 2"))
					&& dl.state == Download.DownloadState.OK) {

					Util.urlRequest(
						dl.url, "HEAD",
						ok -> System.out.printf("OK: %s%n", co.name()),
						failed -> {
							dl.state = Download.DownloadState.MISSING;
							changed[0] = true;
						}
					);
				}
			}
			maybeCheckin(cm, co, changed[0]);
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

	private record StringField(String name, Function<Addon, String> getter, BiConsumer<Addon, String> setter) {

		String get(Addon content) {
			return getter.apply(content);
		}

		void set(Addon content, String value) {
			setter.accept(content, value);
		}
	}

	private static final List<StringField> STRING_FIELDS = List.of(
		new StringField("name", a -> a.name, (a, v) -> a.name = v),
		new StringField("author", a -> a.author, (a, v) -> a.author = v),
		new StringField("description", a -> a.description, (a, v) -> a.description = v),
		new StringField("title", a -> a instanceof Map m ? m.title : null, (a, v) -> ((Map)a).title = v),
		new StringField("playerCount", a -> a instanceof Map m ? m.playerCount : null, (a, v) -> ((Map)a).playerCount = v)
	);

	/**
	 * Corrects file sizes recorded while {@link Addon#fileSize} was an int, which silently wrapped
	 * for content over 2GB.
	 *
	 * @param hash    content to correct
	 * @param theFile a known-good local copy of the content
	 */
	private static void fixFileSize(String hash, String theFile) throws IOException {
		final Path file = Paths.get(theFile);

		ContentManager cm = manager();
		Addon co = cm.checkout(hash);
		if (co == null) throw new IllegalArgumentException("No content found for hash " + hash);

		if (!Util.hash(file).equals(co.hash)) {
			throw new IllegalArgumentException(String.format("%s is not the content of %s", file, co.name));
		}

		final long size = Files.size(file);
		if (co.fileSize == size) {
			System.out.printf("%s fileSize is already correct%n", co.name);
			return;
		}

		System.out.printf("%s fileSize: %d -> %d%n", co.name, co.fileSize, size);
		co.fileSize = size;
		checkinChange(cm, co);
	}

	/**
	 * Repairs metadata indexed before unreal-package-lib 1.15.8, which baked UT colour markup and
	 * mis-decoded wide strings into stored strings.
	 * <p>
	 * Rather than force a re-index of everything, this only fetches content which currently holds
	 * broken strings, and copies back only the fields which are actually broken. Attachments,
	 * files, downloads, themes, gametypes and index dates are left exactly as they are.
	 */
	private static void fixCorruptStrings() throws IOException {
		initAuthors();

		ContentManager cm = manager();
		// a stable location, so a download which has already been fetched by hand or by a previous
		// run can be reused - some of this content is over a gigabyte
		final Path tmpDir = Files.createDirectories(Paths.get(System.getProperty("java.io.tmpdir"), "ua-strings"));

		List<Addon> affected = cm.repo().all().stream()
								 .filter(c -> !c.deleted && corrupt(c))
								 .sorted(Comparator.comparingLong(a -> a.fileSize))
								 .toList();

		System.out.printf("Found %d entries with corrupt strings%n", affected.size());

		int fixed = 0, skipped = 0;
		for (Addon c : affected) {
			System.out.printf("%s [%s]%n", String.join(" / ", c.game, c.contentType(), c.name), c.hash.substring(0, 8));

			Addon indexed = reindexInMemory(c, tmpDir);
			if (indexed == null) {
				System.out.println("  ! could not re-index");
				skipped++;
			} else {
				Addon co = cm.checkout(c.hash);
				if (applyStrings(co, indexed)) {
					checkinChange(cm, co);
					fixed++;
				} else {
					System.out.println("  - nothing changed");
					skipped++;
				}
			}
		}

		System.out.printf("Fixed %d entries, skipped %d%n", fixed, skipped);
	}

	/**
	 * Download and index content, without checking anything in - we only want to read the strings
	 * the current indexer produces for it.
	 */
	private static Addon reindexInMemory(Addon content, Path tmpDir) {
		final Addon[] result = { null };

		// a download already sitting in tmpDir is reused, as long as it's complete
		new LocalMirrorClient.Downloader(content, tmpDir, d -> {
			if (!Files.exists(d.destination)) return;

			try (Incoming incoming = new Incoming(new Submission(d.destination), new IndexLog()).prepare()) {
				AddonClassifier.AddonIdentifier ident = AddonClassifier.identifierForType(
					SimpleAddonType.valueOf(content.contentType));
				ident.indexer().get().index(incoming, AddonClassifier.newContent(ident, incoming), r -> {
					// the same normalisation a real re-index would apply after the handler runs
					IndexUtils.cleanStrings(r.content);
					result[0] = r.content;

					// we're only after the strings, so throw away any images which were generated
					for (IndexResult.NewAttachment f : r.files) {
						try {
							Files.deleteIfExists(f.path());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
			} catch (Throwable t) {
				System.out.printf("  ! failed to index %s: %s%n", d.destination.getFileName(), t);
			} finally {
				try {
					// keep the download when indexing failed, so it can be inspected or retried
					// without fetching it all over again
					if (result[0] != null) Files.deleteIfExists(d.destination);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).run();

		return result[0];
	}

	private static boolean applyStrings(Addon existing, Addon indexed) {
		boolean changed = false;

		for (StringField f : STRING_FIELDS) {
			if (corrupt(f.get(existing))) {
				changed |= replaceString(f.name(), f.get(existing), f.get(indexed), v -> f.set(existing, v));
			}
		}

		// map packs carry their own per-map strings
		if (existing instanceof MapPack was && indexed instanceof MapPack now) {
			for (MapPack.PackMap m : was.maps) {
				MapPack.PackMap fresh = now.maps.stream()
												.filter(n -> n.name.equalsIgnoreCase(m.name))
												.findFirst().orElse(null);
				if (fresh == null) continue;
				if (corrupt(m.title)) changed |= replaceString(m.name + ".title", m.title, fresh.title, v -> m.title = v);
				if (corrupt(m.author)) changed |= replaceString(m.name + ".author", m.author, fresh.author, v -> m.author = v);
			}
		}

		return changed;
	}

	private static boolean replaceString(String field, String was, String now, Consumer<String> setter) {
		if (now == null || now.isBlank() || corrupt(now)) {
			System.out.printf("  ! %s is still unusable after re-index, leaving alone: [%s]%n", field, escaped(now));
			return false;
		}
		setter.accept(now);
		System.out.printf("  %s: [%s] -> [%s]%n", field, escaped(was), now);
		return true;
	}

	private static boolean corrupt(Addon content) {
		if (STRING_FIELDS.stream().anyMatch(f -> corrupt(f.get(content)))) return true;
		return content instanceof MapPack p
			   && p.maps.stream().anyMatch(m -> corrupt(m.name) || corrupt(m.title) || corrupt(m.author));
	}

	/**
	 * Control characters are never legitimate content; they're markup or decoding damage.
	 */
	private static boolean corrupt(String s) {
		if (s == null) return false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c < 0x20 && c != '\n' && c != '\t') return true;
		}
		return false;
	}

	private static String escaped(String s) {
		if (s == null) return "null";
		StringBuilder out = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c < 0x20) out.append(String.format("\\x%02x", (int)c));
			else out.append(c);
		}
		return out.toString();
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

	private static void fixMissingScreenshots() throws IOException {

		final LocalDate dateFrom = LocalDate.parse("2025-03-12");
		final LocalDate dateTo = LocalDate.parse("2025-03-15");

		final CLI cli = CLI.parse();
		final DataStore imageStore = store(DataStore.StoreContent.IMAGES, cli);
		final DataStore contentStore = store(DataStore.StoreContent.CONTENT, cli);

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
					result.content.name = before.name;
					result.content.description = before.description;
					result.content.game = before.game;
					result.content.author = before.author;
					result.content.variationOf = before.variationOf;

					if (result.content instanceof Map m) m.gametype = ((Map)before).gametype;
					if (result.content instanceof MapPack p) p.gametype = ((MapPack)before).gametype;
				}
			}
		});

		Collection<Addon> search = cm.repo().all(true);
		final Path tmpDir = Files.createTempDirectory("ua-pics");

		final List<Addon> contents = search.stream()
										   .filter(c -> !c.deleted)
										   .filter(c ->
													   c.attachments.isEmpty() &&
													   (c.firstIndex.toLocalDate().isAfter(dateFrom) &&
														c.firstIndex.toLocalDate().isBefore(dateTo))
										   )
										   .sorted(Comparator.comparingLong(c -> c.fileSize))
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
