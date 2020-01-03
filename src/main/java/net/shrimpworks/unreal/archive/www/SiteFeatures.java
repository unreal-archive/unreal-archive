package net.shrimpworks.unreal.archive.www;

public class SiteFeatures {

	public final boolean localImages;
	public final boolean latest;
	public final boolean submit;
	public final boolean search;

	public static final SiteFeatures ALL = new SiteFeatures(true, true, true, true);

	public SiteFeatures(boolean localImages, boolean latest, boolean submit, boolean search) {
		this.localImages = localImages;
		this.latest = latest;
		this.submit = submit;
		this.search = search;
	}
}
