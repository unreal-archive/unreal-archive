package net.shrimpworks.unreal.archive.content.wiki;

import java.beans.ConstructorProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InterWikiList {

	public final Map<String, InterWikiLink> wikis;

	@ConstructorProperties("interwikimap")
	public InterWikiList(Set<InterWikiLink> wikis) {
		this.wikis = new HashMap<>();
		wikis.forEach(l -> this.wikis.put(l.prefix, l));
	}

	public static class InterWikiLink {

		public final String prefix;
		public final String url;
		public final boolean local;
		public final boolean localInterWiki;

		@ConstructorProperties({ "prefix", "url", "local", "localinterwiki" })
		public InterWikiLink(String prefix, String url, String local, String localinterwiki) {
			this.prefix = prefix;
			this.url = url;
			this.local = local != null;
			this.localInterWiki = localinterwiki != null;
		}
	}
}
