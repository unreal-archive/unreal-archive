package org.unrealarchive.www;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.unrealarchive.common.JSON;
import org.unrealarchive.common.Util;
import org.unrealarchive.content.ContentEntity;
import org.unrealarchive.content.FileType;
import org.unrealarchive.content.Games;
import org.unrealarchive.content.RepositoryManager;
import org.unrealarchive.content.addons.Addon;
import org.unrealarchive.content.addons.GameType;
import org.unrealarchive.content.wiki.WikiPage;
import org.unrealarchive.content.wiki.WikiRepository;
import org.unrealarchive.www.content.Packages;

/**
 * Submits contents to Minimum Effort Search instance.
 * <p>
 * See https://github.com/shrimpza/minimum-effort-search
 */
public class MESSubmitter {

	private static final String ADD_ENDPOINT = "/index/add";
	private static final String ADD_BATCH_ENDPOINT = "/index/addBatch";

	public void submit(
		RepositoryManager repos, String rootUrl, String mseUrl, String mseToken, int batchSize,
		Consumer<Double> progress, Consumer<Boolean> done
	) throws IOException {
		Collection<ContentEntity<?>> contents = new HashSet<>();
		contents.addAll(repos.addons().all(false));
		contents.addAll(repos.gameTypes().all());
		Path root = Paths.get("");
		final int count = contents.size();
		int i = 0;

		final List<Map<String, Object>> batchDocs = new ArrayList<>(batchSize);

		for (ContentEntity<?> content : contents) {
			Map<String, Object> doc = Map.of(
				"id", content.id(),
				"score", content instanceof GameType ? 2.0d : 1.0d,
				"fields", Map.of(
					"name", content.name().replaceAll("-", "\\\\-"),
					"game", content.game(),
					"type", content.friendlyType(),
					"author", content.authorName().replaceAll("-", "\\\\-"),
					"url", rootUrl + "/" + content.pagePath(root).toString(),
					"date", content.releaseDate(),
					"description", content.autoDescription(),
					"image", content.leadImage(),
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

	public void submitPackages(
		RepositoryManager repos, String rootUrl, String mseUrl, String mseToken, int batchSize,
		Consumer<Double> progress, Consumer<Boolean> done
	) {
		Map<Games, Map<String, Map<Addon.ContentFile, List<ContentEntity<?>>>>> contents = Packages.loadContentFiles(repos);

		final int count = contents.values().stream().mapToInt(Map::size).sum();
		final int[] i = { 0 };

		final List<Map<String, Object>> batchDocs = new ArrayList<>(batchSize);

		contents.forEach((game, content) -> content.forEach((pkg, file) -> {

			Map<String, Object> doc = Map.of(
				"id", String.format("%s_%s", Util.slug(game.name()), Util.slug(pkg)),
				"score", 1.0d,
				"fields", Map.of(
					"name", pkg,
					"fileName", file.keySet().stream().findFirst().map(c -> c.name).orElse(pkg),
					"game", game.name,
					"type", file.keySet().stream()
								.map(f -> FileType.forFile(f.name))
								.filter(Objects::nonNull)
								.distinct()
								.map(Enum::name)
								.collect(Collectors.joining(", ")),
					"versions", file.size(),
					"uses", file.values().stream().mapToInt(List::size).sum(),
					"url", String.format("%s/%s/packages/%s/index.html", rootUrl, Util.slug(game.name), Util.slug(pkg)),
					"keywords", String.join(" ", game.tags)
				)
			);

			batchDocs.add(doc);

			if (batchDocs.size() >= batchSize) {
				try {
					post(mseUrl + ADD_BATCH_ENDPOINT, mseToken, JSON.toString(Map.of("docs", batchDocs)));
				} catch (IOException ignored) {}
				batchDocs.clear();
			}

			i[0]++;

			if (i[0] % 1000 == 0) progress.accept((double)i[0] / (double)count);
		}));

		progress.accept(1.0d);
		done.accept(true);
	}

	public void submitWiki(RepositoryManager repos, String rootUrl, String mseUrl, String mseToken, int batchSize,
						   Consumer<Double> progress, Consumer<Boolean> done) throws IOException {
		Set<String> stopWords = stopWords();

		for (WikiRepository.Wiki wiki : repos.wikis().all()) {
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

		progress.accept(1.0d);
		done.accept(true);
	}

	private static Set<String> stopWords() throws IOException {
		try (InputStream stopwords = MESSubmitter.class.getResourceAsStream("stopWords.txt");
			 BufferedReader br = new BufferedReader(new InputStreamReader(stopwords))) {
			return br.lines()
					 .map(String::trim)
					 .filter(s -> !s.isBlank() && !s.startsWith("#"))
					 .collect(Collectors.toSet());
		}
	}

	private static boolean post(String url, String token, String payload) throws IOException {
		URL urlConnection = Util.url(url);
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
