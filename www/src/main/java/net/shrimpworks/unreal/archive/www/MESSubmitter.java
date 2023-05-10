package net.shrimpworks.unreal.archive.www;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import net.shrimpworks.unreal.archive.common.JSON;
import net.shrimpworks.unreal.archive.common.Util;
import net.shrimpworks.unreal.archive.content.addons.Addon;
import net.shrimpworks.unreal.archive.content.addons.SimpleAddonRepository;
import net.shrimpworks.unreal.archive.content.wiki.WikiPage;
import net.shrimpworks.unreal.archive.content.wiki.WikiRepository;

/**
 * Submits contents to Minimum Effort Search instance.
 * <p>
 * See https://github.com/shrimpza/minimum-effort-search
 */
public class MESSubmitter {

	private static final String ADD_ENDPOINT = "/index/add";
	private static final String ADD_BATCH_ENDPOINT = "/index/addBatch";

	public void submit(
		SimpleAddonRepository contentRepo, String rootUrl, String mseUrl, String mseToken, int batchSize,
		Consumer<Double> progress, Consumer<Boolean> done
	) throws IOException {
		Collection<Addon> contents = contentRepo.all(false);
		Path root = Paths.get("");
		final int count = contents.size();
		int i = 0;

		final List<Map<String, Object>> batchDocs = new ArrayList<>(batchSize);

		for (Addon content : contents) {
			Map<String, Object> doc = Map.of(
				"id", content.hash,
				"score", 1.0d,
				"fields", Map.of(
					"name", content.name.replaceAll("-", "\\\\-"),
					"game", content.game,
					"type", content.friendlyType(),
					"author", content.authorName().replaceAll("-", "\\\\-"),
					"url", rootUrl + "/" + content.slugPath(root).toString() + ".html",
					"date", content.releaseDate,
					"description", content.autoDescription(),
					"image", content.attachments.stream()
												.filter(a -> a.type == Addon.AttachmentType.IMAGE)
												.map(a -> a.url)
												.findFirst().orElse(""),
					"keywords", String.join(" ", content.autoTags())
				)
			);

			batchDocs.add(doc);

			if (batchDocs.size() >= batchSize) {
				post(mseUrl + ADD_BATCH_ENDPOINT, mseToken, JSON.toString(Map.of("docs", batchDocs)));
				batchDocs.clear();
			}

			i++;

			if (i % 1000 == 0) progress.accept((double)i / (double)count);
		}

		progress.accept(1.0d);
		done.accept(true);
	}

	public void submit(WikiRepository wikiManager, String rootUrl, String mseUrl, String mseToken, int batchSize,
					   Consumer<Double> progress, Consumer<Boolean> done) throws IOException {
		Set<String> stopWords = stopWords();

		for (WikiRepository.Wiki wiki : wikiManager.all()) {
			int i = 0;

			Set<WikiPage> candidates = wiki.all().parallelStream()
										   .filter(p -> p.parse.categories
											   .stream().noneMatch(c -> wiki.skipCategories.stream().anyMatch(c.name::contains))
										   )
										   .filter(p -> p.parse.templates
											   .stream().noneMatch(c -> wiki.skipTemplates.stream().anyMatch(c.name::contains))
										   )
										   .collect(Collectors.toSet());

			final List<Map<String, Object>> batchDocs = new ArrayList<>(batchSize);

			for (WikiPage page : candidates) {
				Document document = Jsoup.parse(page.parse.text.text);
				document.outputSettings().prettyPrint(false);
				Wiki.sanitisedPageHtml(document, wiki);
				document.select("pre").remove();           // remove code blocks...
				document.select(".toc").remove();          // remove table of contents
				document.select(".navbox").remove();       // remove table of contents
				document.select(".infobox-class").remove();// remove table of contents

				String content = document.select("body").text().toLowerCase();
				// remove non-alphanumerics
				content = content.replaceAll("[^A-Za-z0-9-'\" ]", "");
				// expensive - distinct terms only
				//content = Arrays.stream(content.split(" ")).distinct().collect(Collectors.joining(" "));
				// remove stop words
				content = " " + content + " ";
				for (String s : stopWords) content = content.replaceAll(" " + s + " ", " ");

				Map<String, Object> doc = Map.of(
					"id", wiki.name + page.name,
					"score", 1.0d,
					"fields", Map.of(
						"name", page.name.replaceAll("-", "\\\\-"),
						"wiki", wiki.name,
						"url", String.format("%s/wikis/%s/%s.html", rootUrl, Util.slug(wiki.name), page.name.replaceAll(" ", "_")),
						"content", content.trim())
				);

				batchDocs.add(doc);

				if (batchDocs.size() >= batchSize) {
					post(mseUrl + ADD_BATCH_ENDPOINT, mseToken, JSON.toString(Map.of("docs", batchDocs)));
					batchDocs.clear();
				}

				i++;

				if (i % 100 == 0) progress.accept((double)i / (double)(candidates.size()));
			}
		}
	}

	private static Set<String> stopWords() throws IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(MESSubmitter.class.getResourceAsStream("stopWords.txt")))) {
			return br.lines()
					 .map(String::trim)
					 .filter(s -> !s.isBlank() && !s.startsWith("#"))
					 .collect(Collectors.toSet());
		}
	}

	private static boolean post(String url, String token, String payload) throws IOException {
		URL urlConnection = new URL(url);
		HttpURLConnection httpConn = (HttpURLConnection)urlConnection.openConnection();

		httpConn.setRequestMethod("POST");
		httpConn.setRequestProperty("Authorization", String.format("bearer %s", token));
		httpConn.setRequestProperty("Content-Length", Long.toString(payload.length()));

		httpConn.setDoOutput(true);
		httpConn.connect();

		try {
			try (OutputStreamWriter wr = new OutputStreamWriter(httpConn.getOutputStream(), StandardCharsets.UTF_8)) {
				wr.write(payload);
				wr.flush();
			}

			int response = httpConn.getResponseCode();
			return response >= 200 && response <= 299;
		} finally {
			String connection = httpConn.getHeaderField("Connection");
			if (connection == null || connection.equals("Close")) {
				httpConn.disconnect();
			}
		}
	}
}
