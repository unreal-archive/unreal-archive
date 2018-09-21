package net.shrimpworks.unreal.archive.indexer.skins;

import net.shrimpworks.unreal.archive.indexer.ContentClassifier;
import net.shrimpworks.unreal.archive.indexer.Incoming;

/**
 * A skin should contain:
 * <p>
 * - At least one .utx file
 * - One .int file
 * <p>
 * The .int file should contain a [public] section, and an entry which follows the format:
 * <pre>
 * [public]
 * Object=(Name=ModelReference_Something.tex1,Class=Texture,Description="Character")
 * </pre>
 * <p>
 * If there's a .u file, or more .int files (with other contents), it's probably a model.
 */
public class SkinClassifier implements ContentClassifier.Classifier {

	private static final String TEXTURE = ".utx";
	private static final String INT = ".int";

	@Override
	public boolean classify(Incoming incoming) {
		// TODO verify content as per description above

		return false;
	}
}
