package net.shrimpworks.unreal.archive;

import net.shrimpworks.unreal.archive.maps.MapClassifier;
import net.shrimpworks.unreal.archive.maps.MapIndexer;

public class ContentClassifier {

	public enum ContentType {
		MAP(new MapClassifier(), new MapIndexer.MapIndexerFactory()),
		SKIN(new NoOpClassifier(), new ContentIndexer.NoOpIndexerFactory()),
		MODEL(new NoOpClassifier(), new ContentIndexer.NoOpIndexerFactory()),
		VOICE(new NoOpClassifier(), new ContentIndexer.NoOpIndexerFactory()),
		MUTATOR(new NoOpClassifier(), new ContentIndexer.NoOpIndexerFactory()),
		MOD(new NoOpClassifier(), new ContentIndexer.NoOpIndexerFactory()),
		UNKNOWN(new NoOpClassifier(), new ContentIndexer.NoOpIndexerFactory()),
		;

		public final Classifier classifier;
		public final ContentIndexer.IndexerFactory<? extends Content> indexer;

		ContentType(Classifier classifier, ContentIndexer.IndexerFactory<? extends Content> indexer) {
			this.classifier = classifier;
			this.indexer = indexer;
		}
	}

	public interface Classifier {

		public boolean classify(ContentSubmission submission);
	}

	private static class NoOpClassifier implements Classifier {

		@Override
		public boolean classify(ContentSubmission submission) {
			return false;
		}
	}
}
