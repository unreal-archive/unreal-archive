package net.shrimpworks.unreal.archive;

import java.beans.ConstructorProperties;
import java.time.LocalDate;

public class Download {

	public final String url;
	public final LocalDate added;
	public LocalDate lastChecked;
	public boolean ok;                // health at last check date
	public final boolean repack;

	public boolean deleted;

	@ConstructorProperties({ "url", "added", "lastChecked", "ok", "repack", "deleted" })
	public Download(String url, LocalDate added, LocalDate lastChecked, boolean ok, boolean repack, boolean deleted) {
		this.url = url;
		this.added = added;
		this.lastChecked = lastChecked;
		this.ok = ok;
		this.repack = repack;
		this.deleted = deleted;
	}

	public Download(String url, LocalDate added, boolean repack) {
		this.url = url;
		this.added = added;
		this.repack = repack;
	}
}
