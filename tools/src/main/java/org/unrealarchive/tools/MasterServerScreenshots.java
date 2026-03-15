package org.unrealarchive.tools;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.unrealarchive.common.CLI;
import org.unrealarchive.common.Util;
import org.unrealarchive.content.Games;
import org.unrealarchive.content.addons.Addon;
import org.unrealarchive.content.addons.SimpleAddonRepository;
import org.unrealarchive.content.addons.SimpleAddonType;

public class MasterServerScreenshots {

	public static void main(String[] args) throws IOException {
		final CLI cli = CLI.parse(args);

		final Games game = Games.byName(cli.commands()[0]);

		final Path outputDir = Path.of(cli.commands()[1]);
		if (!Files.isDirectory(outputDir)) {
			Files.createDirectories(outputDir);
		}

		System.out.println("Fetching screenshots for " + game.name() + " into " + outputDir);

		downloadScreenshots(game, outputDir);
	}

	public static void downloadScreenshots(Games game, Path outputDir) throws IOException {
		// 1. get addon repository
		SimpleAddonRepository repo = IndexHelper.repo();

		// 2. get all maps for the game provided which have at least 1 image attachment
		List<Addon> maps = repo.search(game.name, null, null, null)
							   .stream()
							   .filter(m -> !m.isVariation())
							   .filter(m -> m.contentType.equals(SimpleAddonType.MAP.name()))
							   .filter(m -> m.attachments != null && !m.attachments.isEmpty())
							   .filter(m -> m.attachments.stream().anyMatch(a -> a.type == Addon.AttachmentType.IMAGE))
							   .sorted((m1, m2) -> {
								   if (m1.releaseDate == null && m2.releaseDate == null) return 0;
								   if (m1.releaseDate == null) return 1;
								   if (m2.releaseDate == null) return -1;
								   return m1.releaseDate.compareTo(m2.releaseDate);
							   })
							   .collect(Collectors.toMap(m -> m.name.toLowerCase(), m -> m, (m1, m2) -> m1))
							   .values()
							   .stream()
							   .toList();

		System.out.println("Found " + maps.size() + " maps with screenshots");

		// 3. create an `_index.csv` file with columns `map` and `filename` in output directory - tab separated
		Path indexFile = outputDir.resolve("_index.csv");
		BufferedWriter csvWriter = Files.newBufferedWriter(indexFile, StandardCharsets.UTF_8);
		csvWriter.write("map\tfilename\n");

		AtomicInteger counter = new AtomicInteger(0);

		// 4. download first image attachment for each map, save it under the map name, lowercase, special characters, in output directory
		//	- use parallel stream over maps collection
		//	- for every 100 maps, print progress
		maps.stream().forEach(map -> {
			try {
				Addon.Attachment imageAttachment = map.attachments.stream()
																  .filter(a -> a.type == Addon.AttachmentType.IMAGE)
																  .findFirst()
																  .orElse(null);

				if (imageAttachment != null) {
					String normalised = Util.normalised(map.name.toLowerCase());
					String extension = Util.extension(imageAttachment.url);
					String filename = String.format("%s.%s", normalised, extension);
					Path outputFile = outputDir.resolve(filename);

					Path downloaded = Util.downloadTo(imageAttachment.url, outputFile);

					// 5. append row to CSV index of map name and output filename (only local filename in output dir, no path)
					synchronized (csvWriter) {
						csvWriter.write(map.name + "\t" + Util.fileName(downloaded) + "\n");
						csvWriter.flush();
					}

					int count = counter.incrementAndGet();
					if (count % 100 == 0) System.out.printf("Progress: %d maps processed%n", count);
				}
			} catch (Exception e) {
				System.err.printf("Error processing map %s: %s%n", map.name, e.getMessage());
				e.printStackTrace();
			}
		});

		csvWriter.close();
		System.out.printf("Completed: %d maps processed%n", counter.get());
	}
}
