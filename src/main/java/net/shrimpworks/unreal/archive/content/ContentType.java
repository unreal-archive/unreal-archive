package net.shrimpworks.unreal.archive.content;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.content.mappacks.MapPack;
import net.shrimpworks.unreal.archive.content.mappacks.MapPackClassifier;
import net.shrimpworks.unreal.archive.content.mappacks.MapPackIndexHandler;
import net.shrimpworks.unreal.archive.content.maps.Map;
import net.shrimpworks.unreal.archive.content.maps.MapClassifier;
import net.shrimpworks.unreal.archive.content.maps.MapIndexHandler;
import net.shrimpworks.unreal.archive.content.models.Model;
import net.shrimpworks.unreal.archive.content.models.ModelClassifier;
import net.shrimpworks.unreal.archive.content.models.ModelIndexHandler;
import net.shrimpworks.unreal.archive.content.mutators.Mutator;
import net.shrimpworks.unreal.archive.content.mutators.MutatorClassifier;
import net.shrimpworks.unreal.archive.content.mutators.MutatorIndexHandler;
import net.shrimpworks.unreal.archive.content.skins.Skin;
import net.shrimpworks.unreal.archive.content.skins.SkinClassifier;
import net.shrimpworks.unreal.archive.content.skins.SkinIndexHandler;
import net.shrimpworks.unreal.archive.content.voices.Voice;
import net.shrimpworks.unreal.archive.content.voices.VoiceClassifier;
import net.shrimpworks.unreal.archive.content.voices.VoiceIndexHandler;

public enum ContentType {
	MAP(new MapClassifier(), new MapIndexHandler.MapIndexHandlerFactory(), Map.class),
	MAP_PACK(new MapPackClassifier(), new MapPackIndexHandler.MapPackIndesHandlerFactory(), MapPack.class),
	SKIN(new SkinClassifier(), new SkinIndexHandler.SkinIndexHandlerFactory(), Skin.class),
	MODEL(new ModelClassifier(), new ModelIndexHandler.ModelIndexHandlerFactory(), Model.class),
	VOICE(new VoiceClassifier(), new VoiceIndexHandler.ModelIndexHandlerFactory(), Voice.class),
	MUTATOR(new MutatorClassifier(), new MutatorIndexHandler.ModelIndexHandlerFactory(), Mutator.class),
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
			T newInstance = (T)contentClass.getDeclaredConstructor().newInstance();

			newInstance.contentType = this.name();

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
			throw new RuntimeException("Failed to create content instance of type " + contentClass.getSimpleName(), e);
		}
	}
}
