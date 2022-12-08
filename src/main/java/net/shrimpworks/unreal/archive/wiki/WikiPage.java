package net.shrimpworks.unreal.archive.wiki;

import java.beans.ConstructorProperties;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WikiPage {

	public final WikiParse parse;

	public String name;
	public WikiPageRevision revision;
	public ZonedDateTime timestamp;
	public transient boolean isRedirect;

	@ConstructorProperties({ "parse", "name", "revision", "timestamp" })
	public WikiPage(WikiParse parse, String name, WikiPageRevision revision, ZonedDateTime timestamp) {
		this.parse = parse;
		this.name = name;
		this.revision = revision;
		this.timestamp = timestamp;
	}

	public static class WikiParse {

		public final String title;
		public final int revId;
		public final WikiText text;
		public final Set<WikiCategory> categories;
		public final Set<WikiLink> links;
		public final Set<WikiTemplate> templates;
		public final Set<String> images;
		public final Set<String> externallinks;
		public final List<WikiSection> sections;
		public final String displaytitle;
		public final Set<WikiIWLink> iwlinks;
		public final WikiText wikitext;
		public final Set<WikiProperty> properties;

		@ConstructorProperties({
			"title", "revid", "text", "categories", "links", "templates", "images", "externallinks", "sections", "displaytitle",
			"iwlinks", "wikitext", "properties"
		})
		public WikiParse(String title, int revId, WikiText text, Set<WikiCategory> categories, Set<WikiLink> links,
						 Set<WikiTemplate> templates, Set<String> images, Set<String> externallinks, List<WikiSection> sections,
						 String displaytitle, Set<WikiIWLink> iwlinks, WikiText wikitext, Set<WikiProperty> properties) {
			this.title = title;
			this.revId = revId;
			this.text = text;
			this.categories = categories;
			this.links = links;
			this.templates = templates;
			this.images = images;
			this.externallinks = externallinks;
			this.sections = sections;
			this.displaytitle = displaytitle;
			this.iwlinks = iwlinks;
			this.wikitext = wikitext;
			this.properties = properties;
		}
	}

	public static class WikiText {

		public final String text;

		@ConstructorProperties({ "*", "text" })
		public WikiText(String asterisk, String text) {
			this.text = text == null ? asterisk : text;
		}
	}

	public static class WikiCategory {

		public final String sortkey;
		public final String name;

		@ConstructorProperties({ "sortkey", "*", "name" })
		public WikiCategory(String sortkey, String asterisk, String name) {
			this.sortkey = sortkey;
			this.name = name == null ? asterisk : name;
		}
	}

	public static class WikiLink {

		public final int ns;
		public final boolean exists;
		public final String name;

		@ConstructorProperties({ "ns", "exists", "*", "name" })
		public WikiLink(int ns, String exists, String asterisk, String name) {
			this.ns = ns;
			this.exists = exists != null && (exists.isBlank() || exists.equalsIgnoreCase("true"));
			this.name = name == null ? asterisk : name;
		}
	}

	public static class WikiTemplate {

		public final int ns;
		public final boolean exists;
		public final String name;

		@ConstructorProperties({ "ns", "exists", "*", "name" })
		public WikiTemplate(int ns, String exists, String asterisk, String name) {
			this.ns = ns;
			this.exists = exists != null && (exists.isBlank() || exists.equalsIgnoreCase("true"));
			this.name = name == null ? asterisk : name;
		}
	}

	public static class WikiSection {

		public final int toclevel;
		public final String level;
		public final String line;
		public final String number;
		public final String index;
		public final String fromtitle;
		public final int byteoffset;
		public final String anchor;

		@ConstructorProperties({ "toclevel", "level", "line", "number", "index", "fromtitle", "byteoffset", "anchor" })
		public WikiSection(int toclevel, String level, String line, String number, String index, String fromtitle, int byteoffset,
						   String anchor) {
			this.toclevel = toclevel;
			this.level = level;
			this.line = line;
			this.number = number;
			this.index = index;
			this.fromtitle = fromtitle;
			this.byteoffset = byteoffset;
			this.anchor = anchor;
		}
	}

	public static class WikiIWLink {

		public final String prefix;
		public final String url;
		public final String name;

		@ConstructorProperties({ "prefix", "url", "*", "name" })
		public WikiIWLink(String prefix, String url, String asterisk, String name) {
			this.prefix = prefix;
			this.url = url;
			this.name = name == null ? asterisk : name;
		}
	}

	public static class WikiProperty {

		public final String name;
		public final String value;

		@ConstructorProperties({ "name", "*", "value" })
		public WikiProperty(String name, String asterisk, String value) {
			this.name = name;
			this.value = value == null ? asterisk : value;
		}
	}

	public static class WikiQueryResult {

		public final WikiQuery query;

		@ConstructorProperties("query")
		public WikiQueryResult(WikiQuery query) {
			this.query = query;
		}
	}

	public static class WikiQuery {

		public final Map<String, WikiPageInfo> pages;

		@ConstructorProperties("pages")
		public WikiQuery(Map<String, WikiPageInfo> pages) {
			this.pages = pages;
		}
	}

	public static class WikiPageInfo {

		public final int pageid;
		public final int ns;
		public final String title;
		public final List<WikiPageRevision> revisions;
		public final boolean missing;

		@ConstructorProperties({ "pageid", "ns", "title", "revisions", "missing" })
		public WikiPageInfo(int pageid, int ns, String title, List<WikiPageRevision> revisions, String missing) {
			this.pageid = pageid;
			this.ns = ns;
			this.title = title;
			this.revisions = revisions;
			this.missing = missing != null && (missing.isBlank() || missing.equalsIgnoreCase("true"));
		}
	}

	public static class WikiPageRevision {

		public final int revid;
		public final int parentid;
		public final String user;
		public final ZonedDateTime timestamp;
		public final String comment;

		@ConstructorProperties({ "revid", "parentid", "user", "timestamp", "comment" })
		public WikiPageRevision(int revid, int parentid, String user, ZonedDateTime timestamp, String comment) {
			this.revid = revid;
			this.parentid = parentid;
			this.user = user;
			this.timestamp = timestamp;
			this.comment = comment;
		}
	}
}
