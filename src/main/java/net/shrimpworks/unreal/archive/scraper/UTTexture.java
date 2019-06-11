package net.shrimpworks.unreal.archive.scraper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * Unreal Tournament: http://www.uttexture.com/UT/Website/Downloads/Downloads.htm
 * Unreal: http://www.unrealtexture.com/Unreal/Website/Downloads/Downloads.htm
 * <p>
 * Direct download listings:
 * <p>
 * Unreal Tournament: http://uttexture.com/UT/Downloads/
 * Unreal: http://unrealtexture.com/Unreal/Downloads/
 */
public class UTTexture {

	private static final Pattern DIR_MATCH = Pattern.compile("Index of /[^/]+/Downloads/(.+)");

	private static final String BASE_URL_UT = "http://uttexture.com/UT/Downloads/";
	private static final String BASE_URL_UNREAL = "http://unrealtexture.com/Unreal/Downloads/";
	private static final String BACK = "Parent Directory";
	private static final String DIR_SIZE = "-";

	private static final String FILECATCHER = "FileCatcher.php";

	public static void index(CLI cli) throws IOException {
		if (cli.commands().length < 3) throw new IllegalArgumentException("A root URL is required!");

		if (!cli.commands()[2].startsWith(BASE_URL_UT) && !cli.commands()[2].startsWith(BASE_URL_UNREAL)) {
			throw new IllegalArgumentException("Provided URL is not for UnrealTexture or UTTexture.");
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

		Elements links = doc.select("table tr");
		Matcher m = DIR_MATCH.matcher(doc.select("h1").text());
		String path = m.matches() ? m.group(1) : "";

		List<Found.FoundUrl> collected = links.stream()
											  .filter(r -> r.child(0).tagName().equalsIgnoreCase("td"))
											  .filter(r -> !r.child(1).child(0).text().trim().contains(BACK))
											  .filter(r -> !r.child(1).child(0).text().trim().equals(FILECATCHER))
											  .map(r -> {
												  String dl = r.child(1).child(0).absUrl("href");
												  String name = r.child(1).child(0).text().trim();
												  boolean dir = r.child(3).text().trim().equals(DIR_SIZE);
												  return new Found.FoundUrl(name, path, dl, dl, dir);
											  })
											  .collect(Collectors.toList());

		completed.accept(new Found(url, collected));
	}
}
