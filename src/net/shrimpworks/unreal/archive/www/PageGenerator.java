package net.shrimpworks.unreal.archive.www;

public interface PageGenerator {

	/**
	 * Generate one or more HTML pages of output.
	 *
	 * @return number of individual pages created
	 */
	public int generate();

}
