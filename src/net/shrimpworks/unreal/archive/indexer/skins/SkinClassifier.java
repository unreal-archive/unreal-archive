package net.shrimpworks.unreal.archive.indexer.skins;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Matcher;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.indexer.ContentClassifier;
import net.shrimpworks.unreal.archive.indexer.Incoming;
import net.shrimpworks.unreal.packages.IntFile;
import net.shrimpworks.unreal.packages.Umod;

/**
 * A skin should contain:
 * <p>
 * - At least one .utx file
 * - One .int file
 * <p>
 * The .int file should contain a [public] section, and an entry which follows the format:
 * <pre>
 * [public]
 * Object=(Name=ModelReference_Something.tex1,Class=Texture,Description="Character")
 * </pre>
 * <p>
 * If there's a .u file, or more .int files (with other contents), it's probably a model.
 */
public class SkinClassifier implements ContentClassifier.Classifier {

	@Override
	public boolean classify(Incoming incoming) {
		// TODO verify content as per description above

		// count all map files in the archive
		long ints = incoming.files.keySet().stream()
								  .filter(f -> Util.extension(f).equalsIgnoreCase(Skin.INT))
								  .count();

		if (ints != 1) return false;

		long texes = incoming.files.keySet().stream()
								   .filter(f -> Util.extension(f).equalsIgnoreCase(Skin.TEXTURE))
								   .count();

		if (texes < 1) return false;

		boolean[] seemsToBeASkin = new boolean[] { false };

		incoming.files.keySet().stream()
					  .filter(f -> Util.extension(f).equalsIgnoreCase(Skin.INT))
					  .map(f -> {
						  try {
							  if (incoming.files.get(f) instanceof Path) {
								  return new IntFile((Path)incoming.files.get(f));
							  } else if (incoming.files.get(f) instanceof Umod.UmodFile) {
								  return new IntFile(((Umod.UmodFile)incoming.files.get(f)).read());
							  }
						  } catch (IOException e) {
							  // TODO add log to this step
						  }
						  return null;
					  })
					  .filter(Objects::nonNull)
					  .forEach(intFile -> {
						  IntFile.Section section = intFile.section("public");
						  if (section == null) return;

						  IntFile.ListValue objects = section.asList("Object");
						  for (IntFile.Value value : objects.values) {
							  if (value instanceof IntFile.MapValue
								  && ((IntFile.MapValue)value).value.containsKey("Name")
								  && ((IntFile.MapValue)value).value.containsKey("Class")
								  && ((IntFile.MapValue)value).value.containsKey("Description")
								  && ((IntFile.MapValue)value).value.get("Class").equalsIgnoreCase("Texture")) {

								  Matcher m = Skin.NAME_MATCH.matcher(((IntFile.MapValue)value).value.get("Name"));
								  if (m.matches()) {
									  seemsToBeASkin[0] = true;
									  return;
								  }
							  }
						  }

					  });

		return seemsToBeASkin[0];
	}
}
