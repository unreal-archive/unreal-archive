package net.shrimpworks.unreal.archive.maps;

import net.shrimpworks.unreal.archive.ContentClassifier;
import net.shrimpworks.unreal.archive.ContentSubmission;

public class MapClassifier implements ContentClassifier.Classifier {

	@Override
	public boolean classify(ContentSubmission submission) {
		return false;
	}
}
