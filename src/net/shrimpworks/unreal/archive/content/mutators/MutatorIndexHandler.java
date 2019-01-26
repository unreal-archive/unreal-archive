package net.shrimpworks.unreal.archive.content.mutators;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import net.shrimpworks.unreal.archive.YAML;
import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.Incoming;
import net.shrimpworks.unreal.archive.content.IndexHandler;
import net.shrimpworks.unreal.archive.content.IndexLog;
import net.shrimpworks.unreal.archive.content.IndexResult;
import net.shrimpworks.unreal.archive.content.IndexUtils;
import net.shrimpworks.unreal.packages.IntFile;

public class MutatorIndexHandler implements IndexHandler<Mutator> {

	public static class ModelIndexHandlerFactory implements IndexHandlerFactory<Mutator> {

		@Override
		public IndexHandler<Mutator> get() {
			return new MutatorIndexHandler();
		}
	}

	@Override
	public void index(Incoming incoming, Content current, Consumer<IndexResult<Mutator>> completed) {
		Mutator m = (Mutator)current;
		IndexLog log = incoming.log;

		Set<IndexResult.NewAttachment> attachments = new HashSet<>();

		m.name = IndexUtils.friendlyName(m.name);

		Set<Incoming.IncomingFile> uclFiles = incoming.files(Incoming.FileType.UCL);
		Set<Incoming.IncomingFile> intFiles = incoming.files(Incoming.FileType.INT);

		if (!uclFiles.isEmpty()) {
			// find mutator information via .ucl files (mutator names, descriptions, weapons and vehicles)

		} else if (!intFiles.isEmpty()) {
			// find mutator information via .int files (weapon names, menus, keybindings)
			readIntFiles(incoming, m, intFiles);
		}

		// if there's only one mutator, rename package to that
		if (m.mutators.size() == 1) m.name = m.mutators.get(0).name;

		try {
			if (m.releaseDate != null && m.releaseDate.compareTo(IndexUtils.RELEASE_UT99) < 0) m.game = "Unreal";
			m.game = game(incoming);
		} catch (Exception e) {
			log.log(IndexLog.EntryType.CONTINUE, "Could not determine game for mutator", e);
		}

		try {
			m.author = IndexUtils.findAuthor(incoming, true);
		} catch (IOException e) {
			log.log(IndexLog.EntryType.CONTINUE, "Failed attempt to read author", e);
		}

		try {
			// see if there are any images the author may have included in the package
			List<BufferedImage> images = IndexUtils.findImageFiles(incoming);
			IndexUtils.saveImages(IndexUtils.SHOT_NAME, m, images, attachments);
		} catch (IOException e) {
			log.log(IndexLog.EntryType.CONTINUE, "Failed to save images", e);
		}

		try {
			System.out.println(YAML.toString(m));
		} catch (IOException e) {
			e.printStackTrace();
		}
		//completed.accept(new IndexResult<>(m, attachments));
	}

	private String game(Incoming incoming) throws IOException {
		if (incoming.submission.override.get("game", null) != null) return incoming.submission.override.get("game", "Unreal Tournament");

		return IndexUtils.game(incoming.files(Incoming.FileType.PACKAGES));
	}

	private void readIntFiles(Incoming incoming, Mutator mutator, Set<Incoming.IncomingFile> intFiles) {
		// search int files for objects describing a skin
		IndexUtils.readIntFiles(incoming, intFiles)
				  .filter(Objects::nonNull)
				  .forEach(intFile -> {
					  IntFile.Section section = intFile.section("public");
					  if (section == null) return;

					  IntFile.ListValue objects = section.asList("Object");
					  for (IntFile.Value value : objects.values) {
						  if (!(value instanceof IntFile.MapValue)) continue;
						  IntFile.MapValue mapVal = (IntFile.MapValue)value;

						  if (!mapVal.containsKey("MetaClass")) continue;

						  if (Mutator.UT_MUTATOR_CLASS.equalsIgnoreCase(mapVal.get("MetaClass"))) {
							  mutator.mutators.add(new Mutator.NameDescription(mapVal.get("Description")));
						  } else if (Mutator.UT_WEAPON_CLASS.equalsIgnoreCase(mapVal.get("MetaClass"))) {
							  mutator.weapons.add(new Mutator.NameDescription(mapVal.get("Description")));
						  } else if (Mutator.UT_KEYBINDINGS_CLASS.equalsIgnoreCase(mapVal.get("MetaClass"))) {
							  mutator.hasKeybinds = true;
						  } else if (Mutator.UT_MENU_CLASS.equalsIgnoreCase(mapVal.get("MetaClass"))) {
							  mutator.hasConfigMenu = true;
						  }
					  }
				  });
	}

}
