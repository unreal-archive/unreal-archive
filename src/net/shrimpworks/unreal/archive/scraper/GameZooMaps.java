package net.shrimpworks.unreal.archive.scraper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import net.shrimpworks.unreal.archive.CLI;
import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.YAML;

public class GameZooMaps {

	private static final String BASE_URL = "http://ut99maps.gamezoo.org/maps.html";

	private static final Pattern PATH_MATCH = Pattern.compile("maps/([^/]+)/.+?");

	public static void index(CLI cli) throws IOException {
		final Connection connection = Jsoup.connect(BASE_URL);
		connection.userAgent(Downloader.USER_AGENT);
		connection.timeout(60000);

		final List<Found.FoundUrl> foundList = new ArrayList<>();

		Document doc = connection.url(BASE_URL).get();

		Elements links = doc.select("#content_wrapper .maps a");

		for (Element link : links) {
			Matcher m = PATH_MATCH.matcher(link.attr("href"));
			if (m.matches()) {
				foundList.add(new Found.FoundUrl(Util.fileName(link.attr("href")), m.group(1), link.absUrl("href"), BASE_URL));
			}
		}

		System.out.println(YAML.toString(foundList));
	}
}
