package org.unrealarchive.content;

import java.beans.ConstructorProperties;
import java.util.Objects;

public class Download implements Comparable<Download> {

	public static enum DownloadState {
		OK,
		MISSING,
	}

	public String url;
	/**
	 * Signifies that a URL points directly to a file that may be retrieved via
	 * an HTTP request, with no intermediary download pages or steps.
	 */
	public boolean direct;
	/**
	 * Indicates the last known state of a download.
	 * <p>
	 * Missing downloads will be considered for imminent removal.
	 */
	public DownloadState state;

	@ConstructorProperties({ "url", "direct", "state" })
	public Download(String url, boolean direct, DownloadState state) {
		this.url = url;
		this.direct = direct;
		this.state = state == null ? DownloadState.OK : state;
	}

	public Download(String url) {
		this.url = url;
		this.direct = false;
		this.state = DownloadState.OK;
	}

	@Override
	public int compareTo(Download o) {
		return direct ? -1 : 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Download that)) return false;
		return state == that.state
			   && direct == that.direct
			   && Objects.equals(url, that.url);
	}

	@Override
	public int hashCode() {
		return Objects.hash(url, direct, state);
	}

	@Override
	public String toString() {
		return String.format("Download [url=%s, direct=%s, state=%s]", url, direct, state);
	}
}
