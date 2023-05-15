package org.unrealarchive.indexing;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.Map;

import org.unrealarchive.common.Util;
import org.unrealarchive.content.FileType;
import org.unrealarchive.content.addons.Addon;
import org.unrealarchive.content.addons.SimpleAddonType;
import org.unrealarchive.indexing.mappacks.MapPackClassifier;
import org.unrealarchive.indexing.mappacks.MapPackIndexHandler;
import org.unrealarchive.indexing.maps.MapClassifier;
import org.unrealarchive.indexing.maps.MapIndexHandler;
import org.unrealarchive.indexing.models.ModelClassifier;
import org.unrealarchive.indexing.models.ModelIndexHandler;
import org.unrealarchive.indexing.mutators.MutatorClassifier;
import org.unrealarchive.indexing.mutators.MutatorIndexHandler;
import org.unrealarchive.indexing.skins.SkinClassifier;
import org.unrealarchive.indexing.skins.SkinIndexHandler;
import org.unrealarchive.indexing.voices.VoiceClassifier;
import org.unrealarchive.indexing.voices.VoiceIndexHandler;

public class AddonClassifier {

	private static final Map<SimpleAddonType, AddonIdentifier> addonTypes = Map.of(
		SimpleAddonType.MAP,
		new AddonIdentifier(SimpleAddonType.MAP, new MapClassifier(), new MapIndexHandler.MapIndexHandlerFactory()),
		SimpleAddonType.MAP_PACK,
		new AddonIdentifier(SimpleAddonType.MAP_PACK, new MapPackClassifier(), new MapPackIndexHandler.MapPackIndexHandlerFactory()),
		SimpleAddonType.SKIN,
		new AddonIdentifier(SimpleAddonType.SKIN, new SkinClassifier(), new SkinIndexHandler.SkinIndexHandlerFactory()),
		SimpleAddonType.MODEL,
		new AddonIdentifier(SimpleAddonType.MODEL, new ModelClassifier(), new ModelIndexHandler.ModelIndexHandlerFactory()),
		SimpleAddonType.VOICE,
		new AddonIdentifier(SimpleAddonType.VOICE, new VoiceClassifier(), new VoiceIndexHandler.ModelIndexHandlerFactory()),
		SimpleAddonType.MUTATOR,
		new AddonIdentifier(SimpleAddonType.MUTATOR, new MutatorClassifier(), new MutatorIndexHandler.ModelIndexHandlerFactory()),
		SimpleAddonType.MOD,
		new AddonIdentifier(SimpleAddonType.MOD, new Classifier.NoOpClassifier(), new IndexHandler.NoOpIndexHandlerFactory()),
		SimpleAddonType.UNKNOWN,
		new AddonIdentifier(SimpleAddonType.UNKNOWN, new Classifier.NoOpClassifier(), new IndexHandler.NoOpIndexHandlerFactory())
	);

	public static AddonIdentifier classify(Incoming incoming) {
		String overrideType = incoming.submission.override.get("contentType", null);

		if (overrideType != null) return addonTypes.get(SimpleAddonType.valueOf(overrideType.toUpperCase()));

		for (AddonIdentifier type : addonTypes.values()) {
			if (type.classifier.classify(incoming)) {
				return type;
			}
		}

		incoming.log.log(IndexLog.EntryType.FATAL, "Unable to classify content in " + incoming.submission.filePath);

		return addonTypes.get(SimpleAddonType.UNKNOWN);
	}

	public static AddonIdentifier identifierForType(SimpleAddonType type) {
		return addonTypes.get(type);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Addon> T newContent(AddonIdentifier type, Incoming incoming) {
		try {
			T newInstance = (T)type.contentType.addonClass.getDeclaredConstructor().newInstance();

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
				for (Incoming.IncomingFile f : incoming.files(FileType.ALL)) {
					if (!FileType.important(f.file)) {
						newInstance.otherFiles++;
						continue;
					}

					try {
						newInstance.files.add(new Addon.ContentFile(f.fileName(), f.fileSize(), f.hash()));

						// try to find the newest possible file within this archive to use as the release date
						// exclude umods because they're not specifically content, and their download date would be used
						if (!FileType.UMOD.matches(f.file)
							&& (releaseDate == null || releaseDate.isBefore(f.fileDate()))) {
							releaseDate = f.fileDate();
						}
					} catch (Exception ex) {
						incoming.log.log(IndexLog.EntryType.CONTINUE, String.format("Failed collecting content files for %s",
																					incoming.submission.filePath), ex);
					}
				}
				if (newInstance.releaseDate.equals("Unknown") && releaseDate != null) {
					newInstance.releaseDate = Addon.RELEASE_DATE_FMT.format(releaseDate);
				}
			}

			newInstance.firstIndex = LocalDateTime.now();

			return newInstance;
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
			throw new RuntimeException("Failed to create content instance of type " + type.contentType.addonClass.getSimpleName(), e);
		}
	}

	public record AddonIdentifier(
		SimpleAddonType contentType,
		Classifier classifier,
		IndexHandler.IndexHandlerFactory<? extends Addon> indexer
	) {}

}
