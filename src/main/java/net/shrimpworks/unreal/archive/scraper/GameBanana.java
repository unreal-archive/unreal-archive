package net.shrimpworks.unreal.archive.scraper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import net.shrimpworks.unreal.archive.CLI;
import net.shrimpworks.unreal.archive.YAML;

/**
 * Valid as of 2018-10.
 * <p>
 * Grabs URLs from maps pages:
 * <p>
 * UT: https://gamebanana.com/maps/games/36
 * UT2003: https://gamebanana.com/maps/games/37
 * UT2004: https://gamebanana.com/maps/games/31
 * <p>
 * Pagination link: https://gamebanana.com/maps/games/36?vl[page]=2&mid=SubmissionsList
 * <p>
 * Link format: https://gamebanana.com/maps/202173
 * Download page for link: https://gamebanana.com/maps/download/202173
 * Actual download link: https://gamebanana.com/dl/400111
 * <p>
 * API thing: https://gamebanana.com/maps/games/36?vl[page]=1&mid=SubmissionsList&api=SubmissionsListModule
 * <p>
 * API note: since not all information is available via the API (download links and stuff),
 * we still have to scrape HTML for that. As such, it's not useful to jump between formats,
 * so we will stick to HTML scraping.
 */
public class GameBanana {

	private static final Pattern PAGEINATOR = Pattern.compile("\\d+-\\d+ of (\\d+)");
	private static final int PAGE_SIZE = 20;

	private static final Pattern FILE_LINK = Pattern.compile(".+/maps/([0-9]+)");
	private static final String BASE_URL = "https://gamebanana.com/maps/games/";
	private static final String PAGINATION_QUERY = "%s?vl[page]=%d&mid=SubmissionsList";
	private static final String DOWNLOAD_PAGE = "https://gamebanana.com/maps/download/%s";

	public static void index(CLI cli) throws IOException {
		if (cli.commands().length < 3) throw new IllegalArgumentException("A root URL is required!");
		if (!cli.commands()[2].startsWith(BASE_URL)) {
			throw new IllegalArgumentException("Provided URL is not for GameBanana");
		}

		final long slowdown = Long.valueOf(cli.option("slowdown", "7500"));

		final Connection connection = Jsoup.connect(cli.commands()[2]);
		connection.userAgent(Downloader.USER_AGENT);
		connection.timeout(60000);

		final List<Found.FoundUrl> foundList = new ArrayList<>();

		Document doc = connection.url(cli.commands()[2]).get();

		Elements pagination = doc.select("#SubmissionsListModule .AjaxPaginator span");
		Matcher m = PAGEINATOR.matcher(pagination.get(0).text());
		if (!m.matches()) throw new IllegalStateException("Could not find paginator!");
		int pages = (int)Math.round(Math.ceil(Double.valueOf(m.group(1)) / PAGE_SIZE));

		for (int p = 1; p <= pages; p++) {
			doc = connection.url(String.format(PAGINATION_QUERY, cli.commands()[2], p)).get();
			Elements links = doc.select("#SubmissionsListModule records record recordcell.Identifiers a");
			for (Element link : links) {
				try {
					if (slowdown > 0) Thread.sleep(slowdown);
					findDownload(connection, link.attr("href"), f -> foundList.addAll(f.files()));
				} catch (InterruptedException | IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		System.out.println(YAML.toString(foundList));
	}

	private static void findDownload(Connection connection, String url, Consumer<Found> completed) throws IOException {
		System.err.printf("Finding download for: %s%n", url);

		Matcher m = FILE_LINK.matcher(url);
		if (!m.matches()) return;

		String dlPage = String.format(DOWNLOAD_PAGE, m.group(1));

		Document doc = connection.url(dlPage).get();

		Elements spans = doc.select("#FilesModule .FileInfo span");
		String name = spans.get(0).text().trim();

		Elements links = doc.select("#FilesModule .DownloadOptions a.GreenColor");

		if (links.size() != 1) return;

		Element link = links.iterator().next();

		Elements breadbrumbs = doc.select("#Breadcrumb a");
		String dir = breadbrumbs.get(breadbrumbs.size() - 2).text();

		completed.accept(new Found(url, Collections.singletonList(new Found.FoundUrl(name, dir, link.absUrl("href"), dlPage))));
	}

}
