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

public class GameFrontOnline {

	// this seems to have many more older files than gamefront.com

	private static final Pattern FILE_LINK = Pattern.compile("/files/([0-9]+)/(.+)");

	private static final String BASE_URL = "https://gamefront.online/files2/listing/pub2/";
	private static final String DOWNLOAD_PAGE = "https://gamefront.online/files2/service/thankyou?id=%s";

	// https://gamefront.online/files2/listing/pub2/unreal/
	// https://gamefront.online/files2/listing/pub2/Unreal_Tournament/

	public static void index(CLI cli) throws IOException {
		if (cli.commands().length < 3) throw new IllegalArgumentException("A root URL is required!");
		if (!cli.commands()[2].startsWith(BASE_URL)) {
			throw new IllegalArgumentException("Provided URL is not for https://gamefront.online/");
		}

		final long slowdown = Long.valueOf(cli.option("slowdown", "2500"));

		final Connection connection = Jsoup.connect(cli.commands()[2]);
		connection.userAgent(Downloader.USER_AGENT);
		connection.timeout(60000);

		final List<Found.FoundUrl> foundList = new ArrayList<>();

		Document doc = connection.url(cli.commands()[2]).get();

		Elements links = doc.select("#listing_of_files h4.filetype_one a");

		for (Element link : links) {
			try {
				if (slowdown > 0) Thread.sleep(slowdown);
				findDownload(connection, link.attr("href"), f -> foundList.addAll(f.files()));
			} catch (InterruptedException | IOException e) {
				throw new RuntimeException(e);
			}
		}

		System.out.println(YAML.toString(foundList));
	}

	private static void findDownload(Connection connection, String url, Consumer<Found> completed) throws IOException {
		System.err.printf("Finding download for: %s%n", url);

		Matcher m = FILE_LINK.matcher(url);
		if (!m.matches()) return;

		String dlPage = String.format(DOWNLOAD_PAGE, m.group(1));
		String name = m.group(2);

		Document doc = connection.url(dlPage).get();

		Elements links = doc.select("div.countDownDiv p a");

		if (links.size() != 1) return;

		Element link = links.iterator().next();

		String dir = "";
		Elements breadbrumbs = doc.select("#files-breadcrumbs a");
		for (int i = 1; i < breadbrumbs.size(); i++) {
			if (!breadbrumbs.get(i).text().toLowerCase().contains("titles")) {
				dir += breadbrumbs.get(i).text() + "/";
			}
		}

		completed.accept(new Found(url, Collections.singletonList(new Found.FoundUrl(name, dir, link.absUrl("href"), dlPage))));
	}

}
