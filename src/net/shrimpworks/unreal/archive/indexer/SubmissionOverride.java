package net.shrimpworks.unreal.archive.indexer;

import java.beans.ConstructorProperties;
import java.util.Map;

/**
 * Affords an opportunity to provide somewhat loose overrides for
 * indexed data, for example Game, Game Type, Author, etc.
 * <p>
 * Still relies on implementation within an Indexer to be made use
 * of.
 */
public class SubmissionOverride {

	public final Map<String, String> overrides;

	@ConstructorProperties("overrides")
	public SubmissionOverride(Map<String, String> overrides) {
		this.overrides = overrides;
	}

	public String get(String key, String defaultValue) {
		return overrides.getOrDefault(key, defaultValue);
	}
}
