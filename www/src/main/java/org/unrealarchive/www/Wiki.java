package org.unrealarchive.www;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import org.unrealarchive.common.Util;
import org.unrealarchive.content.wiki.WikiRepository;
import org.unrealarchive.content.wiki.WikiPage;

public class Wiki implements PageGenerator {

	private static final Pattern FILE_LINK = Pattern.compile(".?/File:(.*)");
	private static final String IMG_PATH = "w/images";

	private final Path root;
	private final Path siteRoot;
	private final Path staticRoot;
	private final SiteFeatures features;

	private final WikiRepository wikiManager;

	public Wiki(Path output, Path staticRoot, SiteFeatures features, WikiRepository wikiRepo) {
		this.root = output.resolve("wikis");
		this.siteRoot = output;
		this.staticRoot = staticRoot;
		this.features = features;
		this.wikiManager = wikiRepo;
	}

	@Override
	public Set<SiteMap.Page> generate() {
		Templates.PageSet pages = new Templates.PageSet("wikis", features, siteRoot, staticRoot, root);

		wikiManager.all().forEach(wiki -> buildWiki(wiki, pages));

		// generate wiki landing page
		pages.add("wikis.ftl", SiteMap.Page.of(0.75f, SiteMap.ChangeFrequency.weekly), "Wikis")
			 .put("wikis", wikiManager.all())
			 .write(root.resolve("index.html"));

		return pages.pages;
	}

	public void buildWiki(WikiRepository.Wiki wiki, Templates.PageSet pages) {
		Path out = root.resolve(Util.slug(wiki.name));

		Map<String, Set<WikiPage>> categories = new ConcurrentHashMap<>();
		Set<String> users = ConcurrentHashMap.newKeySet();
		Set<String> discussions = ConcurrentHashMap.newKeySet();

		Set<WikiPage> candidates = wiki.all().parallelStream()
									   .filter(p -> p.parse.categories
										   .stream().noneMatch(c -> wiki.skipCategories.stream().anyMatch(c.name::contains))
									   )
									   .filter(p -> p.parse.templates
										   .stream().noneMatch(c -> wiki.skipTemplates.stream().anyMatch(c.name::contains))
									   )
									   .peek(p -> {
										   // collect category associations
										   p.parse.categories.stream()
															 .filter(c -> c.name != null && !c.name.isBlank())
															 .forEach(c -> categories.computeIfAbsent(c.name, n -> new HashSet<>()).add(p));

										   // collect users
										   if (p.name.startsWith("User:")) {
											   users.add(p.name.substring(p.name.indexOf(':') + 1));
										   }
										   // collect discussion pages
										   if (p.name.startsWith("Talk:")) {
											   discussions.add(p.name.substring(p.name.indexOf(':') + 1));
										   }
									   })
									   .collect(Collectors.toSet());

		Set<String> linkingCandidates = candidates.parallelStream()
												  .map(p -> p.name.replaceAll(" ", "_").replaceAll("'", "%27"))
												  .collect(Collectors.toSet());

		final Path imagesPath = out.resolve(IMG_PATH);
		try {
			Files.createDirectories(imagesPath);

			// copy wiki image stuff
			if (wiki.title != null && !wiki.title.isBlank()) {
				Files.copy(wiki.path.resolve(wiki.title), out.resolve(wiki.title), StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to create images output path", e);
		}

		candidates
			.parallelStream()
			.forEach(page -> {
				try {
					// dump images for this page
					copyFiles(wiki, page, imagesPath);

					Path pagePath;
					if (page.name.equals(wiki.homepage)) {
						pagePath = out.resolve("index.html");
					} else if (page.name.contains("/")) {
						pagePath = Files.createDirectories(
											out.resolve(page.name.substring(0, page.name.lastIndexOf('/')).replaceAll(" ", "_"))
										)
										.resolve(String.format(
													 "%s.html", page.name.substring(page.name.lastIndexOf('/') + 1)
												 ).replaceAll(" ", "_")
										);
					} else {
						pagePath = out.resolve(String.format("%s.html", page.name.replaceAll(" ", "_")));
					}

					Set<WikiPage> categoryPages = new HashSet<>();
					if (page.name.toLowerCase().startsWith("category:")) {
						categoryPages.addAll(categories.getOrDefault(page.name.substring(page.name.indexOf(':') + 1).replaceAll(" ", "_"),
																	 Set.of()));
					}

					Document document = Jsoup.parse(page.parse.text.text);
					document.outputSettings().prettyPrint(false);
					sanitisedPageHtml(document, wiki);
					fixLinks(document, wiki, linkingCandidates, out, pagePath, imagesPath);

					pages.add("page.ftl", SiteMap.Page.of(0.75f, SiteMap.ChangeFrequency.monthly),
							  String.join(" / ", "Wikis", wiki.name, page.name))
						 .put("text", document.select("body").html().trim())
						 .put("page", page)
						 .put("wiki", wiki)
						 .put("categoryPages", categoryPages)
						 .put("wikiPath", out)
						 .put("hasUserPage", page.revision != null && users.contains(page.revision.user))
						 .put("hasDiscussion", discussions.contains(page.name))
						 .write(pagePath);
				} catch (Exception e) {
					System.err.println("Failed generating page: " + page.name);
					e.printStackTrace();
				}
			});
	}

	private static void fixLinks(Document document, WikiRepository.Wiki wiki, Set<String> linkingCandidates, Path out, Path pagePath,
								 Path imagesPath) {
		// fix local links
		document.select("a").stream()
				.filter(a -> a.hasAttr("href") && a.attr("href").startsWith("/"))
				.filter(a -> !a.attr("href").startsWith("/File:"))
				.forEach(a -> {
					String target = a.attr("href").substring(1);
					if (target.isEmpty()) target = wiki.homepage;
					String targetPage = target;

					if (target.indexOf("#") > 0) targetPage = target.substring(0, target.indexOf("#"));

					// resolve redirects automatically
					targetPage = wiki.redirects.getOrDefault(targetPage, targetPage);

					if (!targetPage.equals("index") // special case - support Main Page -> index rename
						&& (a.attr("href").contains("?redlink") || !linkingCandidates.contains(targetPage.replaceAll(" ", "_")))) {
						// remove links to pages that don't exist, or we're not tracking
						a.replaceWith(new Element("span").addClass("redlink").text(a.text()));
					} else {
						// make links to pages relative
						targetPage = String.format("%s.html", targetPage.replaceAll(" ", "_"));
						targetPage = String.format("./%s", pagePath.getParent().relativize(out.resolve(targetPage)));

						// put anchors back on
						if (target.indexOf("#") > 0) targetPage = String.format("%s%s", targetPage, target.substring(target.indexOf("#")));

						a.attr("href", targetPage);
					}
				});

		// update image links
		document.select("a[href*=\"File:\"]")
				.forEach(a -> {
					Matcher href = FILE_LINK.matcher(a.attr("href"));
					if (href.find()) {
						a.attr("href", pagePath.getParent().relativize(imagesPath).resolve(href.group(1)).toString());
					}
					a.select("img")
					 .forEach(i -> i.attr("src", pagePath.getParent().relativize(imagesPath).resolve(href.group(1)).toString()));
				});

	}

	public static void sanitisedPageHtml(Document document, WikiRepository.Wiki wiki) {
		wiki.deleteElements.forEach(selector -> document.select(selector).remove());

		// strip comments
		document.forEachNode((Consumer<Node>)n -> {
			if (n.nodeName().equals("#comment")) n.remove();
		});

		// remove empty paragraphs (these seem to happen under .ambox blocks)
		document.select("p").stream()
				.filter(n -> n.childrenSize() == 1 && n.child(0).tagName().equalsIgnoreCase("br"))
				.forEach(Node::remove);

		// remove custom formatting from inline tables
		document.select("table:not([class])")
				.forEach(t -> t.select("tr").removeAttr("style"));

		// special magic formatting for special style Liandri wiki tables
		document.select("table[style*=\"background: #ddd\"],table.infobox")
				.forEach(t -> {
					t.removeAttr("style");
					t.addClass("meta");
					t.select("td[style*=\"font-size\"]").removeAttr("style");
					t.select("td div[style*=\"font-size\"]").removeAttr("style");
				});

		// formatting for thumbnails
		document.select(".thumb .thumbinner")
				.forEach(t -> t.addClass("meta"));

		// remove useless magnifier elements
		document.select("div.magnify").remove();
	}

	private void copyFiles(WikiRepository.Wiki wiki, WikiPage page, Path imagesPath) throws IOException {
		Path sourceRoot = wiki.path.resolve("content").resolve(wiki.imagesPath);
		for (String image : page.parse.images) {
			if (Files.exists(sourceRoot.resolve(image))
				&& !Files.exists(imagesPath.resolve(image))) {
				Files.copy(sourceRoot.resolve(image), imagesPath.resolve(image), StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

}
