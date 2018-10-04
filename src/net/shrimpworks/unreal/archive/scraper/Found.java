package net.shrimpworks.unreal.archive.scraper;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

class Found {

	public final String url;
	public final List<FoundUrl> found;

	public Found(String url, List<FoundUrl> found) {
		this.url = url;
		this.found = found;
	}

	public List<FoundUrl> files() {
		return found.stream().filter(f -> !f.dir()).collect(Collectors.toList());
	}

	public List<FoundUrl> dirs() {
		return found.stream().filter(FoundUrl::dir).collect(Collectors.toList());
	}

	@Override
	public String toString() {
		return String.format("Found [url=%s, found=%s]", url, found);
	}

	static class FoundUrl {

		public final String name;
		public String path;
		public final String url;
		public final String pageUrl;

		@JsonIgnore
		public boolean dir;

		@ConstructorProperties({ "name", "path", "url", "pageUrl" })
		public FoundUrl(String name, String path, String url, String pageUrl) {
			this.name = name;
			this.path = path;
			this.url = url;
			this.pageUrl = pageUrl;
		}

		public FoundUrl(String name, String path, String url, String pageUrl, boolean dir) {
			this.name = name;
			this.path = path;
			this.url = url;
			this.pageUrl = pageUrl;
			this.dir = dir;
		}

		public boolean dir() {
			return dir;
		}

		@Override
		public String toString() {
			return String.format("Found [dir=%s, name=%s, url=%s]", dir(), name, url);
		}
	}
}
