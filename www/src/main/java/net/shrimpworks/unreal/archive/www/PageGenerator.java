package net.shrimpworks.unreal.archive.www;

import java.util.Set;

public interface PageGenerator {

	/**
	 * Generate one or more HTML pages of output.
	 *
	 * @return paths to pages generated
	 */
	public Set<SiteMap.Page> generate();
}
