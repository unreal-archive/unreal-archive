package net.shrimpworks.unreal.archive.scraper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
 */
public class UnrealPlayground {

	private static final String BASE_URL = "http://www.unrealplayground.com/forums/downloads.php";
	private static final String CATEGORY_URL = "%s?do=cat&id=%s&page=%d";

	private static final int[] CATEGORIES = {
			24, 25, 26, 27, 29, // Unreal Tournament
			20, 21, 22, 23, // UT2003
			11, 12, 13, 14, 15, 16, 17, 18, 19 // UT2004
	};

	private static final Pattern TITLE_NAME_MATCH = Pattern.compile(".+?Downloads - (.+)");

	public static void index(CLI cli) throws IOException {
		if (cli.commands().length < 3) throw new IllegalArgumentException("Category IDs are required!");

		String[] cats = cli.commands()[2].split(",");

		final Connection connection = Jsoup.connect(BASE_URL);
		connection.userAgent(Downloader.USER_AGENT);
		connection.timeout(60000);

		final Set<String> visited = new HashSet<>();

		final List<Found.FoundUrl> foundList = new ArrayList<>();

		final long slowdown = Long.valueOf(cli.option("slowdown", "2500"));

		for (String cat : cats) {
			index(connection, String.format(CATEGORY_URL, BASE_URL, cat, 1), slowdown, new Consumer<Found>() {
				@Override
				public void accept(Found found) {
					visited.add(found.url);

					foundList.addAll(found.files());

					System.out.println(found.files());

					// more pages
					for (Found.FoundUrl dir : found.dirs()) {
						if (!visited.contains(dir.url)) {
							try {
								if (slowdown > 0) Thread.sleep(slowdown);
								index(connection, dir.url, slowdown, this);
							} catch (InterruptedException | IOException e) {
								throw new RuntimeException(e);
							}
						}
					}
				}
			});
		}

		System.out.println(YAML.toString(foundList));
	}

	private static void index(Connection connection, String url, long slowdown, Consumer<Found> completed) throws IOException {
		System.err.printf("Indexing URL: %s \r", url);

		Document doc = connection.url(url).get();

		Matcher m = TITLE_NAME_MATCH.matcher(doc.title());
		final String category = (m.matches()) ? m.group(1) : "Maps";

		Elements links = doc.select("table tbody td > a");

		List<String> downloadPages = links.stream()
										  .filter(e -> !e.text().equalsIgnoreCase("more"))
										  .filter(e -> e.attr("href").contains("?do=file&id="))
										  .map(e -> e.absUrl("href"))
										  .distinct()
										  .collect(Collectors.toList());

		for (int i = 0; i < downloadPages.size(); i++) {
			System.err.printf("Indexing URL: %s [%d of %d] \r", url, i, downloadPages.size());
			try {
				if (slowdown > 0) Thread.sleep(slowdown);
				indexPage(connection, downloadPages.get(i), category, completed);
			} catch (Exception e) {
				//
			}
		}

		System.err.println();

		// next page
		Elements nextLinks = doc.select("div.pagenav a.smallfont");
		nextLinks.stream()
				 .filter(e -> e.attr("title").startsWith("Next Page"))
				 .map(e -> e.absUrl("href"))
				 .findFirst()
				 .ifPresent(u -> completed.accept(
						 new Found(url, Collections.singletonList(new Found.FoundUrl(category, category, u, url, true)))));
	}

	private static void indexPage(Connection connection, String url, String category, Consumer<Found> completed) throws IOException {
		Document doc = connection.url(url).get();

		Matcher m = TITLE_NAME_MATCH.matcher(doc.title());
		final String name = (m.matches()) ? m.group(1) : "Unknown";

		Elements links = doc.select("table tbody td > a");

		links.stream()
			 .filter(e -> e.childNodeSize() == 1
						  && e.childNode(0).nodeName().equalsIgnoreCase("img")
						  && e.childNode(0).hasAttr("alt")
						  && e.child(0).attr("alt").equalsIgnoreCase("download"))
			 .map(e -> e.absUrl("href"))
			 .findFirst()
			 .ifPresent(d -> completed.accept(new Found(url, Collections.singletonList(new Found.FoundUrl(name, category, d, url)))));
	}
}
