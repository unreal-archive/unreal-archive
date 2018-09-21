package net.shrimpworks.unreal.archive.indexer;

import java.time.LocalDateTime;

import net.shrimpworks.unreal.archive.indexer.maps.Map;
import net.shrimpworks.unreal.archive.indexer.maps.MapClassifier;
import net.shrimpworks.unreal.archive.indexer.maps.MapIndexer;
import net.shrimpworks.unreal.archive.indexer.skins.Skin;
import net.shrimpworks.unreal.archive.indexer.skins.SkinClassifier;
import net.shrimpworks.unreal.archive.indexer.skins.SkinIndexer;

public class ContentClassifier {

	public enum ContentType {
		MAP(new MapClassifier(), new MapIndexer.MapIndexerFactory(), Map.class),
		MAP_PACK(new NoOpClassifier(), new ContentIndexer.NoOpIndexerFactory(), UnknownContent.class),
		SKIN(new SkinClassifier(), new SkinIndexer.SkinIndexerFactory(), Skin.class),
		MODEL(new NoOpClassifier(), new ContentIndexer.NoOpIndexerFactory(), UnknownContent.class),
		VOICE(new NoOpClassifier(), new ContentIndexer.NoOpIndexerFactory(), UnknownContent.class),
		MUTATOR(new NoOpClassifier(), new ContentIndexer.NoOpIndexerFactory(), UnknownContent.class),
		MOD(new NoOpClassifier(), new ContentIndexer.NoOpIndexerFactory(), UnknownContent.class),
		UNKNOWN(new NoOpClassifier(), new ContentIndexer.NoOpIndexerFactory(), UnknownContent.class),
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
					newInstance.name = incoming.submission.filePath.getFileName().toString();
					newInstance.name = newInstance.name.substring(0, newInstance.name.lastIndexOf(".")).replaceAll("/", "");
					newInstance.hash = incoming.hash;
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

		log.log(IndexLog.EntryType.FATAL, "Unable to classify content in " + incoming.submission.filePath);

		return ContentType.UNKNOWN;
	}
}
