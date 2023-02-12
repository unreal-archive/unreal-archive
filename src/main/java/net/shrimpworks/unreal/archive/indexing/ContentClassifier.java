package net.shrimpworks.unreal.archive.indexing;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.Map;

import net.shrimpworks.unreal.archive.common.Util;
import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.ContentType;
import net.shrimpworks.unreal.archive.indexing.mappacks.MapPackClassifier;
import net.shrimpworks.unreal.archive.indexing.mappacks.MapPackIndexHandler;
import net.shrimpworks.unreal.archive.indexing.maps.MapClassifier;
import net.shrimpworks.unreal.archive.indexing.maps.MapIndexHandler;
import net.shrimpworks.unreal.archive.indexing.models.ModelClassifier;
import net.shrimpworks.unreal.archive.indexing.models.ModelIndexHandler;
import net.shrimpworks.unreal.archive.indexing.mutators.MutatorClassifier;
import net.shrimpworks.unreal.archive.indexing.mutators.MutatorIndexHandler;
import net.shrimpworks.unreal.archive.indexing.skins.SkinClassifier;
import net.shrimpworks.unreal.archive.indexing.skins.SkinIndexHandler;
import net.shrimpworks.unreal.archive.indexing.voices.VoiceClassifier;
import net.shrimpworks.unreal.archive.indexing.voices.VoiceIndexHandler;

public class ContentClassifier {

	private static final Map<ContentType, ContentIdentifier> contentTypes = Map.of(
		ContentType.MAP,
		new ContentIdentifier(ContentType.MAP, new MapClassifier(), new MapIndexHandler.MapIndexHandlerFactory()),
		ContentType.MAP_PACK,
		new ContentIdentifier(ContentType.MAP_PACK, new MapPackClassifier(), new MapPackIndexHandler.MapPackIndexHandlerFactory()),
		ContentType.SKIN,
		new ContentIdentifier(ContentType.SKIN, new SkinClassifier(), new SkinIndexHandler.SkinIndexHandlerFactory()),
		ContentType.MODEL,
		new ContentIdentifier(ContentType.MODEL, new ModelClassifier(), new ModelIndexHandler.ModelIndexHandlerFactory()),
		ContentType.VOICE,
		new ContentIdentifier(ContentType.VOICE, new VoiceClassifier(), new VoiceIndexHandler.ModelIndexHandlerFactory()),
		ContentType.MUTATOR,
		new ContentIdentifier(ContentType.MUTATOR, new MutatorClassifier(), new MutatorIndexHandler.ModelIndexHandlerFactory()),
		ContentType.MOD,
		new ContentIdentifier(ContentType.MOD, new Classifier.NoOpClassifier(), new IndexHandler.NoOpIndexHandlerFactory()),
		ContentType.UNKNOWN,
		new ContentIdentifier(ContentType.UNKNOWN, new Classifier.NoOpClassifier(), new IndexHandler.NoOpIndexHandlerFactory())
	);

	public static ContentIdentifier classify(Incoming incoming) {
		String overrideType = incoming.submission.override.get("contentType", null);

		if (overrideType != null) return contentTypes.get(ContentType.valueOf(overrideType.toUpperCase()));

		for (ContentIdentifier type : contentTypes.values()) {
			if (type.classifier.classify(incoming)) {
				return type;
			}
		}

		incoming.log.log(IndexLog.EntryType.FATAL, "Unable to classify content in " + incoming.submission.filePath);

		return contentTypes.get(ContentType.UNKNOWN);
	}

	public static ContentIdentifier identifierForType(ContentType type) {
		return contentTypes.get(type);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Content> T newContent(ContentIdentifier type, Incoming incoming) {
		try {
			T newInstance = (T)type.contentType.contentClass.getDeclaredConstructor().newInstance();

			newInstance.contentType = type.contentType.name();

			if (incoming != null) {
				newInstance.name = Util.plainName(incoming.submission.filePath);
				newInstance.hash = incoming.hash;
				newInstance.originalFilename = Util.fileName(incoming.submission.filePath);
				newInstance.fileSize = incoming.fileSize;

				// populate a couple of basic overrides
				newInstance.game = incoming.submission.override.get("game", "Unknown");
				newInstance.author = incoming.submission.override.get("author", "Unknown");

				LocalDateTime releaseDate = null;
				// populate list of interesting files
				for (Incoming.IncomingFile f : incoming.files(Incoming.FileType.ALL)) {
					if (!Incoming.FileType.important(f.file)) {
						newInstance.otherFiles++;
						continue;
					}

					try {
						newInstance.files.add(new Content.ContentFile(f.fileName(), f.fileSize(), f.hash()));

						// try to find the newest possible file within this archive to use as the release date
						// exclude umods because they're not specifically content, and their download date would be used
						if (!Incoming.FileType.UMOD.matches(f.file)
							&& (releaseDate == null || releaseDate.isBefore(f.fileDate()))) {
							releaseDate = f.fileDate();
						}
					} catch (Exception ex) {
						incoming.log.log(IndexLog.EntryType.CONTINUE, String.format("Failed collecting content files for %s",
																					incoming.submission.filePath), ex);
					}
				}
				if (newInstance.releaseDate.equals("Unknown") && releaseDate != null) {
					newInstance.releaseDate = Content.RELEASE_DATE_FMT.format(releaseDate);
				}
			}

			newInstance.firstIndex = LocalDateTime.now();

			return newInstance;
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
			throw new RuntimeException("Failed to create content instance of type " + type.contentType.contentClass.getSimpleName(), e);
		}
	}

	public record ContentIdentifier(
		ContentType contentType,
		Classifier classifier,
		IndexHandler.IndexHandlerFactory<? extends Content> indexer
	) {}

}
