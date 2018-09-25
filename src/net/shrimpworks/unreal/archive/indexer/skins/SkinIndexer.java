package net.shrimpworks.unreal.archive.indexer.skins;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

import net.shrimpworks.unreal.archive.indexer.Content;
import net.shrimpworks.unreal.archive.indexer.Incoming;
import net.shrimpworks.unreal.archive.indexer.IndexLog;
import net.shrimpworks.unreal.archive.indexer.IndexResult;
import net.shrimpworks.unreal.archive.indexer.Indexer;
import net.shrimpworks.unreal.packages.IntFile;
import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;

public class SkinIndexer implements Indexer<Skin> {

	private static final String SHOT_NAME = "%s_shot_%d.png";

	public static class SkinIndexerFactory implements IndexerFactory<Skin> {

		@Override
		public Indexer<Skin> get() {
			return new SkinIndexer();
		}
	}

	@Override
	public void index(Incoming incoming, Content current, IndexLog log, Consumer<IndexResult<Skin>> completed) {
		Skin s = (Skin)current;

		// TODO support UT2004 via .upl files

		Set<IndexResult.CreatedFile> files = new HashSet<>();

		String origName = s.name;

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

		try {
			s.game = game(incoming);
		} catch (Exception e) {
			log.log(IndexLog.EntryType.CONTINUE, "Could not determine game for skin", e);
		}

		try {
			s.author = author(incoming, log);
		} catch (IOException e) {
			log.log(IndexLog.EntryType.CONTINUE, "Failed attempt to read author", e);
		}

		List<BufferedImage> images = images(incoming, log);
		try {
			saveImages(SHOT_NAME, s, images, files);
		} catch (IOException e) {
			log.log(IndexLog.EntryType.CONTINUE, "Failed to save images", e);
		}

		completed.accept(new IndexResult<>(s, files));
	}

	private List<IntFile.MapValue> skinDescriptors(Incoming incoming) {

		return incoming.files(Incoming.FileType.INT).stream()
					   .map(f -> {
						   try {
							   return new IntFile(f.asChannel());
						   } catch (IOException e) {
							   // TODO add log to this step
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

	private String game(Incoming incoming) throws IOException {
		if (incoming.submission.override.get("game", null) != null) return incoming.submission.override.get("game", "Unreal Tournament");

		Set<Incoming.IncomingFile> files = incoming.files(Incoming.FileType.TEXTURE);
		if (files.isEmpty()) return UNKNOWN;

		try (Package pkg = new Package(new PackageReader(files.iterator().next().asChannel()))) {
			if (pkg.version < 68) return "Unreal";
			else if (pkg.version < 117) return "Unreal Tournament";
			else return "Unreal Tournament 2004";
		}
	}

	private List<BufferedImage> images(Incoming incoming, IndexLog log) {
		List<BufferedImage> images = new ArrayList<>();
		try {
			Set<Incoming.IncomingFile> files = incoming.files(Incoming.FileType.IMAGE);
			for (Incoming.IncomingFile img : files) {
				BufferedImage image = ImageIO.read(Channels.newInputStream(Objects.requireNonNull(img.asChannel())));
				if (image != null) images.add(image);
			}
		} catch (Exception e) {
			log.log(IndexLog.EntryType.CONTINUE, "Failed to load image", e);
		}
		return images;
	}

	private String author(Incoming incoming, IndexLog log) throws IOException {
		for (Incoming.IncomingFile f : incoming.files(Incoming.FileType.TEXT, Incoming.FileType.HTML)) {
			List<String> lines;
			try (BufferedReader br = new BufferedReader(Channels.newReader(f.asChannel(), StandardCharsets.UTF_8.name()))) {
				lines = br.lines().collect(Collectors.toList());
			} catch (UncheckedIOException e) {
				log.log(IndexLog.EntryType.INFO, "Could not read file as UTF-8, trying ISO-8859-1", e);
				try (BufferedReader br = new BufferedReader(Channels.newReader(f.asChannel(), StandardCharsets.ISO_8859_1.name()))) {
					lines = br.lines().collect(Collectors.toList());
				} catch (UncheckedIOException ex) {
					log.log(IndexLog.EntryType.CONTINUE, "Failed to search for author", e);
					continue;
				}
			}

			for (String s : lines) {
				Matcher m = Skin.AUTHOR_MATCH.matcher(s);
				if (m.matches() && !m.group(4).trim().isEmpty()) {
					return m.group(4).trim();
				}
			}
		}

		return UNKNOWN;
	}
}
