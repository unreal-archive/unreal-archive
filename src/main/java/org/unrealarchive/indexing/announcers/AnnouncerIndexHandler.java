package org.unrealarchive.indexing.announcers;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import net.shrimpworks.unreal.packages.IntFile;

import org.unrealarchive.content.FileType;
import org.unrealarchive.content.NameDescription;
import org.unrealarchive.content.addons.Addon;
import org.unrealarchive.content.addons.Announcer;
import org.unrealarchive.indexing.Incoming;
import org.unrealarchive.indexing.IndexHandler;
import org.unrealarchive.indexing.IndexLog;
import org.unrealarchive.indexing.IndexResult;
import org.unrealarchive.indexing.IndexUtils;

public class AnnouncerIndexHandler implements IndexHandler<Announcer> {

	public static class AnnouncerIndexHandlerFactory implements IndexHandlerFactory<Announcer> {

		@Override
		public IndexHandler<Announcer> get() {
			return new AnnouncerIndexHandler();
		}
	}

	@Override
	public void index(Incoming incoming, Addon current, Consumer<IndexResult<Announcer>> completed) {
		Announcer a = (Announcer)current;
		IndexLog log = incoming.log;

		Set<IndexResult.NewAttachment> attachments = new HashSet<>();

		a.name = IndexUtils.friendlyName(a.name);

		Set<Incoming.IncomingFile> uclFiles = incoming.files(FileType.UCL);

		if (!uclFiles.isEmpty()) {
			// find mutator information via .ucl files (mutator names, descriptions, weapons and vehicles)
			readUclFiles(incoming, a, uclFiles);
		}

		// if there's only one mutator, rename package to that
		if (a.announcers.size() == 1) {
			a.name = a.announcers.get(0).name;
			a.description = a.announcers.get(0).description;
		}

		a.game = IndexUtils.game(incoming).name;

		a.author = IndexUtils.findAuthor(incoming, true);

		try {
			// see if there are any images the author may have included in the package
			List<BufferedImage> images = IndexUtils.findImageFiles(incoming);
			IndexUtils.saveImages(IndexUtils.SHOT_NAME, a, images, attachments);
		} catch (IOException e) {
			log.log(IndexLog.EntryType.CONTINUE, "Failed to save images", e);
		}

		completed.accept(new IndexResult<>(a, attachments));
	}

	private void readUclFiles(
		Incoming incoming, Announcer announcer, Set<Incoming.IncomingFile> uclFiles) {
		// search ucl files for objects describing a mutator and related things
		IndexUtils.readIntFiles(incoming, uclFiles, true)
				  .filter(Objects::nonNull)
				  .forEach(intFile -> {
					  IntFile.Section section = intFile.section("root");
					  if (section == null) return;

					  // read mutators
					  IntFile.ListValue mutators = section.asList("Announcer");
					  for (IntFile.Value value : mutators.values()) {
						  if (!(value instanceof IntFile.MapValue mapVal)) continue;
						  if (mapVal.containsKey("FallbackName")) {
							  announcer.announcers.add(new NameDescription(mapVal.get("FallbackName"),
																		   mapVal.getOrDefault("FallbackDesc", "")));
						  }
					  }
				  });
	}

}
