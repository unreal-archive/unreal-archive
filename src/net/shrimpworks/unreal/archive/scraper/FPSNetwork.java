package net.shrimpworks.unreal.archive.scraper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import net.shrimpworks.unreal.archive.CLI;
import net.shrimpworks.unreal.archive.YAML;

/**
 * Valid as of 2018-10.
 * <p>
 * Scraper for URLS from fpsnetwork.com/downloads/index.php, expects an input path.
 */
public class FPSNetwork {

	private static final String BASE_URL = "http://fpsnetwork.com/downloads/";
	private static final String BACK = "..";
	private static final String DIR = "<DIR>";

	public static void index(CLI cli) throws IOException {
		if (cli.commands().length < 3) throw new IllegalArgumentException("A root URL is required!");
		if (!cli.commands()[2].startsWith(BASE_URL)) {
			throw new IllegalArgumentException("Provided URL is not for https://gamefront.online/");
		}

		final Connection connection = Jsoup.connect(cli.commands()[2]);
		connection.userAgent(Downloader.USER_AGENT);
		connection.timeout(60000);

		final Set<String> visited = new HashSet<>();

		final List<Found.FoundUrl> foundList = new ArrayList<>();

		final long slowdown = Long.valueOf(cli.option("slowdown", "2500"));

		index(connection, cli.commands()[2], new Consumer<Found>() {
			@Override
			public void accept(Found found) {
				visited.add(found.url);

				foundList.addAll(found.files());

				for (Found.FoundUrl dir : found.dirs()) {
					if (!visited.contains(dir.url)) {
						try {
							if (slowdown > 0) Thread.sleep(slowdown);
							index(connection, dir.url, this);
						} catch (InterruptedException | IOException e) {
							throw new RuntimeException(e);
						}
					}
				}
			}
		});

		System.out.println(YAML.toString(foundList));
	}

	private static void index(Connection connection, String url, Consumer<Found> completed) throws IOException {
		System.err.printf("Indexing URL: %s%n", url);

		Document doc = connection.url(url).get();

		Elements links = doc.select("table.snif tr.snF");

		List<Found.FoundUrl> collected = links.stream()
											  .filter(r -> !r.child(2).child(0).attr("title").equals(BACK))
											  .map(r -> {
												  String dl = r.child(2).child(0).absUrl("href");
												  String name = r.child(2).child(0).attr("title").trim();
												  String path = dl.replaceFirst(BASE_URL, "").replace(name, "");
												  boolean dir = r.child(3).text().trim().equals(DIR);
												  return new Found.FoundUrl(name, path, dl, url, dir);
											  })
											  .collect(Collectors.toList());

		completed.accept(new Found(url, collected));
	}
}
