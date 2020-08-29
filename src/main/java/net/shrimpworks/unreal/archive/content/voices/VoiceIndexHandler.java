package net.shrimpworks.unreal.archive.content.voices;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.Incoming;
import net.shrimpworks.unreal.archive.content.IndexHandler;
import net.shrimpworks.unreal.archive.content.IndexLog;
import net.shrimpworks.unreal.archive.content.IndexResult;
import net.shrimpworks.unreal.archive.content.IndexUtils;
import net.shrimpworks.unreal.packages.IntFile;

public class VoiceIndexHandler implements IndexHandler<Voice> {

	public static class ModelIndexHandlerFactory implements IndexHandlerFactory<Voice> {

		@Override
		public IndexHandler<Voice> get() {
			return new VoiceIndexHandler();
		}
	}

	@Override
	public void index(Incoming incoming, Content current, Consumer<IndexResult<Voice>> completed) {
		Voice v = (Voice)current;
		IndexLog log = incoming.log;

		Set<IndexResult.NewAttachment> attachments = new HashSet<>();

		String origName = v.name;

		// find voice information via .int files
		v.voices = voiceNames(incoming);

		v.voices.forEach(n -> {
			if (v.name == null || v.name.equalsIgnoreCase(origName)) {
				v.name = n.replaceAll("\"", "");
			}
		});

		try {
			if (v.releaseDate != null && v.releaseDate.compareTo(IndexUtils.RELEASE_UT99) < 0) v.game = "Unreal";
			v.game = game(incoming);
		} catch (Exception e) {
			log.log(IndexLog.EntryType.CONTINUE, "Could not determine game for model", e);
		}

		try {
			v.author = IndexUtils.findAuthor(incoming, true);
		} catch (IOException e) {
			log.log(IndexLog.EntryType.CONTINUE, "Failed attempt to read author", e);
		}

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
		return IndexUtils.readIntFiles(incoming, incoming.files(Incoming.FileType.INT))
						 .filter(Objects::nonNull)
						 .map(intFile -> {
							 IntFile.Section section = intFile.section("public");
							 if (section == null) return null;

							 IntFile.ListValue objects = section.asList("Object");
							 for (IntFile.Value value : objects.values) {
								 IntFile.MapValue mapVal = (IntFile.MapValue)value;

								 if (!mapVal.containsKey("MetaClass")) return null;

								 String[] voiceClass = mapVal.getOrDefault("Name", "Package.Unknown").split("\\.");
								 String maybeName = voiceClass[1];

								 // UT2003/4 check
								 if (mapVal.get("MetaClass").equalsIgnoreCase(Voice.UT2_VOICE_CLASS)) {
									 // try to get name from named section in int file
									 IntFile.Section nameSection = intFile.section(maybeName);
									 if (nameSection != null) {
										 IntFile.Value nameVal = nameSection.value("VoicePackName");
										 if (nameVal instanceof IntFile.SimpleValue) {
											 return ((IntFile.SimpleValue)nameVal).value;
										 }
									 }

									 return maybeName;
								 } else if (Voice.UT_VOICE_MATCH.matcher(mapVal.get("MetaClass")).matches()) {
									 return mapVal.getOrDefault("Description", maybeName);
								 }
							 }
							 return null;
						 })
						 .filter(Objects::nonNull)
						 .collect(Collectors.toList());
	}

	private String game(Incoming incoming) throws IOException {
		if (incoming.submission.override.get("game", null) != null) return incoming.submission.override.get("game", "Unreal Tournament");

		return IndexUtils.game(incoming.files(Incoming.FileType.PACKAGES));
	}

}
