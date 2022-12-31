package net.shrimpworks.unreal.archive.content.models;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.Games;
import net.shrimpworks.unreal.archive.content.Incoming;
import net.shrimpworks.unreal.archive.content.IndexHandler;
import net.shrimpworks.unreal.archive.content.IndexLog;
import net.shrimpworks.unreal.archive.content.IndexResult;
import net.shrimpworks.unreal.archive.content.IndexUtils;
import net.shrimpworks.unreal.archive.content.skins.Skin;
import net.shrimpworks.unreal.archive.content.skins.SkinIndexHandler;
import net.shrimpworks.unreal.packages.IntFile;
import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.ExportedObject;
import net.shrimpworks.unreal.packages.entities.Name;
import net.shrimpworks.unreal.packages.entities.objects.Object;
import net.shrimpworks.unreal.packages.entities.objects.Texture2D;
import net.shrimpworks.unreal.packages.entities.properties.ArrayProperty;
import net.shrimpworks.unreal.packages.entities.properties.ObjectProperty;

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

		m.models = new ArrayList<>();
		m.skins = new ArrayList<>();

		m.game = IndexUtils.game(incoming).name;

		if (!incoming.files(Incoming.FileType.PACKAGE).isEmpty()) {
			// UT3 content includes .UPK files

			// they don't seem to have skins, just characters, so that's all we'll record
			characterDescriptors(incoming)
				.forEach(v -> {
					String maybeFaction = v.getOrDefault("Faction", "");
					if (!maybeFaction.isBlank()) {
						m.models.add(maybeFaction + ": " + v.getOrDefault("CharName", "Unknown").trim());
					} else {
						m.models.add(v.getOrDefault("CharName", "Unknown").trim());
					}

					if (m.name == null || m.name.equals(origName)) m.name = v.getOrDefault("CharName", "Unknown").trim();
				});

		} else if (!incoming.files(Incoming.FileType.PLAYER).isEmpty()) {
			// UT2003/4 model is defined by a UPL file

			// each record contains both mesh and skin information, so keep track of seen meshes vs skins
			Set<String> meshes = new HashSet<>();

			SkinIndexHandler.playerDescriptors(incoming).forEach(p -> {
				if (p.containsKey("DefaultName")) {
					if (p.containsKey("Mesh") && !meshes.contains(p.get("Mesh").trim())) {
						m.models.add(p.get("DefaultName").trim());
						meshes.add(p.get("Mesh").trim());
					}

					if (m.name == null || m.name.equals(origName)) m.name = p.get("DefaultName");
					m.skins.add(p.get("DefaultName").trim());
				}
			});
		} else {
			// find model and skin information via .int files
			modelDescriptors(incoming).forEach(d -> {
				if (d.containsKey("Description") && Model.NAME_MATCH.matcher(d.get("Name")).matches() &&
					!d.get("Description").trim().isBlank()) {
					if (m.name == null || m.name.equals(origName)) m.name = d.get("Description");
					m.models.add(d.get("Description").trim());

					if (d.get("MetaClass").equalsIgnoreCase(Model.RUNE_PLAYER_CLASS)) {
						m.game = Games.RUNE.name;
					}
				}
			});

			SkinIndexHandler.skinDescriptors(incoming).forEach(d -> {
				if (d.containsKey("Description") && Skin.NAME_MATCH.matcher(d.get("Name")).matches()) {
					if (m.name == null) m.name = d.get("Description");
					m.skins.add(d.get("Description").trim());
				}
			});
		}

		m.author = IndexUtils.findAuthor(incoming);

		try {
			// see if there are any images the author may have included in the package
			List<BufferedImage> images = IndexUtils.findImageFiles(incoming);

			// also see if we can at least include chat portrait images
			SkinIndexHandler.findPortraits(incoming, images);

			// try to find UT3 preview images
			findUt3Previews(incoming, images);

			IndexUtils.saveImages(IndexUtils.SHOT_NAME, m, images, attachments);
		} catch (Throwable e) {
			log.log(IndexLog.EntryType.CONTINUE, "Failed to save images", e);
		}

		completed.accept(new IndexResult<>(m, attachments));
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
								 if (!(value instanceof IntFile.MapValue)) continue;
								 IntFile.MapValue mapVal = (IntFile.MapValue)value;
								 if (mapVal.containsKey("Name")
									 && mapVal.containsKey("MetaClass")
									 && mapVal.containsKey("Description")
									 && (mapVal.get("MetaClass").equalsIgnoreCase(Model.UT_PLAYER_CLASS)
										 || (mapVal.get("MetaClass").equalsIgnoreCase(Model.RUNE_PLAYER_CLASS)))
								 ) {
									 vals.add(mapVal);
								 }
							 }

							 return vals.stream();
						 })
						 .filter(Objects::nonNull)
						 .toList();
	}

	private List<IntFile.MapValue> characterDescriptors(Incoming incoming) {
		return IndexUtils.readIntFiles(incoming, incoming.files(Incoming.FileType.INI))
						 .filter(Objects::nonNull)
						 .flatMap(intFile -> {
							 IntFile.Section section = intFile.section(Model.UT3_CHARACTER_DEF);
							 if (section == null) return Stream.empty();

							 return section.asList("+Characters")
								 .values
								 .stream()
								 .filter(v -> v instanceof IntFile.MapValue)
								 .map(v -> (IntFile.MapValue)v)
								 .filter(v -> v.containsKey("CharName"));

						 })
						 .filter(Objects::nonNull)
						 .toList();

	}

	private void findUt3Previews(Incoming incoming, List<BufferedImage> images) {
		characterDescriptors(incoming)
			.stream()
			.filter(c -> c.containsKey("PreviewImageMarkup"))
			.forEach(c -> {
				Matcher matcher = IndexUtils.UT3_SCREENSHOT_MATCH.matcher(c.get("PreviewImageMarkup"));
				if (matcher.find()) {
					String pkgName = matcher.group(1);
					incoming.files(Incoming.FileType.PACKAGE).stream().filter(
						f -> Util.plainName(f.fileName()).equalsIgnoreCase(pkgName)).findFirst().ifPresent(f -> {
						try (Package pkg = new Package(new PackageReader(f.asChannel()))) {

							ExportedObject export = pkg.objectByName(new Name(matcher.group(3)));
							if (export == null) return;

							Object object = export.object();

							if (object instanceof Texture2D) images.add(IndexUtils.screenshotFromObject(pkg, object));

							// UT3 maps may use a Material to hold multiple screenshots
							if (object.className().equals("Material") && object.property("ReferencedTextures") instanceof ArrayProperty) {
								((ArrayProperty)object.property("ReferencedTextures")).values.forEach(t -> {
									if (t instanceof ObjectProperty) {
										Object tex = pkg.objectByRef(((ObjectProperty)t).value).object();
										if (tex instanceof Texture2D) {
											images.add(IndexUtils.screenshotFromObject(pkg, tex));
										}
									}
								});
							}
						} catch (Throwable e) {
							incoming.log.log(IndexLog.EntryType.CONTINUE, "Failed to extract UE3 images", e);
						}
					});

				}
			});
	}

}
