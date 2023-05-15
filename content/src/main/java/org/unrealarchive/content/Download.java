package org.unrealarchive.content;

import java.beans.ConstructorProperties;
import java.util.Objects;

public class Download implements Comparable<Download> {

	public static enum DownloadState {
		OK,
		MISSING,
		DELETED
	}

	public String url;
	public final boolean main;
	public final boolean repack;
	public DownloadState state;

	@ConstructorProperties({ "url", "main", "repack", "state" })
	public Download(String url, boolean main, boolean repack, DownloadState state) {
		this.url = url;
		this.main = main;
		this.repack = repack;
		this.state = state == null ? DownloadState.OK : state;
	}

	public Download(String url, boolean repack) {
		this.url = url;
		this.repack = repack;
		this.main = false;
		this.state = DownloadState.OK;
	}

	@Override
	public int compareTo(Download o) {
		return main ? -1 : 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Download that)) return false;
		return state == that.state
			   && main == that.main
			   && repack == that.repack
			   && Objects.equals(url, that.url);
	}

	@Override
	public int hashCode() {
		return Objects.hash(url, main, repack, state);
	}

	@Override
	public String toString() {
		return String.format("Download [url=%s, main=%s, repack=%s, state=%s]", url, main, repack, state);
	}
}
