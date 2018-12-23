package net.shrimpworks.unreal.archive.indexer.models;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.shrimpworks.unreal.archive.YAML;
import net.shrimpworks.unreal.archive.indexer.Content;
import net.shrimpworks.unreal.archive.indexer.Incoming;
import net.shrimpworks.unreal.archive.indexer.IndexHandler;
import net.shrimpworks.unreal.archive.indexer.IndexLog;
import net.shrimpworks.unreal.archive.indexer.IndexResult;
import net.shrimpworks.unreal.archive.indexer.IndexUtils;
import net.shrimpworks.unreal.archive.indexer.skins.SkinIndexHandler;
import net.shrimpworks.unreal.packages.IntFile;
import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;

public class ModelIndexHandler implements IndexHandler<Model> {

	public static class ModelIndexHandlerFactory implements IndexHandlerFactory<Model> {

		@Override
		public IndexHandler<Model> get() {
			return new ModelIndexHandler();
		}
	}

	@Override
	public void index(Incoming incoming, Content current, Consumer<IndexResult<Model>> completed) {
		Model m = (Model)current;
		IndexLog log = incoming.log;

		Set<IndexResult.NewAttachment> attachments = new HashSet<>();

		String origName = m.name;

		// UT2003/4 model is defined by a UPL file
		if (!incoming.files(Incoming.FileType.PLAYER).isEmpty()) {
			// each record contains both mesh and skin information, so keep track of seen meshes vs skins
			Set<String> meshes = new HashSet<>();

			SkinIndexHandler.playerDescriptors(incoming).forEach(p -> {
				if (p.value.containsKey("DefaultName")) {
					if (p.value.containsKey("Mesh") && !meshes.contains(p.value.get("Mesh").trim())) {
						m.models.add(p.value.get("DefaultName").trim());
						meshes.add(p.value.get("Mesh").trim());
					}

					if (m.name == null || m.name.equals(origName)) m.name = p.value.get("DefaultName");
					m.skins.add(p.value.get("DefaultName").trim());
				}
			});
		} else {
			// find model and skin information via .int files
			modelDescriptors(incoming).forEach(d -> {
				if (d.value.containsKey("Description") && Model.NAME_MATCH.matcher(d.value.get("Name")).matches()) {
					if (m.name == null || m.name.equals(origName)) m.name = d.value.get("Description");
					m.models.add(d.value.get("Description").trim());
				}
			});

			SkinIndexHandler.skinDescriptors(incoming).forEach(d -> {
				if (d.value.containsKey("Description") && Model.NAME_MATCH.matcher(d.value.get("Name")).matches()) {
					if (m.name == null || m.name.equals(origName)) m.name = d.value.get("Description");
					m.skins.add(d.value.get("Description").trim());
				}
			});
		}

		try {
			if (m.releaseDate != null && m.releaseDate.compareTo(IndexUtils.RELEASE_UT99) < 0) m.game = "Unreal";
			m.game = game(incoming);
		} catch (Exception e) {
			log.log(IndexLog.EntryType.CONTINUE, "Could not determine game for model", e);
		}

		try {
			m.author = IndexUtils.findAuthor(incoming);
		} catch (IOException e) {
			log.log(IndexLog.EntryType.CONTINUE, "Failed attempt to read author", e);
		}

		try {
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

//		completed.accept(new IndexResult<>(m, attachments));
	}

	public static List<IntFile.MapValue> modelDescriptors(Incoming incoming) {
		return IndexUtils.readIntFiles(incoming, incoming.files(Incoming.FileType.INT))
						 .filter(Objects::nonNull)
						 .flatMap(intFile -> {
							 List<IntFile.MapValue> vals = new ArrayList<>();

							 IntFile.Section section = intFile.section("public");
							 if (section == null) return Stream.empty();

							 IntFile.ListValue objects = section.asList("Object");
							 for (IntFile.Value value : objects.values) {
								 if (value instanceof IntFile.MapValue
									 && ((IntFile.MapValue)value).value.containsKey("Name")
									 && ((IntFile.MapValue)value).value.containsKey("MetaClass")
									 && ((IntFile.MapValue)value).value.containsKey("Description")
									 && ((IntFile.MapValue)value).value.get("MetaClass").equalsIgnoreCase(Model.UT_PLAYER_CLASS)) {

									 vals.add((IntFile.MapValue)value);
								 }
							 }

							 return vals.stream();
						 })
						 .filter(Objects::nonNull)
						 .collect(Collectors.toList());
	}

	private String game(Incoming incoming) throws IOException {
		if (incoming.submission.override.get("game", null) != null) return incoming.submission.override.get("game", "Unreal Tournament");

		if (!incoming.files(Incoming.FileType.PLAYER).isEmpty()) return "Unreal Tournament 2004";

		Set<Incoming.IncomingFile> files = incoming.files(Incoming.FileType.TEXTURE);
		if (files.isEmpty()) return IndexUtils.UNKNOWN;

		try (Package pkg = new Package(new PackageReader(files.iterator().next().asChannel()))) {
			if (pkg.version < 68) return "Unreal";
			else if (pkg.version < 117) return "Unreal Tournament";
			else return "Unreal Tournament 2004";
		}
	}

}
