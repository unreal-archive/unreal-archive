package org.unrealarchive.www;

import java.util.HashMap;
import java.util.Map;

public class SiteFeatures {

	public final boolean localImages;
	public final boolean latest;
	public final boolean submit;
	public final boolean search;
	public final boolean files;
	public final boolean wikis;
	public final boolean umod;
	public final boolean collections;

	/**
	 * Collection of URL attachment rewrite rules.
	 * <p>
	 * Replaces substring in attachment URLs with a specified replacement.
	 */
	public final Map<String, String> attachmentRewrites;

	public static final SiteFeatures ALL = new SiteFeatures(true, true, true, true, true, true, true, true, Map.of());

	public SiteFeatures(boolean localImages, boolean latest, boolean submit, boolean search, boolean files, boolean wikis, boolean umod,
						boolean collections, Map<String, String> attachmentRewrites) {
		this.localImages = localImages;
		this.latest = latest;
		this.submit = submit;
		this.search = search;
		this.files = files;
		this.wikis = wikis;
		this.umod = umod;
		this.collections = collections;
		this.attachmentRewrites = attachmentRewrites != null ? attachmentRewrites : new HashMap<>();
	}
}
