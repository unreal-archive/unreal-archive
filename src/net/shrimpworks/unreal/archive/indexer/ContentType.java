package net.shrimpworks.unreal.archive.indexer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.indexer.maps.Map;
import net.shrimpworks.unreal.archive.indexer.maps.MapClassifier;
import net.shrimpworks.unreal.archive.indexer.maps.MapIndexer;
import net.shrimpworks.unreal.archive.indexer.skins.Skin;
import net.shrimpworks.unreal.archive.indexer.skins.SkinClassifier;
import net.shrimpworks.unreal.archive.indexer.skins.SkinIndexer;
import net.shrimpworks.unreal.packages.Umod;

public enum ContentType {
	MAP(new MapClassifier(), new MapIndexer.MapIndexerFactory(), Map.class),
	MAP_PACK(new Classifier.NoOpClassifier(), new Indexer.NoOpIndexerFactory(), UnknownContent.class),
	SKIN(new SkinClassifier(), new SkinIndexer.SkinIndexerFactory(), Skin.class),
	MODEL(new Classifier.NoOpClassifier(), new Indexer.NoOpIndexerFactory(), UnknownContent.class),
	VOICE(new Classifier.NoOpClassifier(), new Indexer.NoOpIndexerFactory(), UnknownContent.class),
	MUTATOR(new Classifier.NoOpClassifier(), new Indexer.NoOpIndexerFactory(), UnknownContent.class),
	MOD(new Classifier.NoOpClassifier(), new Indexer.NoOpIndexerFactory(), UnknownContent.class),
	UNKNOWN(new Classifier.NoOpClassifier(), new Indexer.NoOpIndexerFactory(), UnknownContent.class),
	;

	public final Classifier classifier;
	public final Indexer.IndexerFactory<? extends Content> indexer;
	public final Class<? extends Content> contentClass;

	ContentType(
			Classifier classifier, Indexer.IndexerFactory<? extends Content> indexer,
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
					if (!Classifier.KNOWN_FILES.contains(Util.extension(e.getKey()))) {
						newInstance.otherFiles++;
						continue;
					}

					try {
						if (e.getValue() instanceof Path) {
							newInstance.files.add(new Content.ContentFile(
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
							newInstance.files.add(new Content.ContentFile(
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
