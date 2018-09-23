package net.shrimpworks.unreal.archive.indexer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.indexer.maps.Map;
import net.shrimpworks.unreal.archive.indexer.maps.MapClassifier;
import net.shrimpworks.unreal.archive.indexer.maps.MapIndexer;
import net.shrimpworks.unreal.archive.indexer.skins.Skin;
import net.shrimpworks.unreal.archive.indexer.skins.SkinClassifier;
import net.shrimpworks.unreal.archive.indexer.skins.SkinIndexer;
import net.shrimpworks.unreal.packages.Umod;

public class ContentClassifier {

	static final Set<String> KNOWN_FILES = new HashSet<>(Arrays.asList(
			"uxx", "unr", "umx", "usa", "uax", "u", "utx", "ut2", "ukx", "usx", "upx", "ogg", "umod"
	));

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

					// populate list of interesting files
					for (java.util.Map.Entry<String, Object> e : incoming.files.entrySet()) {
						if (!KNOWN_FILES.contains(Util.extension(e.getKey()))) {
							newInstance.otherFiles++;
							continue;
						}

						try {
							if (e.getValue() instanceof Path) {
								newInstance.files.add(new ContentFile(
										Util.fileName(e.getKey()),
										(int)Files.size((Path)e.getValue()),
										Util.hash((Path)e.getValue())
								));

								// take a guess at release date based on file modification time
								if (newInstance.releaseDate.equals("Unknown")) {
									newInstance.releaseDate = Content.RELEASE_DATE_FMT.format(
											Files.getLastModifiedTime((Path)e.getValue()).toInstant());
								}

							} else if (e.getValue() instanceof Umod.UmodFile) {
								newInstance.files.add(new ContentFile(
										Util.fileName(((Umod.UmodFile)e.getValue()).name),
										((Umod.UmodFile)e.getValue()).size,
										((Umod.UmodFile)e.getValue()).sha1()
								));
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
