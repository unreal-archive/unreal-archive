package org.unrealarchive.indexing.voices;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import net.shrimpworks.unreal.packages.IntFile;

import org.unrealarchive.content.FileType;
import org.unrealarchive.content.addons.Addon;
import org.unrealarchive.content.addons.Voice;
import org.unrealarchive.indexing.Incoming;
import org.unrealarchive.indexing.IndexHandler;
import org.unrealarchive.indexing.IndexLog;
import org.unrealarchive.indexing.IndexResult;
import org.unrealarchive.indexing.IndexUtils;

public class VoiceIndexHandler implements IndexHandler<Voice> {

	public static class VoiceIndexHandlerFactory implements IndexHandlerFactory<Voice> {

		@Override
		public IndexHandler<Voice> get() {
			return new VoiceIndexHandler();
		}
	}

	@Override
	public void index(Incoming incoming, Addon current, Consumer<IndexResult<Voice>> completed) {
		Voice v = (Voice)current;
		IndexLog log = incoming.log;

		Set<IndexResult.NewAttachment> attachments = new HashSet<>();

		String origName = v.name;

		// find voice information via .int files
		v.voices = voiceNames(incoming);

		v.voices = v.voices.stream().map(n -> {
			n = n.replaceAll("\"", "");
			if (v.name == null || v.name.equalsIgnoreCase(origName)) v.name = n;
			return n;
		}).distinct().toList();

		v.game = IndexUtils.game(incoming).name;

		v.author = IndexUtils.findAuthor(incoming, true);

		try {
			// see if there are any images the author may have included in the package
			List<BufferedImage> images = IndexUtils.findImageFiles(incoming);
			IndexUtils.saveImages(IndexUtils.SHOT_NAME, v, images, attachments);
		} catch (IOException e) {
			log.log(IndexLog.EntryType.CONTINUE, "Failed to save images", e);
		}

		completed.accept(new IndexResult<>(v, attachments));
	}

	private List<String> voiceNames(Incoming incoming) {
		return IndexUtils.readIntFiles(incoming, incoming.files(FileType.INT))
						 .filter(Objects::nonNull)
						 .flatMap(intFile -> {
							 IntFile.Section section = intFile.section("public");
							 if (section == null) return Stream.empty();

							 final Set<String> foundVoices = new HashSet<>();

							 IntFile.ListValue objects = section.asList("Object");
							 for (IntFile.Value value : objects.values()) {
								 IntFile.MapValue mapVal = (IntFile.MapValue)value;

								 if (!mapVal.containsKey("MetaClass")) continue;

								 String[] voiceClass = mapVal.getOrDefault("Name", "Package.Unknown").split("\\.");
								 String maybeName = voiceClass[voiceClass.length - 1];

								 // UT2003/4 check
								 if (mapVal.get("MetaClass").equalsIgnoreCase(VoiceClassifier.UT2_VOICE_CLASS)) {
									 // try to get name from named section in int file
									 IntFile.Section nameSection = intFile.section(maybeName);
									 if (nameSection != null) {
										 IntFile.Value nameVal = nameSection.value("VoicePackName");
										 if (nameVal instanceof IntFile.SimpleValue) {
											 maybeName = ((IntFile.SimpleValue)nameVal).value();
										 }
									 }

									 foundVoices.add(maybeName);
								 } else if (VoiceClassifier.UT_VOICE_MATCH.matcher(mapVal.get("MetaClass")).matches()) {
									 foundVoices.add(mapVal.getOrDefault("Description", maybeName));
								 }
							 }
							 return foundVoices.stream();
						 })
						 .filter(Objects::nonNull)
						 .toList();
	}

}
