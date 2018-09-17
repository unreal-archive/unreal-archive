package net.shrimpworks.unreal.archive;

import java.time.LocalDateTime;

import net.shrimpworks.unreal.archive.maps.Map;
import net.shrimpworks.unreal.archive.maps.MapClassifier;
import net.shrimpworks.unreal.archive.maps.MapIndexer;

public class ContentClassifier {

	public enum ContentType {
		MAP(new MapClassifier(), new MapIndexer.MapIndexerFactory(), Map.class),
		MAP_PACK(new NoOpClassifier(), new ContentIndexer.NoOpIndexerFactory(), Content.class),
		SKIN(new NoOpClassifier(), new ContentIndexer.NoOpIndexerFactory(), Content.class),
		MODEL(new NoOpClassifier(), new ContentIndexer.NoOpIndexerFactory(), Content.class),
		VOICE(new NoOpClassifier(), new ContentIndexer.NoOpIndexerFactory(), Content.class),
		MUTATOR(new NoOpClassifier(), new ContentIndexer.NoOpIndexerFactory(), Content.class),
		MOD(new NoOpClassifier(), new ContentIndexer.NoOpIndexerFactory(), Content.class),
		UNKNOWN(new NoOpClassifier(), new ContentIndexer.NoOpIndexerFactory(), Content.class),
		;

		public final Classifier classifier;
		public final ContentIndexer.IndexerFactory<? extends Content> indexer;
		public final Class<? extends Content> contentClass;

		ContentType(
				Classifier classifier, ContentIndexer.IndexerFactory<? extends Content> indexer,
				Class<? extends Content> contentClass) {
			this.classifier = classifier;
			this.indexer = indexer;
			this.contentClass = contentClass;
		}

		@SuppressWarnings("unchecked")
		public <T extends Content> T newContent(Incoming incoming) {
			try {
				T newInstance = (T)contentClass.newInstance();

				newInstance.contentType = this.name();

				if (incoming != null) {
					newInstance.sha1 = incoming.originalSha1;
					newInstance.originalFilename = incoming.submission.filePath.toString();
					newInstance.fileSize = incoming.fileSize;
				}

				newInstance.firstIndex = LocalDateTime.now();

				return newInstance;
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException("Failed to create content instance of type " + contentClass.getSimpleName(), e);
			}
		}
	}

	public interface Classifier {

		public boolean classify(Incoming incoming);
	}

	private static class NoOpClassifier implements Classifier {

		@Override
		public boolean classify(Incoming incoming) {
			return false;
		}
	}

	public static ContentType classify(Incoming incoming, IndexLog log) {
		for (ContentType type : ContentType.values()) {
			if (type.classifier.classify(incoming)) {
				return type;
			}
		}

		log.log(IndexLog.EntryType.FATAL, "Unable to classify content " + incoming);

		return ContentType.UNKNOWN;
	}
}
