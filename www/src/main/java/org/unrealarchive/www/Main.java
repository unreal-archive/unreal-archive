package org.unrealarchive.www;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

import org.unrealarchive.common.CLI;
import org.unrealarchive.common.Version;
import org.unrealarchive.content.AuthorRepository;
import org.unrealarchive.content.addons.GameTypeRepository;
import org.unrealarchive.content.addons.SimpleAddonRepository;
import org.unrealarchive.content.docs.DocumentRepository;
import org.unrealarchive.content.managed.ManagedContentRepository;
import org.unrealarchive.content.wiki.WikiRepository;
import org.unrealarchive.www.content.Announcers;
import org.unrealarchive.www.content.Authors;
import org.unrealarchive.www.content.FileDetails;
import org.unrealarchive.www.content.GameTypes;
import org.unrealarchive.www.content.Latest;
import org.unrealarchive.www.content.MapPacks;
import org.unrealarchive.www.content.Maps;
import org.unrealarchive.www.content.Models;
import org.unrealarchive.www.content.Mutators;
import org.unrealarchive.www.content.Packages;
import org.unrealarchive.www.content.Skins;
import org.unrealarchive.www.content.Voices;
import org.unrealarchive.www.features.Search;
import org.unrealarchive.www.features.Submit;
import org.unrealarchive.www.features.UmodRepack;

import static org.unrealarchive.content.RepoFactory.*;

public class Main {

	static {
		// prepare the version
		Version.setVersion(Main.class);
	}

	public static void main(String[] args) throws IOException {
		System.err.printf("Unreal Archive WWW version %s%n", Version.version());

		final CLI cli = CLI.parse(Collections.emptyMap(), args);

		if (cli.commands().length == 0) {
			usage();
			System.exit(1);
		}

		switch (cli.commands()[0].toLowerCase()) {
			case "www" ->
				www(authorRepo(cli), contentRepo(cli), gameTypeRepo(cli), documentRepo(cli), managedRepo(cli), wikiRepo(cli), cli);
			case "search-submit" ->
				searchSubmit(authorRepo(cli), contentRepo(cli), gameTypeRepo(cli), documentRepo(cli), managedRepo(cli), wikiRepo(cli), cli);
			case "summary" -> System.out.println(contentRepo(cli).summary());
			default -> {
				System.out.printf("Command \"%s\" does not exist!%n%n", cli.commands()[0]);
				usage();
			}
		}

		System.exit(0);
	}

	private static void www(AuthorRepository authorRepo, SimpleAddonRepository contentRepo, GameTypeRepository gameTypeRepo,
							DocumentRepository documentRepo, ManagedContentRepository managedRepo, WikiRepository wikiRepo, CLI cli)
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
		final boolean withFiles = Boolean.parseBoolean(cli.option("with-files", "true"));
		final boolean withPackages = Boolean.parseBoolean(cli.option("with-packages", "false"));
		final boolean withWikis = Boolean.parseBoolean(cli.option("with-wikis", "false"));
		final boolean withUmod = Boolean.parseBoolean(cli.option("with-umod", "false"));

		final boolean localImages = Boolean.parseBoolean(cli.option("local-images", "false"));
		if (localImages) System.out.println("Will download a local copy of content images, this will take additional time.");

		final SiteFeatures features = new SiteFeatures(localImages, withLatest, withSubmit, withSearch, withFiles, withWikis, withUmod);

		final Path staticOutput = outputPath.resolve("static");

		final long start = System.currentTimeMillis();

		// unpack static content
		Templates.unpackResources("static.list", Files.createDirectories(staticOutput).getParent());

		// prepare author names and aliases
		org.unrealarchive.content.Authors.autoPopRepository(authorRepo, contentRepo, gameTypeRepo, managedRepo);

		final Set<SiteMap.Page> allPages = ConcurrentHashMap.newKeySet();

		final Set<PageGenerator> generators = new HashSet<>();
		generators.add(new Index(contentRepo, gameTypeRepo, documentRepo, managedRepo, outputPath, staticOutput, features));

		if (cli.commands().length == 2 || (cli.commands().length > 2 && cli.commands()[1].equalsIgnoreCase("content"))) {
			// generate content pages
			generators.addAll(
				Arrays.asList(
					new Maps(contentRepo, outputPath, staticOutput, features, gameTypeRepo),
					new MapPacks(contentRepo, outputPath, staticOutput, features, gameTypeRepo),
					new Skins(contentRepo, outputPath, staticOutput, features),
					new Models(contentRepo, outputPath, staticOutput, features),
					new Voices(contentRepo, outputPath, staticOutput, features),
					new Mutators(contentRepo, outputPath, staticOutput, features),
					new Announcers(contentRepo, outputPath, staticOutput, features)
				));
			if (withPackages) generators.add(new Packages(contentRepo, gameTypeRepo, managedRepo, outputPath, staticOutput, features));
		}

		if (cli.commands().length == 2 || (cli.commands().length > 2 && cli.commands()[1].equalsIgnoreCase("authors"))) {
			generators.add(new Authors(authorRepo, contentRepo, gameTypeRepo, managedRepo, outputPath, staticOutput, features));
		}

		if (cli.commands().length == 2 || (cli.commands().length > 2 && cli.commands()[1].equalsIgnoreCase("docs"))) {
			generators.add(new Documents(documentRepo, outputPath, staticOutput, features));
		}

		if (cli.commands().length == 2 || (cli.commands().length > 2 && cli.commands()[1].equalsIgnoreCase("managed"))) {
			generators.add(new ManagedContent(managedRepo, outputPath, staticOutput, features));
		}

		if (cli.commands().length == 2 || (cli.commands().length > 2 && cli.commands()[1].equalsIgnoreCase("gametypes"))) {
			generators.add(new GameTypes(gameTypeRepo, contentRepo, outputPath, staticOutput, features));
		}

		if (cli.commands().length > 2 && cli.commands()[1].equalsIgnoreCase("packages")) {
			generators.add(new Packages(contentRepo, gameTypeRepo, managedRepo, outputPath, staticOutput, features));
		}

		if (features.wikis || (cli.commands().length > 2 && cli.commands()[1].equalsIgnoreCase("wiki"))) {
			generators.add(new Wiki(outputPath, staticOutput, features, wikiRepo));
		}

		if (features.submit) generators.add(new Submit(outputPath, staticOutput, features));
		if (features.search) generators.add(new Search(outputPath, staticOutput, features));
		if (features.umod) generators.add(new UmodRepack(outputPath, staticOutput, features));
		if (features.latest) generators.add(new Latest(contentRepo, gameTypeRepo, managedRepo, outputPath, staticOutput, features));
		if (features.files) generators.add(new FileDetails(contentRepo, outputPath, staticOutput, features));

		try (ForkJoinPool myPool = new ForkJoinPool(Integer.parseInt(cli.option("concurrency", "4")))) {
			myPool.submit(() -> generators.parallelStream().forEach(g -> {
				System.out.printf("Generating %s pages%n", g.getClass().getSimpleName());
				allPages.addAll(g.generate());
			})).join();
		}

		System.out.println("Generating sitemap");
		allPages.addAll(SiteMap.siteMap(SiteMap.SITE_ROOT, outputPath, allPages, 50000, features).generate());

		System.out.printf("Output %d pages in %.2fs%n", allPages.size(), (System.currentTimeMillis() - start) / 1000f);
	}

	private static void searchSubmit(AuthorRepository authorRepo, SimpleAddonRepository contentRepo, GameTypeRepository gameTypeRepo,
									 DocumentRepository documentRepo, ManagedContentRepository managedRepo, WikiRepository wikiManager,
									 CLI cli)
		throws IOException {
		// TODO documents, managed content

		// prepare author names and aliases
		org.unrealarchive.content.Authors.autoPopRepository(authorRepo, contentRepo, gameTypeRepo, managedRepo);

		final long start = System.currentTimeMillis();

		MESSubmitter submitter = new MESSubmitter();

		System.out.printf("Submitting content to search instance at %s%n",
						  System.getenv().getOrDefault("MSE_CONTENT_URL", System.getenv().getOrDefault("MSE_URL", ""))
		);

		submitter.submit(contentRepo, gameTypeRepo,
						 System.getenv().getOrDefault("SITE_URL", ""),
						 System.getenv().getOrDefault("MES_CONTENT_URL", System.getenv().getOrDefault("MES_URL", "")),
						 System.getenv().getOrDefault("MES_CONTENT_TOKEN", System.getenv().getOrDefault("MES_TOKEN", "")),
						 200, // submission batch size
						 percent -> System.out.printf("\r%.1f%% complete contents", percent * 100d),
						 done -> System.out.printf("%nContent search submission complete in %.2fs%n",
												   (System.currentTimeMillis() - start) / 1000f));

		System.out.printf("Submitting wikis to search instance at %s%n", System.getenv().getOrDefault("MES_WIKI_URL", ""));
		submitter.submit(wikiManager,
						 System.getenv().getOrDefault("SITE_URL", ""),
						 System.getenv().getOrDefault("MES_WIKI_URL", ""),
						 System.getenv().getOrDefault("MES_WIKI_TOKEN", ""),
						 50, // submission batch size
						 percent -> System.out.printf("\r%.1f%% complete wikis", percent * 100d),
						 done -> System.out.printf("%nWiki search submission complete in %.2fs%n",
												   (System.currentTimeMillis() - start) / 1000f));

		submitter.submitPackages(contentRepo, gameTypeRepo,
								 System.getenv().getOrDefault("SITE_URL", ""),
								 System.getenv().getOrDefault("MES_PACKAGE_URL", ""),
								 System.getenv().getOrDefault("MES_PACKAGE_TOKEN", ""),
								 500, // submission batch size
								 percent -> System.out.printf("\r%.1f%% complete packages", percent * 100d),
								 done -> System.out.printf("%nPackage search submission complete in %.2fs%n",
														   (System.currentTimeMillis() - start) / 1000f));
	}

	private static void usage() {
		System.out.println("Usage: www <command> [options]");
		System.out.println();
		System.out.println("Commands:");
		System.out.println("  www <output-path> [--content-path=<path> | --content-download]");
		System.out.println("    Generate the HTML website for browsing content.");
		System.out.println("  search-submit [--content-path=<path> | --content-download]");
		System.out.println("    Sync search metadata with a search service.");
		System.out.println("  summary [--content-path=<path> | --content-download]");
		System.out.println("    Show stats and counters for the content index in <content-path>");
	}
}
