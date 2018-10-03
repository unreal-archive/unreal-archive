package net.shrimpworks.unreal.archive.indexer.skins;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.shrimpworks.unreal.archive.indexer.Content;
import net.shrimpworks.unreal.archive.indexer.Incoming;
import net.shrimpworks.unreal.archive.indexer.IndexHandler;
import net.shrimpworks.unreal.archive.indexer.IndexLog;
import net.shrimpworks.unreal.archive.indexer.IndexResult;
import net.shrimpworks.unreal.archive.indexer.IndexUtils;
import net.shrimpworks.unreal.packages.IntFile;
import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;

public class SkinIndexHandler implements IndexHandler<Skin> {

	private static final String SHOT_NAME = "%s_shot_%d.png";

	public static class SkinIndexHandlerFactory implements IndexHandlerFactory<Skin> {

		@Override
		public IndexHandler<Skin> get() {
			return new SkinIndexHandler();
		}
	}

	@Override
	public void index(Incoming incoming, Content current, Consumer<IndexResult<Skin>> completed) {
		Skin s = (Skin)current;
		IndexLog log = incoming.log;

		// TODO support UT2004 via .upl files

		Set<IndexResult.NewAttachment> attachments = new HashSet<>();

		String origName = s.name;

		if (!incoming.files(Incoming.FileType.PLAYER).isEmpty()) {
			playerDescriptors(incoming).forEach(p -> {
				if (p.value.containsKey("DefaultName")) {
					if (s.name == null || s.name.equals(origName)) s.name = p.value.get("DefaultName");
					s.skins.add(p.value.get("DefaultName").trim());
				}
			});
		} else {
			// find skin via .int files
			skinDescriptors(incoming).forEach(d -> {
				if (d.value.containsKey("Description") && Skin.NAME_MATCH.matcher(d.value.get("Name")).matches()) {
					if (s.name == null || s.name.equals(origName)) s.name = d.value.get("Description");
					s.skins.add(d.value.get("Description").trim());
				} else if (Skin.TEAM_MATCH.matcher(d.value.get("Name")).matches()) {
					s.teamSkins = true;
				} else if (d.value.containsKey("Description") && Skin.FACE_MATCH.matcher(d.value.get("Name")).matches()) {
					s.faces.add(d.value.get("Description"));
				}
			});
		}

		try {
			if (s.releaseDate != null && s.releaseDate.compareTo(RELEASE_UT99) < 0) s.game = "Unreal";
			s.game = game(incoming);
		} catch (Exception e) {
			log.log(IndexLog.EntryType.CONTINUE, "Could not determine game for skin", e);
		}

		try {
			s.author = author(incoming);
		} catch (IOException e) {
			log.log(IndexLog.EntryType.CONTINUE, "Failed attempt to read author", e);
		}

		try {
			List<BufferedImage> images = IndexUtils.findImageFiles(incoming);
			IndexUtils.saveImages(SHOT_NAME, s, images, attachments);
		} catch (IOException e) {
			log.log(IndexLog.EntryType.CONTINUE, "Failed to save images", e);
		}

		completed.accept(new IndexResult<>(s, attachments));
	}

	private List<IntFile.MapValue> skinDescriptors(Incoming incoming) {
		return incoming.files(Incoming.FileType.INT).stream()
					   .map(f -> {
						   try {
							   return new IntFile(f.asChannel());
						   } catch (IOException e) {
							   incoming.log.log(IndexLog.EntryType.CONTINUE, "Couldn't load INT file " + f.fileName(), e);
							   return null;
						   }
					   })
					   .filter(Objects::nonNull)
					   .flatMap(intFile -> {
						   List<IntFile.MapValue> vals = new ArrayList<>();

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

						   return vals.stream();
					   })
					   .filter(Objects::nonNull)
					   .collect(Collectors.toList());
	}

	private List<IntFile.MapValue> playerDescriptors(Incoming incoming) {
		return incoming.files(Incoming.FileType.PLAYER).stream()
					   .map(f -> {
						   try {
							   return new IntFile(f.asChannel());
						   } catch (IOException e) {
							   incoming.log.log(IndexLog.EntryType.CONTINUE, "Couldn't load UPL file " + f.fileName(), e);
							   return null;
						   }
					   })
					   .filter(Objects::nonNull)
					   .flatMap(intFile -> {
						   List<IntFile.MapValue> vals = new ArrayList<>();

						   IntFile.Section section = intFile.section("public");
						   if (section == null) return Stream.empty();

						   IntFile.ListValue objects = section.asList("Player");
						   for (IntFile.Value value : objects.values) {
							   if (value instanceof IntFile.MapValue
								   && ((IntFile.MapValue)value).value.containsKey("DefaultName")) {

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
		if (files.isEmpty()) return UNKNOWN;

		try (Package pkg = new Package(new PackageReader(files.iterator().next().asChannel()))) {
			if (pkg.version < 68) return "Unreal";
			else if (pkg.version < 117) return "Unreal Tournament";
			else return "Unreal Tournament 2004";
		}
	}

	private String author(Incoming incoming) throws IOException {
		List<String> lines = IndexUtils.textContent(incoming);

		for (String s : lines) {
			Matcher m = Skin.AUTHOR_MATCH.matcher(s);
			if (m.matches() && !m.group(4).trim().isEmpty()) {
				return m.group(4).trim();
			}
		}

		return UNKNOWN;
	}

}
