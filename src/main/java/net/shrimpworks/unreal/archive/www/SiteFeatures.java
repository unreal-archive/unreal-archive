package net.shrimpworks.unreal.archive.www;

public class SiteFeatures {

	public final boolean localImages;
	public final boolean latest;
	public final boolean submit;
	public final boolean search;
	public final boolean files;

	public static final SiteFeatures ALL = new SiteFeatures(true, true, true, true, true);

	public SiteFeatures(boolean localImages, boolean latest, boolean submit, boolean search, boolean files) {
		this.localImages = localImages;
		this.latest = latest;
		this.submit = submit;
		this.search = search;
		this.files = files;
	}
}
