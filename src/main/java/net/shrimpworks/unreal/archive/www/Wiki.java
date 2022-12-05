package net.shrimpworks.unreal.archive.www;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import net.shrimpworks.unreal.archive.wiki.WikiManager;
import net.shrimpworks.unreal.archive.wiki.WikiPage;

public class Wiki implements PageGenerator {

	private final Path root;
	private final Path siteRoot;
	private final Path staticRoot;
	private final SiteFeatures features;

	private final WikiManager wikiManager;

	public Wiki(Path output, Path staticRoot, SiteFeatures features, WikiManager wikiManager) {
		this.root = output.resolve("wiki");
		this.siteRoot = output;
		this.staticRoot = staticRoot;
		this.features = features;
		this.wikiManager = wikiManager;
	}

	@Override
	public Set<SiteMap.Page> generate() {
		Templates.PageSet pages = new Templates.PageSet("wiki", features, siteRoot, staticRoot, root);

		WikiManager.Wiki wiki = wikiManager.wiki("Unreal Wiki");

		Set<WikiPage> candidates = wiki.all().stream()
									   .filter(p -> p.parse.categories.stream().noneMatch(c -> c.name.contains("-specific_")))
									   .filter(p -> p.parse.categories.stream().noneMatch(c -> c.name.contains("Subclasses_of_")))
									   .filter(p -> p.parse.templates.stream().noneMatch(
										   c -> c.name.contains("Template:Infobox class/core"))).collect(Collectors.toSet());

		Set<String> linkingCandidates = candidates.stream().map(p -> p.name.replaceAll(" ", "_")).collect(Collectors.toSet());

		candidates
			.forEach(page -> {
				try {
					Path pagePath;
					if (page.name.contains("/")) {
						pagePath = Files.createDirectories(root.resolve(page.name.substring(0, page.name.lastIndexOf('/'))))
										.resolve(String.format(
													 "%s.html", page.name.substring(page.name.lastIndexOf('/') + 1)
												 ).replaceAll(" ", "_")
										);
					} else {
						pagePath = root.resolve(String.format("%s.html", page.name.replaceAll(" ", "_")));
					}

					pages.add("page.ftl", SiteMap.Page.of(0.75f, SiteMap.ChangeFrequency.weekly), String.join(" / ", wiki.name, page.name))
						 .put("page", sanitisedPageHtml(page, linkingCandidates))
						 .write(pagePath);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});

		return pages.pages;
	}

	private String sanitisedPageHtml(WikiPage page, Set<String> linkingCandidates) {
		Document document = Jsoup.parse(page.parse.text.text);

		// remove edit links from section titles
		document.select("span.mw-editsection").remove();

		// remove "please improve" blocks
		document.select("table.ambox").stream()
//				.filter(n -> n.outerHtml().contains("improve this article"))
				.forEach(Node::remove);

		// remove "recent edits" blocks
		document.select("#recent-edits").remove();

		// remove little edit button things in nav boxes
		document.select(".noprint.plainlinks").remove();

		// remove empty paragraphs
		document.select("p").stream()
				.filter(n -> n.childrenSize() == 1 && n.child(0).tagName().equalsIgnoreCase("br"))
				.forEach(Node::remove);

		// fix local links
		document.select("a").stream()
				.filter(a -> a.hasAttr("href") && a.attr("href").startsWith("/"))
				.forEach(a -> {
					String target = a.attr("href").substring(1);
					String targetPage = target;
					targetPage = wikiManager.wiki("Unreal Wiki").redirects.getOrDefault(targetPage, targetPage);
					if (target.indexOf("#") > 0) targetPage = target.substring(0, target.indexOf("#"));

					if (a.attr("href").contains("?redlink") || !linkingCandidates.contains(targetPage.replaceAll(" ", "_"))) {
						// remove links to pages that don't exist or we're not tracking
						a.replaceWith(new Element("span").addClass("redlink").text(a.text()));
					} else {
						// resolve redirects automatically

						if (target.indexOf("#") > 0) targetPage = String.format("%s%s", targetPage, target.substring(target.indexOf("#")));

						// make links to pages relative
						a.attr("href", String.format("./%s.html", targetPage.replaceAll(" ", "_")));
					}
				});

		// remove custom formatting from inline tables
		document.select("table:not([class])").stream()
				.forEach(t -> {
					t.select("tr").removeAttr("style");
				});

		return document.outerHtml();
	}

}
