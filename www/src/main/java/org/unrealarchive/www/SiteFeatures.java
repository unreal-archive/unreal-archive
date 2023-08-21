package org.unrealarchive.www;

public class SiteFeatures {

	public final boolean localImages;
	public final boolean latest;
	public final boolean submit;
	public final boolean search;
	public final boolean files;
	public final boolean wikis;
	public final boolean umod;

	public static final SiteFeatures ALL = new SiteFeatures(true, true, true, true, true, true, true);

	public SiteFeatures(boolean localImages, boolean latest, boolean submit, boolean search, boolean files, boolean wikis, boolean umod) {
		this.localImages = localImages;
		this.latest = latest;
		this.submit = submit;
		this.search = search;
		this.files = files;
		this.wikis = wikis;
		this.umod = umod;
	}
}
