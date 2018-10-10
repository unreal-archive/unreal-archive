package net.shrimpworks.unreal.archive.indexer;

import java.time.LocalDateTime;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.indexer.mappacks.MapPack;
import net.shrimpworks.unreal.archive.indexer.mappacks.MapPackClassifier;
import net.shrimpworks.unreal.archive.indexer.mappacks.MapPackIndesHandler;
import net.shrimpworks.unreal.archive.indexer.maps.Map;
import net.shrimpworks.unreal.archive.indexer.maps.MapClassifier;
import net.shrimpworks.unreal.archive.indexer.maps.MapIndexHandler;
import net.shrimpworks.unreal.archive.indexer.skins.Skin;
import net.shrimpworks.unreal.archive.indexer.skins.SkinClassifier;
import net.shrimpworks.unreal.archive.indexer.skins.SkinIndexHandler;

public enum ContentType {
	MAP(new MapClassifier(), new MapIndexHandler.MapIndexHandlerFactory(), Map.class),
	MAP_PACK(new MapPackClassifier(), new MapPackIndesHandler.MapPackIndesHandlerFactory(), MapPack.class),
	SKIN(new SkinClassifier(), new SkinIndexHandler.SkinIndexHandlerFactory(), Skin.class),
	MODEL(new Classifier.NoOpClassifier(), new IndexHandler.NoOpIndexHandlerFactory(), UnknownContent.class),
	VOICE(new Classifier.NoOpClassifier(), new IndexHandler.NoOpIndexHandlerFactory(), UnknownContent.class),
	MUTATOR(new Classifier.NoOpClassifier(), new IndexHandler.NoOpIndexHandlerFactory(), UnknownContent.class),
	MOD(new Classifier.NoOpClassifier(), new IndexHandler.NoOpIndexHandlerFactory(), UnknownContent.class),
	UNKNOWN(new Classifier.NoOpClassifier(), new IndexHandler.NoOpIndexHandlerFactory(), UnknownContent.class),
	;

	public final Classifier classifier;
	public final IndexHandler.IndexHandlerFactory<? extends Content> indexer;
	public final Class<? extends Content> contentClass;

	ContentType(
			Classifier classifier, IndexHandler.IndexHandlerFactory<? extends Content> indexer,
			Class<? extends Content> contentClass) {
		this.classifier = classifier;
		this.indexer = indexer;
		this.contentClass = contentClass;
	}

	public static ContentType classify(Incoming incoming) {
		String overrideType = incoming.submission.override.get("contentType", null);

		if (overrideType != null) return ContentType.valueOf(overrideType.toUpperCase());

		for (ContentType type : ContentType.values()) {
			if (type.classifier.classify(incoming)) {
				return type;
			}
		}

		incoming.log.log(IndexLog.EntryType.FATAL, "Unable to classify content in " + incoming.submission.filePath);

		return ContentType.UNKNOWN;
	}

	@SuppressWarnings("unchecked")
	public <T extends Content> T newContent(Incoming incoming) {
		try {
			T newInstance = (T)contentClass.newInstance();

			newInstance.contentType = this.name();

			if (incoming != null) {
				newInstance.name = Util.plainName(incoming.submission.filePath);
				newInstance.hash = incoming.hash;
				newInstance.originalFilename = Util.fileName(incoming.submission.filePath);
				newInstance.fileSize = incoming.fileSize;

				// populate a couple of basic overrides
				newInstance.game = incoming.submission.override.get("game", "Unknown");
				newInstance.author = incoming.submission.override.get("author", "Unknown");

				// populate list of interesting files
				for (Incoming.IncomingFile f : incoming.files(Incoming.FileType.ALL)) {
					if (!Incoming.FileType.important(f.file)) {
						newInstance.otherFiles++;
						continue;
					}

					try {
						newInstance.files.add(new Content.ContentFile(f.fileName(), f.fileSize(), f.hash()));
						if (newInstance.releaseDate.equals("Unknown")) {
							newInstance.releaseDate = Content.RELEASE_DATE_FMT.format(f.fileDate());
						}
					} catch (Exception ex) {
//							log.log(IndexLog.EntryType.CONTINUE, "Failed getting data for " + e.getKey(), ex);
						// TODO expose log via incoming
					}
				}
			}

			newInstance.firstIndex = LocalDateTime.now();

			return newInstance;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException("Failed to create content instance of type " + contentClass.getSimpleName(), e);
		}
	}
}
