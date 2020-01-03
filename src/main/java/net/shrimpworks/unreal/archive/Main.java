package net.shrimpworks.unreal.archive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.ContentManager;
import net.shrimpworks.unreal.archive.content.ContentType;
import net.shrimpworks.unreal.archive.content.IndexResult;
import net.shrimpworks.unreal.archive.content.Indexer;
import net.shrimpworks.unreal.archive.content.Scanner;
import net.shrimpworks.unreal.archive.docs.DocumentManager;
import net.shrimpworks.unreal.archive.managed.Managed;
import net.shrimpworks.unreal.archive.managed.ManagedContentManager;
import net.shrimpworks.unreal.archive.mirror.MirrorClient;
import net.shrimpworks.unreal.archive.scraper.AutoIndexPHPScraper;
import net.shrimpworks.unreal.archive.scraper.Downloader;
import net.shrimpworks.unreal.archive.scraper.FPSNetwork;
import net.shrimpworks.unreal.archive.scraper.GameBanana;
import net.shrimpworks.unreal.archive.scraper.GameFrontOnline;
import net.shrimpworks.unreal.archive.scraper.GameZooMaps;
import net.shrimpworks.unreal.archive.scraper.UTTexture;
import net.shrimpworks.unreal.archive.scraper.UnrealPlayground;
import net.shrimpworks.unreal.archive.storage.DataStore;
import net.shrimpworks.unreal.archive.www.Documents;
import net.shrimpworks.unreal.archive.www.Index;
import net.shrimpworks.unreal.archive.www.MESSubmitter;
import net.shrimpworks.unreal.archive.www.ManagedContent;
import net.shrimpworks.unreal.archive.www.Search;
import net.shrimpworks.unreal.archive.www.SiteFeatures;
import net.shrimpworks.unreal.archive.www.SiteMap;
import net.shrimpworks.unreal.archive.www.Submit;
import net.shrimpworks.unreal.archive.www.Templates;
import net.shrimpworks.unreal.archive.www.content.FileDetails;
import net.shrimpworks.unreal.archive.www.content.Latest;
import net.shrimpworks.unreal.archive.www.content.MapPacks;
import net.shrimpworks.unreal.archive.www.content.Maps;
import net.shrimpworks.unreal.archive.www.content.Models;
import net.shrimpworks.unreal.archive.www.content.Mutators;
import net.shrimpworks.unreal.archive.www.content.Skins;
import net.shrimpworks.unreal.archive.www.content.Voices;
import net.shrimpworks.unreal.packages.Umod;

public class Main {

	private static final String CONTENT_DIR = "content";
	private static final String DOCUMENTS_DIR = "documents";

	private static final Path TMP = Paths.get(System.getProperty("java.io.tmpdir"));
	private static final String CONTENT_URL = System.getenv().getOrDefault("UA_CONTENT_URL",
																		   "https://github.com/unreal-archive/unreal-archive-data/archive/master.zip");

	public static void main(String[] args) throws IOException, InterruptedException {
		final CLI cli = CLI.parse(Collections.emptyMap(), args);

		if (cli.commands().length == 0) {
			usage();
			System.exit(1);
		}

		switch (cli.commands()[0].toLowerCase()) {
			case "index":
				index(contentManager(cli), cli);
				break;
			case "scan":
				scan(contentManager(cli), cli);
				break;
			case "edit":
				edit(contentManager(cli), cli);
				break;
			case "sync":
				sync(cli);
				break;
			case "mirror":
				mirror(contentManager(cli), cli);
				break;
			case "www":
				www(contentManager(cli), documentManager(cli), managedContent(cli, "updates"), cli);
				break;
			case "search-submit":
				searchSubmit(contentManager(cli), documentManager(cli), managedContent(cli, "updates"), cli);
				break;
			case "summary":
				summary(contentManager(cli));
				break;
			case "ls":
				list(contentManager(cli), cli);
				break;
			case "show":
				show(contentManager(cli), cli);
				break;
			case "scrape":
				scrape(cli);
				break;
			case "download":
				download(cli);
				break;
			case "unpack":
				unpack(cli);
				break;
			default:
				System.out.printf("Command \"%s\" has not been implemented!%n%n", cli.commands()[0]);
				usage();
		}

		System.exit(0);
	}

	private static String userPrompt(String prompt, String defaultValue) {
		System.out.println(prompt);
		System.out.print("> ");
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
			String in = reader.readLine().trim();
			if (in.isEmpty()) return defaultValue;
			else return in;
		} catch (IOException e) {
			System.err.println("Failed to read user input: " + e.toString());
			System.exit(254);
		}
		return defaultValue;
	}

	private static Path contentPath(CLI cli) throws IOException {
		if (cli.option("content-path", null) == null) {
			final String opt = userPrompt("content-path not specified. Download and use read-only content data? [Y/n]", "y").toLowerCase();
			if (opt.equalsIgnoreCase("y")) {
				Path tmpPath = TMP.resolve("ua-tmp");
				if (!Files.exists(tmpPath) || !Files.isDirectory(tmpPath)) {
					Files.createDirectories(tmpPath);
					Path tmpFile = tmpPath.resolve("archive.zip");
					System.out.printf("Downloading archive from %s... ", CONTENT_URL);
					Util.downloadTo(Util.toUriString(CONTENT_URL), tmpFile);
					System.out.println("Done");
					try {
						System.out.printf("Extracting archive from %s to %s... ", tmpFile.toString(), tmpPath.toString());
						ArchiveUtil.extract(tmpFile, tmpPath, Duration.ofMinutes(10));
						System.out.println("Done");
					} catch (Throwable e) {
						// make sure to clean out the path, so we can try again next time
						ArchiveUtil.cleanPath(tmpPath);
						throw new IOException("Failed to extract downloaded content archive", e);
					}
				}

				// find the content directory within the extracted stuff
				Optional<Path> contentParent = Files.walk(tmpPath, 3, FileVisitOption.FOLLOW_LINKS)
													.filter(p -> Files.isDirectory(p) && p.getFileName().toString().equals(CONTENT_DIR))
													.map(Path::getParent)
													.findFirst();

				cli.putOption("content-path", contentParent.orElseThrow(IllegalArgumentException::new).toString());
			} else {
				System.err.println("content-path must be specified!");
				System.exit(2);
			}
		}

		Path contentPath = Paths.get(cli.option("content-path", null));
		if (!Files.isDirectory(contentPath)) {
			System.err.println("content-path must be a directory!");
			System.exit(3);
		}

		return contentPath;
	}

	private static Path[] cliPaths(CLI cli, int fromOffset) throws IOException {
		// let's see if there are cli paths which are actually URLs, and download them to local paths
		String[] urls = Arrays.stream(cli.commands(), fromOffset, cli.commands().length)
							  .filter(s -> s.matches("^https?://.*"))
							  .toArray(String[]::new);

		List<Path> dlPaths = new ArrayList<>();
		if (urls.length > 0) {
			Path dlTemp = Files.createTempDirectory("ua-download");
			for (String url : urls) {
				System.out.printf("Fetching %s ... ", url);
				dlPaths.add(Util.downloadTo(url, dlTemp));
				System.out.println("Done");
			}
		}

		List<Path> diskPaths = Arrays.stream(cli.commands(), fromOffset, cli.commands().length)
									 .filter(s -> !s.matches("^https?://.*"))
									 .map(s -> Paths.get(s))
									 .peek(p -> {
										 if (!Files.exists(p)) {
											 System.err.println("Input path does not exist: " + p.toString());
											 System.exit(4);
										 }
									 })
									 .collect(Collectors.toList());

		return Stream.concat(dlPaths.stream(), diskPaths.stream()).toArray(Path[]::new);
	}

	private static ContentManager contentManager(CLI cli) throws IOException {
		Path contentPath = contentPath(cli);

		final DataStore imageStore = store(DataStore.StoreContent.IMAGES, cli);
		final DataStore attachmentStore = store(DataStore.StoreContent.ATTACHMENTS, cli);
		final DataStore contentStore = store(DataStore.StoreContent.CONTENT, cli);

		// prepare cleanup
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				imageStore.close();
				attachmentStore.close();
				contentStore.close();
			} catch (IOException e) {
				//
			}
		}));

		final long start = System.currentTimeMillis();
		final ContentManager contentManager = new ContentManager(contentPath.resolve(CONTENT_DIR), contentStore, imageStore,
																 attachmentStore);
		final double gigs = (contentManager.fileSize() / 1024d / 1024d / 1024d);
		System.err.printf("Loaded content index with %d items (%.2fGB) in %.2fs%n",
						  contentManager.size(), gigs, (System.currentTimeMillis() - start) / 1000f);

		return contentManager;
	}

	private static DocumentManager documentManager(CLI cli) throws IOException {
		Path contentPath = contentPath(cli);

		final long start = System.currentTimeMillis();
		final DocumentManager documentManager = new DocumentManager(contentPath.resolve(DOCUMENTS_DIR));
		System.err.printf("Loaded document index with %d items in %.2fs%n",
						  documentManager.size(), (System.currentTimeMillis() - start) / 1000f);

		return documentManager;
	}

	private static ManagedContentManager managedContent(CLI cli, String group) throws IOException {
		Path contentPath = contentPath(cli);

		Path managedPath = contentPath.resolve(group);
		if (!Files.isDirectory(managedPath)) {
			System.err.printf("Can't find content path for group %s!%n", group);
			System.exit(3);
		}

		final long start = System.currentTimeMillis();
		ManagedContentManager managedContentManager = new ManagedContentManager(managedPath, group);
		System.err.printf("Loaded managed content [%s] index with %d items in %.2fs%n",
						  group, managedContentManager.size(), (System.currentTimeMillis() - start) / 1000f);

		return managedContentManager;
	}

	private static void sync(CLI cli) throws IOException {
		ManagedContentManager managedContent = managedContent(cli, cli.commands()[1]);

		final DataStore contentStore = store(DataStore.StoreContent.CONTENT, cli);
		Set<Managed> synced = managedContent.sync(contentStore);

		if (synced.isEmpty()) {
			System.out.println("No files were synced.");
		} else {
			System.out.printf("Synced %d files:%n", synced.size());
			synced.forEach(m -> System.out.printf(" - %s%n", m.title));
		}
	}

	public static DataStore store(DataStore.StoreContent contentType, CLI cli) {
		String stringType = cli.option(contentType.name().toLowerCase() + "-store", cli.option("store", null));
		if (stringType == null) {
			System.err.printf("No %s store specified, this will be necessary for indexing new content. Falling back to no-op store.%n",
							  contentType.name().toLowerCase());
			stringType = "NOP";
		}

		DataStore.StoreType storeType = DataStore.StoreType.valueOf(stringType.toUpperCase());

		return storeType.newStore(contentType, cli);
	}

	private static void index(ContentManager contentManager, CLI cli) throws IOException {
		if (cli.commands().length < 2) {
			System.err.println("An index path must be specified!");
			System.exit(2);
		}

		boolean verbose = Boolean.parseBoolean(cli.option("verbose", "true"));
		boolean force = Boolean.parseBoolean(cli.option("force", "false"));
		ContentType forceType = (!cli.option("type", "").isEmpty()) ? ContentType.valueOf(cli.option("type", "").toUpperCase()) : null;

		Indexer indexer = new Indexer(contentManager, new Indexer.CLIEventPrinter(verbose));

		Path[] paths;

		// read file set from stdin
		if (cli.commands()[1].equals("-")) {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
				String l;
				List<Path> inPaths = new ArrayList<>();
				while ((l = br.readLine()) != null) {
					Path p = Paths.get(l);
					if (!Files.exists(p)) {
						System.err.println("Input path does not exist: " + p.toString());
						System.exit(4);
					}
					inPaths.add(p);
				}
				paths = inPaths.toArray(new Path[0]);
			}
		} else {
			paths = cliPaths(cli, 1);
		}

		indexer.index(force, forceType, paths);
	}

	private static void scan(ContentManager contentManager, CLI cli) throws IOException {
		if (cli.commands().length < 2) {
			System.err.println("An input path must be specified!");
			System.exit(2);
		}

		Scanner scanner = new Scanner(contentManager, cli);

		Path[] paths = cliPaths(cli, 1);

		scanner.scan(new Scanner.CLIEventPrinter(), paths);
	}

	private static void edit(ContentManager contentManager, CLI cli) throws IOException, InterruptedException {
		if (cli.commands().length < 2) {
			System.err.println("A content hash should be provided!");
			System.exit(2);
		}

		Content content = contentManager.forHash(cli.commands()[1]);
		if (content == null) {
			System.err.println("Content for provided hash does not exist!");
			System.exit(4);
		}

		Path yaml = Files.write(Files.createTempFile(content.hash, ".yml"), YAML.toString(content).getBytes(StandardCharsets.UTF_8),
								StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

		String editor = System.getenv().getOrDefault("UA_EDITOR", "sensible-editor");

		FileTime fileTime = Files.getLastModifiedTime(yaml);
		Process editorProcess = new ProcessBuilder(editor, yaml.toString()).inheritIO().start();
		int res = editorProcess.waitFor();
		if (res == 0) {
			if (!fileTime.equals(Files.getLastModifiedTime(yaml))) {
				Content updated = YAML.fromFile(yaml, Content.class);
				if (contentManager.checkin(new IndexResult<>(updated, Collections.emptySet()), null)) {
					System.out.println("Stored changes!");
				} else {
					System.out.println("Failed to apply");
				}
			} else {
				System.out.println("No changes!");
			}
		}
	}

	private static void mirror(ContentManager contentManager, CLI cli) throws IOException {
		if (cli.commands().length < 2) {
			System.err.println("An output path should be provided!");
			System.exit(2);
		}

		Path output = Files.createDirectories(Paths.get(cli.commands()[1])).toAbsolutePath();

		System.out.printf("Writing files to %s with concurrency of %s%n",
						  output.toString(), cli.option("concurrency", "3"));

		MirrorClient mirror = new MirrorClient(
				contentManager,
				output,
				Integer.parseInt(cli.option("concurrency", "3")),
				((total, remaining, last) -> System.out.printf("\r[ %-6s / %-6s ] Processed %-40s",
															   total - remaining, total, last.originalFilename))
		);
		mirror.mirror();

		System.out.println("Mirror completed");

		// cleanup executor
		mirror.cancel();
	}

	private static void www(ContentManager contentManager, DocumentManager documentManager, ManagedContentManager updates, CLI cli)
			throws IOException {
		if (cli.commands().length < 2) {
			System.err.println("An output path must be specified!");
			System.exit(2);
		}

		final Path outputPath = Paths.get(cli.commands()[1]).toAbsolutePath();
		if (!Files.exists(outputPath)) {
			System.out.println("Creating directory " + outputPath);
			Files.createDirectories(outputPath);
		} else if (!Files.isDirectory(outputPath)) {
			System.err.println("Output path must be a directory!");
			System.exit(4);
		}

		final boolean withSearch = Boolean.parseBoolean(cli.option("with-search", "false"));
		final boolean withSubmit = Boolean.parseBoolean(cli.option("with-submit", "false"));
		final boolean withLatest = Boolean.parseBoolean(cli.option("with-latest", "false"));
		final boolean localImages = Boolean.parseBoolean(cli.option("local-images", "false"));
		if (localImages) System.out.println("Will download a local copy of content images, this will take additional time.");

		final SiteFeatures features = new SiteFeatures(localImages, withLatest, withSubmit, withSearch);

		final Path staticOutput = outputPath.resolve("static");

		final long start = System.currentTimeMillis();

		// unpack static content
		Templates.unpackResources("static.list", Files.createDirectories(staticOutput).getParent());

		final Set<SiteMap.Page> allPages = ConcurrentHashMap.newKeySet();

		if (cli.commands().length == 2 || (cli.commands().length > 2 && cli.commands()[2].equalsIgnoreCase("content"))) {
			// generate content pages
			Arrays.asList(
					new Maps(contentManager, outputPath, staticOutput, features),
					new MapPacks(contentManager, outputPath, staticOutput, features),
					new Skins(contentManager, outputPath, staticOutput, features),
					new Models(contentManager, outputPath, staticOutput, features),
					new Voices(contentManager, outputPath, staticOutput, features),
					new Mutators(contentManager, outputPath, staticOutput, features),
					new FileDetails(contentManager, outputPath, staticOutput, features)
			).parallelStream().forEach(g -> {
				System.out.printf("Generating %s pages%n", g.getClass().getSimpleName());
				allPages.addAll(g.generate());
			});
		}

		if (cli.commands().length == 2 || (cli.commands().length > 2 && cli.commands()[2].equalsIgnoreCase("docs"))) {
			// generate document pages
			System.out.println("Generating Document pages");
			allPages.addAll(new Documents(documentManager, outputPath, staticOutput, features).generate());
		}

		if (cli.commands().length == 2 || (cli.commands().length > 2 && cli.commands()[2].equalsIgnoreCase("updates"))) {
			// generate updates pages
			System.out.println("Generating Updates pages");
			allPages.addAll(new ManagedContent(updates, outputPath, staticOutput, features, "Patches & Updates").generate());
		}

		// generate index
		System.out.println("Generating index page");
		allPages.addAll(new Index(contentManager, documentManager, updates, outputPath, staticOutput, features).generate());

		if (withSubmit) {
			System.out.println("Generating submit page");
			allPages.addAll(new Submit(outputPath, staticOutput, features).generate());
		}

		if (withSearch) {
			System.out.println("Generating search page");
			allPages.addAll(new Search(outputPath, staticOutput, features).generate());
		}

		if (withLatest) {
			System.out.println("Generating latest files");
			allPages.addAll(new Latest(contentManager, outputPath, staticOutput, features).generate());
		}

		System.out.println("Generating sitemap");
		allPages.addAll(SiteMap.siteMap(SiteMap.SITE_ROOT, outputPath, allPages, 50000, features).generate());

		System.out.printf("Output %d pages in %.2fs%n", allPages.size(), (System.currentTimeMillis() - start) / 1000f);
	}

	private static void searchSubmit(ContentManager contentManager, DocumentManager documentManager, ManagedContentManager updates,
									 CLI cli) throws IOException {
		// TODO documents and updates

		final long start = System.currentTimeMillis();

		System.out.printf("Submitting content to search instance at %s%n", System.getenv().getOrDefault("MSE_URL", ""));
		new MESSubmitter(contentManager,
						 System.getenv().getOrDefault("SITE_URL", ""),
						 System.getenv().getOrDefault("MSE_URL", ""),
						 System.getenv().getOrDefault("MSE_TOKEN", ""), 10)
				.submit(percent -> System.out.printf("\r%.1f%% complete", percent * 100d),
						done -> System.out.printf("%nSearch submission complete in %.2fs%n", (System.currentTimeMillis() - start) / 1000f));
	}

	private static void summary(ContentManager contentManager) {
		Map<Class<? extends Content>, Long> byType = contentManager.countByType();
		if (byType.size() > 0) {
			System.out.println("Current content by Type:");
			byType.forEach((type, count) -> System.out.printf(" > %s: %d%n", type.getSimpleName(), count));

			System.out.println("Current content by Game:");
			contentManager.countByGame().forEach((game, count) -> System.out.printf(" > %s: %d%n", game, count));
		} else {
			System.out.println("No content stored yet");
		}
	}

	private static void list(ContentManager contentManager, CLI cli) {
		String game = cli.option("game", null);
		String type = cli.option("type", null);
		String author = cli.option("author", null);
		String name = cli.option("name", null);

		if (null == game && type == null && author == null && name == null) {
			System.err.println("Options to search by game, type, author or name are expected");
			System.exit(255);
		}

		Set<Content> results = new HashSet<>(contentManager.search(game, type, name, author));

		if (results.isEmpty()) {
			System.out.println("No results found");
		} else {
			System.out.printf("%-22s | %-10s | %-30s | %-20s | %s%n", "Game", "Type", "Name", "Author", "Hash");
			for (Content result : results) {
				System.out.printf("%-22s | %-10s | %-30s | %-20s | %s%n",
								  result.game, result.contentType,
								  result.name.substring(0, Math.min(20, result.name.length())),
								  result.author.substring(0, Math.min(20, result.author.length())),
								  result.hash);
			}
		}
	}

	private static void show(ContentManager contentManager, CLI cli) throws IOException {
		if (cli.commands().length < 2) {
			System.err.println("List of content hashes or names expected");
			System.exit(255);
		}

		Set<Content> results = new HashSet<>();

		String[] terms = Arrays.copyOfRange(cli.commands(), 1, cli.commands().length);
		for (String term : terms) {
			if (term.matches("[a-f0-9]{40}")) {
				Content found = contentManager.forHash(term);
				if (found != null) results.add(found);
			} else {
				results.addAll(contentManager.forName(term));
			}
		}

		if (results.isEmpty()) {
			System.out.printf("No results for terms %s found%n", Arrays.toString(terms));
		} else {
			for (Content result : results) {
				System.out.println(YAML.toString(result));
			}
		}
	}

	private static void scrape(CLI cli) throws IOException {
		switch (cli.commands()[1]) {
			case "autoindexphp":
				AutoIndexPHPScraper.index(cli);
				break;
			case "unrealplayground":
				UnrealPlayground.index(cli);
				break;
			case "gamezoo":
				GameZooMaps.index(cli);
				break;
			case "gamefrontonline":
				GameFrontOnline.index(cli);
				break;
			case "fpsnetwork":
				FPSNetwork.index(cli);
				break;
			case "uttexture":
				UTTexture.index(cli);
				break;
			case "gamebanana":
				GameBanana.index(cli);
				break;
			default:
				throw new UnsupportedOperationException("Scraper not supported: " + cli.commands()[1]);
		}

	}

	private static void download(CLI cli) throws IOException {
		if (cli.commands().length < 3) {
			System.err.println("An input file and output directory are required");
			System.exit(255);
		}

		Downloader.download(cli);
	}

	private static void unpack(CLI cli) throws IOException {
		if (cli.commands().length < 3) {
			System.err.println("A Umod file and destination directory are required!");
			System.exit(2);
		}

		Path umodFile = Paths.get(cli.commands()[1]);
		if (!Files.exists(umodFile)) {
			System.err.println("Umod file does not exist!");
			System.exit(4);
		}

		Path dest = Paths.get(cli.commands()[2]);
		if (!Files.isDirectory(dest)) {
			System.err.println("Destination directory does not exist!");
			System.exit(4);
		}

		Umod umod = new Umod(umodFile);
		ByteBuffer buffer = ByteBuffer.allocate(1024 * 8);
		for (Umod.UmodFile f : umod.files) {
			if (f.name.startsWith("System\\Manifest")) continue;

			System.out.printf("Unpacking %s ", f.name);
			Path out = dest.resolve(Util.filePath(f.name));

			if (!Files.exists(out)) Files.createDirectories(out);

			out = out.resolve(Util.fileName(f.name));

			System.out.printf("to %s%n", out);

			try (FileChannel fileChannel = FileChannel.open(out, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
															StandardOpenOption.TRUNCATE_EXISTING);
				 SeekableByteChannel fileData = f.read()) {

				while (fileData.read(buffer) > 0) {
					fileData.read(buffer);
					buffer.flip();
					fileChannel.write(buffer);
					buffer.clear();
				}
			}
		}
	}

	private static void usage() {
		System.out.println("Unreal Archive");
		System.out.println("Usage: unreal-archive.jar <command> [options]");
		System.out.println();
		System.out.println("Commands:");
		System.out.println("  index <file ...> --content-path=<path> [--force=<true|false>]");
		System.out.println("    Index the contents of files or paths, writing the results to <content-path>.");
		System.out.println("    Optionally force re-indexing of existing content, rather than skipping it.");
		System.out.println("  sync <kind> --content-path=<path>");
		System.out.println("    Sync managed files' local files to remote storage.");
		System.out.println("  scan <file ...> --content-path=<path>");
		System.out.println("    Dry-run scan the contents of files or paths, comparing to known content where possible.");
		System.out.println("  edit <hash> --content-path=<path>");
		System.out.println("    Edit the metadata for the <hash> provided. Relies on `sensible-editor` on Linux.");
		System.out.println("  mirror <output-path> --content-path=<path> [--concurrency=<count>]");
		System.out.println("    Create a local mirror of the content in <content-path> in local directory <output-path>.");
		System.out.println("    Optionally specify the number of concurrent downloads via <count>, defaults to 3.");
		System.out.println("  www <output-path> [docs|content] --content-path=<path>");
		System.out.println("    Generate the HTML website for browsing content.");
		System.out.println("  summary --content-path=<path>");
		System.out.println("    Show stats and counters for the content index in <content-path>");
		System.out.println("  ls [--game=<game>] [--type=<type>] [--author=<author>] --content-path=<path>");
		System.out.println("    List indexed content in <content-path>, filtered by game, type or author");
		System.out.println("  show [name ...] [hash ...] --content-path=<path>");
		System.out.println("    Show data for the content items specified");
		System.out.println("  unpack <umod-file> <destination>");
		System.out.println("    Unpack the contents of <umod-file> to directory <destination>");
		System.out.println("  scrape <type> [parameters ...] [--slowdown=<millis>]");
		System.out.println("    Scrape file listings from the provided URL, <type> is the type of scraper ");
		System.out.println("    to use ('autoindexphp', 'unrealplayground', 'gamezoo' supported).");
		System.out.println("    [slowdown] will cause the scraper to pause between page loads, defaults to 2000ms.");
		System.out.println("  download <file-list> <output-path> [--slowdown=<millis>]");
		System.out.println("    Download previously-scraped files defined in the file <file-list>, and write");
		System.out.println("    them out to <output-path>, along with a YML file containing the original URL.");
		System.out.println("    [slowdown] will cause the downloader to pause between downloads, defaults to 2000ms.");
	}
}
