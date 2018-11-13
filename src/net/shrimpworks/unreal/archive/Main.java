package net.shrimpworks.unreal.archive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.indexer.Content;
import net.shrimpworks.unreal.archive.indexer.ContentManager;
import net.shrimpworks.unreal.archive.indexer.ContentType;
import net.shrimpworks.unreal.archive.indexer.IndexResult;
import net.shrimpworks.unreal.archive.indexer.Indexer;
import net.shrimpworks.unreal.archive.indexer.Scanner;
import net.shrimpworks.unreal.archive.scraper.AutoIndexPHPScraper;
import net.shrimpworks.unreal.archive.scraper.Downloader;
import net.shrimpworks.unreal.archive.scraper.FPSNetwork;
import net.shrimpworks.unreal.archive.scraper.GameBanana;
import net.shrimpworks.unreal.archive.scraper.GameFrontOnline;
import net.shrimpworks.unreal.archive.scraper.GameZooMaps;
import net.shrimpworks.unreal.archive.scraper.UTTexture;
import net.shrimpworks.unreal.archive.scraper.UnrealPlayground;
import net.shrimpworks.unreal.archive.storage.DataStore;
import net.shrimpworks.unreal.archive.www.FileDetails;
import net.shrimpworks.unreal.archive.www.Index;
import net.shrimpworks.unreal.archive.www.MapPacks;
import net.shrimpworks.unreal.archive.www.Maps;
import net.shrimpworks.unreal.archive.www.Templates;
import net.shrimpworks.unreal.packages.Umod;

public class Main {

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
			case "www":
				www(contentManager(cli), cli);
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
				System.out.printf("Command \"%s\" has not been implemented!", cli.commands()[0]);
		}

		System.exit(0);
	}

	private static ContentManager contentManager(CLI cli) throws IOException {
		if (cli.option("content-path", null) == null) {
			System.err.println("content-path must be specified!");
			System.exit(2);
		}

		Path contentPath = Paths.get(cli.option("content-path", null));
		if (!Files.isDirectory(contentPath)) {
			System.err.println("content-path must be a directory!");
			System.exit(3);
		}

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
		final ContentManager contentManager = new ContentManager(contentPath, contentStore, imageStore, attachmentStore);
		System.err.printf("Loaded content index with %d items in %.2fs%n",
						  contentManager.size(), (System.currentTimeMillis() - start) / 1000f);

		return contentManager;
	}

	private static DataStore store(DataStore.StoreContent contentType, CLI cli) {
		String stringType = cli.option(contentType.name().toLowerCase() + "-store", cli.option("store", null));
		if (stringType == null) {
			System.err.println(contentType.name().toLowerCase() + "-store or store must be specified!");
			System.err.println("Valid options are: " + Arrays.stream(DataStore.StoreType.values())
															 .map(Enum::name)
															 .collect(Collectors.joining(", ")));
			System.exit(3);
		}

		DataStore.StoreType storeType = DataStore.StoreType.valueOf(stringType.toUpperCase());

		return storeType.newStore(contentType, cli);
	}

	private static void index(ContentManager contentManager, CLI cli) throws IOException {
		if (cli.commands().length < 2) {
			System.err.println("An index path must be specified!");
			System.exit(2);
		}

		boolean force = Boolean.valueOf(cli.option("force", "false"));
		ContentType forceType = (!cli.option("type", "").isEmpty()) ? ContentType.valueOf(cli.option("type", "").toUpperCase()) : null;

		Indexer indexer = new Indexer(contentManager, cli);

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
			paths = Arrays.stream(cli.commands(), 1, cli.commands().length)
						  .map(s -> Paths.get(s))
						  .peek(p -> {
							  if (!Files.exists(p)) {
								  System.err.println("Input path does not exist: " + p.toString());
								  System.exit(4);
							  }
						  })
						  .toArray(Path[]::new);
		}

		indexer.index(force, forceType, paths);
	}

	private static void scan(ContentManager contentManager, CLI cli) throws IOException {
		if (cli.commands().length < 2) {
			System.err.println("An input path must be specified!");
			System.exit(2);
		}

		Scanner scanner = new Scanner(contentManager, cli);

		Path[] paths = Arrays.stream(cli.commands(), 1, cli.commands().length)
							 .map(s -> Paths.get(s))
							 .peek(p -> {
								 if (!Files.exists(p)) {
									 System.err.println("Input path does not exist: " + p.toString());
									 System.exit(4);
								 }
							 })
							 .toArray(Path[]::new);

		scanner.scan(paths);
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

		FileTime fileTime = Files.getLastModifiedTime(yaml);
		Process editor = new ProcessBuilder("sensible-editor", yaml.toString()).inheritIO().start();
		int res = editor.waitFor();
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

	private static void www(ContentManager contentManager, CLI cli) throws IOException {
		if (cli.commands().length < 2) {
			System.err.println("An output path must be specified!");
			System.exit(2);
		}

		Path outputPath = Paths.get(cli.commands()[1]);
		if (!Files.exists(outputPath)) {
			System.out.println("Creating directory " + outputPath);
			Files.createDirectories(outputPath);
		} else if (!Files.isDirectory(outputPath)) {
			System.err.println("Output path must be a directory!");
			System.exit(4);
		}

		Path staticOutput = outputPath.resolve("static");

		long start = System.currentTimeMillis();

		Templates.unpackResourceZip("static.zip", Files.createDirectories(staticOutput));

		AtomicInteger pageCount = new AtomicInteger();

		Arrays.asList(
				new Index(contentManager, outputPath, staticOutput),
				new Maps(contentManager, outputPath, staticOutput),
				new MapPacks(contentManager, outputPath, staticOutput),
				new FileDetails(contentManager, outputPath, staticOutput)
		).forEach(g -> pageCount.addAndGet(g.generate()));

		System.out.printf("Output %d pages in %.2fs%n", pageCount.get(), (System.currentTimeMillis() - start) / 1000f);
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
		System.out.println("    Index the contents of files, writing the results to <content-path>.");
		System.out.println("    Optionally force re-indexing of existing content, rather than skipping it.");
		System.out.println("  scan <file ...> --content-path=<path>");
		System.out.println("    Dry-run scan the contents of files, comparing to known content where possible.");
		System.out.println("  edit <hash> --content-path=<path>");
		System.out.println("    Edit the metadata for the <hash> provided. Relies on `sensible-editor` on Linux.");
		System.out.println("  www <output-path> --content-path=<path>");
		System.out.println("    Generate the HTML website for browsing content.");
		System.out.println("  summary --content-path=<path>");
		System.out.println("    Show stats and counters for the content index in <content-path>");
		System.out.println("  ls [--game=<game>] [--type=<type>] [--author=<author>] --content-path=<path>");
		System.out.println("    List indexed content in <content-path>, filtered by game, type or author");
		System.out.println("  show [name ...] [hash ...] --content-path=<path>");
		System.out.println("    Show data for the content items specified");
		System.out.println("  refresh --content-path=<path>");
		System.out.println("    Perform a liveliness check of all download URLs");
		System.out.println("  mirror <output-path> --content-path=<path>");
		System.out.println("    Download all content in the index to <output-path>");
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
