package net.shrimpworks.unreal.archive.indexer.skins;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.indexer.Content;
import net.shrimpworks.unreal.archive.indexer.ContentIndexer;
import net.shrimpworks.unreal.archive.indexer.Incoming;
import net.shrimpworks.unreal.archive.indexer.IndexLog;
import net.shrimpworks.unreal.archive.indexer.IndexResult;
import net.shrimpworks.unreal.packages.IntFile;
import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.Umod;

public class SkinIndexer implements ContentIndexer<Skin> {

	private static final String SHOT_NAME = "%s_shot_%d.png";

	public static class SkinIndexerFactory implements IndexerFactory<Skin> {

		@Override
		public ContentIndexer<Skin> get() {
			return new SkinIndexer();
		}
	}

	@Override
	public void index(Incoming incoming, Content current, IndexLog log, Consumer<IndexResult<Skin>> completed) {
		Skin s = (Skin)current;

		Set<IndexResult.CreatedFile> files = new HashSet<>();

		String origName = s.name;

		skinDescriptors(incoming).forEach(d -> {

			if (d.value.containsKey("Description") && Skin.NAME_MATCH.matcher(d.value.get("Name")).matches()) {
				if (s.name == null || s.name.equals(origName)) s.name = d.value.get("Description");
				s.skins.add(d.value.get("Description"));
			} else if (Skin.TEAM_MATCH.matcher(d.value.get("Name")).matches()) {
				s.teamSkins = true;
			} else if (d.value.containsKey("Description") && Skin.FACE_MATCH.matcher(d.value.get("Name")).matches()) {
				s.faces.add(d.value.get("Description"));
			}
		});

		try {
			s.game = game(incoming);
		} catch (Exception e) {
			log.log(IndexLog.EntryType.CONTINUE, "Could not determine game for skin", e);
		}

		List<BufferedImage> images = images(incoming, log);
		for (int i = 0; i < images.size(); i++) {
			try {
				String shotName = String.format(SHOT_NAME, s.name.replaceAll(" ", "_"), i + 1);
				Path out = Paths.get(shotName);
				ImageIO.write(images.get(i), "png", out.toFile());
				s.screenshots.add(out.getFileName().toString());
				files.add(new IndexResult.CreatedFile(shotName, out));
			} catch (Exception e) {
				log.log(IndexLog.EntryType.CONTINUE, "Error while processing an image", e);
			}
		}

		completed.accept(new IndexResult<>(s, files));
	}

	private List<IntFile.MapValue> skinDescriptors(Incoming incoming) {

		return incoming.files.keySet().stream()
							 .filter(f -> Util.extension(f).equalsIgnoreCase(Skin.INT))
							 .map(f -> (Path)incoming.files.get(f))
							 .flatMap(intPath -> {
								 List<IntFile.MapValue> vals = new ArrayList<>();
								 try {
									 IntFile intFile = new IntFile(intPath);
									 IntFile.Section section = intFile.section("public");
									 if (section == null) return Stream.empty();

									 IntFile.ListValue objects = section.asList("Object");
									 for (IntFile.Value value : objects.values) {
										 if (value instanceof IntFile.MapValue
											 && ((IntFile.MapValue)value).value.containsKey("Name")
											 && ((IntFile.MapValue)value).value.containsKey("Class")
											 && ((IntFile.MapValue)value).value.get("Class").equalsIgnoreCase("Texture")) {

											 vals.add((IntFile.MapValue)value);
										 }
									 }

								 } catch (IOException e) {
									 // TODO add log to this step
								 }

								 return vals.stream();
							 })
							 .filter(Objects::nonNull)
							 .collect(Collectors.toList());
	}

	private String game(Incoming incoming) throws IOException {
		Package pkg = null;
		for (java.util.Map.Entry<String, java.lang.Object> kv : incoming.files.entrySet()) {
			if (Util.extension(kv.getKey()).equalsIgnoreCase(Skin.TEXTURE)) {
				if (kv.getValue() instanceof Path) {
					pkg = new Package((Path)kv.getValue());
				} else if (kv.getValue() instanceof Umod.UmodFile) {
					pkg = new Package(new PackageReader(((Umod.UmodFile)kv.getValue()).read()));
				}
			}
		}

		if (pkg != null) {
			if (pkg.version <= 68) return "Unreal";
			else if (pkg.version < 117) return "Unreal Tournament";
			else return "Unreal Tournament 2004";
		}

		return "Unknown";
	}

	private List<BufferedImage> images(Incoming incoming, IndexLog log) {
		List<BufferedImage> images = new ArrayList<>();
		try {
			for (java.util.Map.Entry<String, java.lang.Object> entry : incoming.files.entrySet()) {
				// only do this for paths, skip umod contents for now
				if (entry.getValue() instanceof Path
					&& (Util.extension(entry.getKey()).toLowerCase().startsWith("jp")
						|| (Util.extension(entry.getKey()).toLowerCase().startsWith("bmp"))
						|| (Util.extension(entry.getKey()).toLowerCase().startsWith("png")))) {
					BufferedImage image = ImageIO.read(((Path)entry.getValue()).toFile());
					if (image != null) images.add(image);
				}
			}
		} catch (Exception e) {
			log.log(IndexLog.EntryType.CONTINUE, "Failed to load image from archive", e);
		}
		return images;
	}
}
