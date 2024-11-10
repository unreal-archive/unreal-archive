package org.unrealarchive.indexing.skins;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import net.shrimpworks.unreal.packages.IntFile;
import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.entities.ExportedObject;
import net.shrimpworks.unreal.packages.entities.Name;
import net.shrimpworks.unreal.packages.entities.objects.Object;
import net.shrimpworks.unreal.packages.entities.objects.Texture;

import org.unrealarchive.content.FileType;
import org.unrealarchive.content.addons.Addon;
import org.unrealarchive.content.addons.Skin;
import org.unrealarchive.indexing.Incoming;
import org.unrealarchive.indexing.IndexHandler;
import org.unrealarchive.indexing.IndexLog;
import org.unrealarchive.indexing.IndexResult;
import org.unrealarchive.indexing.IndexUtils;

public class SkinIndexHandler implements IndexHandler<Skin> {

	public static class SkinIndexHandlerFactory implements IndexHandlerFactory<Skin> {

		@Override
		public IndexHandler<Skin> get() {
			return new SkinIndexHandler();
		}
	}

	@Override
	public void index(Incoming incoming, Addon current, Consumer<IndexResult<Skin>> completed) {
		Skin s = (Skin)current;
		IndexLog log = incoming.log;

		Set<IndexResult.NewAttachment> attachments = new HashSet<>();

		s.skins = new ArrayList<>();
		s.faces = new ArrayList<>();

		String origName = s.name;

		s.game = IndexUtils.game(incoming).name;

		if (!incoming.files(FileType.PLAYER).isEmpty()) {
			playerDescriptors(incoming).forEach(p -> {
				if (p.containsKey("DefaultName")) {
					if (s.name == null || s.name.equals(origName)) s.name = p.get("DefaultName");
					s.skins.add(p.get("DefaultName").trim());
				}
			});
		} else {
			// find skin via .int files
			skinDescriptors(incoming).forEach(d -> {
				if (s.game.equals("Unreal")) {
					Matcher nameMatch = SkinClassifier.NAME_MATCH_UNREAL.matcher(d.get("Name"));
					if (nameMatch.matches()) {
						if (s.name == null || s.name.equals(origName)) s.name = nameMatch.group(1).trim();
						s.skins.add(nameMatch.group(1).trim());
					}
				} else {
					if (d.containsKey("Description") && SkinClassifier.NAME_MATCH.matcher(d.get("Name")).matches() &&
						!d.get("Description").trim().isBlank()) {
						if (s.name == null || s.name.equals(origName)) s.name = d.get("Description");
						s.skins.add(d.get("Description").trim());
					} else if (!s.teamSkins && SkinClassifier.TEAM_MATCH.matcher(d.get("Name")).matches()) {
						s.teamSkins = true;
					} else if (d.containsKey("Description") && SkinClassifier.FACE_MATCH.matcher(d.get("Name")).matches()) {
						s.faces.add(d.get("Description"));
					}
				}
			});
		}

		s.skins = s.skins.stream().distinct().toList();
		s.faces = s.faces.stream().distinct().toList();

		s.author = IndexUtils.findAuthor(incoming);

		try {
			// see if there are any images the author may have included in the package
			List<BufferedImage> images = IndexUtils.findImageFiles(incoming);

			// also see if we can at least include chat portrait images
			findPortraits(incoming, images);

			IndexUtils.saveImages(IndexUtils.SHOT_NAME, s, images, attachments);
		} catch (IOException e) {
			log.log(IndexLog.EntryType.CONTINUE, "Failed to save images", e);
		}

		completed.accept(new IndexResult<>(s, attachments));
	}

	public static void findPortraits(Incoming incoming, List<BufferedImage> images) {
		if (!incoming.files(FileType.PLAYER).isEmpty()) {
			// find from UT2003/4 UPL "Portrait" property
			playerDescriptors(incoming).forEach(d -> {
				if (d.containsKey("Portrait")) {
					try {
						String[] pkgTex = d.get("Portrait").split("\\.");
						Package pkg = IndexUtils.findPackage(incoming, pkgTex[0]);

						ExportedObject e = pkg.objectByName(new Name(pkgTex[1], 0));
						Object o = e.object();
						if (o instanceof Texture) {
							Texture.MipMap[] mipMaps = ((Texture)o).mipMaps();
							images.add(mipMaps[0].get());
						}
					} catch (Exception e) {
						incoming.log.log(IndexLog.EntryType.CONTINUE, "Finding portrait failed", e);
					}
				}
			});
		} else {
			// look up portraits from skin descriptors
			skinDescriptors(incoming).forEach(d -> {
				Matcher faceMatch = SkinClassifier.FACE_PORTRAIT_MATCH.matcher(d.get("Name"));
				if (faceMatch.matches()) {
					try {
						String pkgName = faceMatch.group(1);
						String texName = faceMatch.group(2);
						Package pkg = IndexUtils.findPackage(incoming, pkgName);

						ExportedObject e = pkg.objectByName(new Name(texName, 0));
						Object o = e.object();
						if (o instanceof Texture) {
							Texture.MipMap[] mipMaps = ((Texture)o).mipMaps();
							images.add(mipMaps[0].get());
						}
					} catch (Exception e) {
						incoming.log.log(IndexLog.EntryType.CONTINUE, "Finding portrait failed", e);
					}
				}
			});
		}
	}

	public static List<IntFile.MapValue> skinDescriptors(Incoming incoming) {
		return IndexUtils.readIntFiles(incoming, incoming.files(FileType.INT))
						 .filter(Objects::nonNull)
						 .flatMap(intFile -> {
							 List<IntFile.MapValue> vals = new ArrayList<>();

							 IntFile.Section section = intFile.section("public");
							 if (section == null) return Stream.empty();

							 IntFile.ListValue objects = section.asList("Object");
							 for (IntFile.Value value : objects.values()) {
								 if (value instanceof IntFile.MapValue
									 && ((IntFile.MapValue)value).containsKey("Name")
									 && ((IntFile.MapValue)value).containsKey("Class")
									 && ((IntFile.MapValue)value).get("Class").equalsIgnoreCase("Texture")) {

									 vals.add((IntFile.MapValue)value);
								 }
							 }

							 return vals.stream();
						 })
						 .filter(Objects::nonNull)
						 .toList();
	}

	public static List<IntFile.MapValue> playerDescriptors(Incoming incoming) {
		return IndexUtils.readIntFiles(incoming, incoming.files(FileType.PLAYER))
						 .filter(Objects::nonNull)
						 .flatMap(intFile -> {
							 List<IntFile.MapValue> vals = new ArrayList<>();

							 IntFile.Section section = intFile.section("public");
							 if (section == null) return Stream.empty();

							 IntFile.ListValue objects = section.asList("Player");
							 for (IntFile.Value value : objects.values()) {
								 if (value instanceof IntFile.MapValue
									 && ((IntFile.MapValue)value).containsKey("DefaultName")) {

									 vals.add((IntFile.MapValue)value);
								 }
							 }

							 return vals.stream();
						 })
						 .filter(Objects::nonNull)
						 .toList();
	}

}
