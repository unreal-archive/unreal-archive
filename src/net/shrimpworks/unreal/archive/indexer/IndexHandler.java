package net.shrimpworks.unreal.archive.indexer;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;

public interface IndexHandler<T extends Content> {

	static final String UNKNOWN = "Unknown";

	static final String RELEASE_UT99 = "1999-11";

	public interface IndexHandlerFactory<T extends Content> {

		public IndexHandler<T> get();
	}

	public void index(Incoming incoming, Content current, Consumer<IndexResult<T>> completed);

	static void saveImages(
			String shotTemplate, Content content, List<BufferedImage> screenshots, Set<IndexResult.NewAttachment> attachments)
			throws IOException {
		for (int i = 0; i < screenshots.size(); i++) {
			String shotName = String.format(shotTemplate, content.name.replaceAll(" ", "_"), i + 1);
			Path out = Paths.get(shotName);
			ImageIO.write(screenshots.get(i), "png", out.toFile());
			attachments.add(new IndexResult.NewAttachment(Content.AttachmentType.IMAGE, shotName, out));
		}
	}

	static Package findPackage(Incoming incoming, String pkg) {
		Set<Incoming.IncomingFile> files = incoming.files(Incoming.FileType.IMPORTANT);
		for (Incoming.IncomingFile f : files) {
			String name = f.fileName();
			name = name.substring(0, name.lastIndexOf("."));
			if (name.equalsIgnoreCase(pkg)) {
				return new Package(new PackageReader(f.asChannel()));
			}
		}
		throw new IllegalStateException("Failed to find package " + pkg);
	}

	static List<BufferedImage> findImageFiles(Incoming incoming) {
		List<BufferedImage> images = new ArrayList<>();
		try {
			Set<Incoming.IncomingFile> files = incoming.files(Incoming.FileType.IMAGE);
			for (Incoming.IncomingFile img : files) {
				BufferedImage image = ImageIO.read(Channels.newInputStream(Objects.requireNonNull(img.asChannel())));
				if (image != null) images.add(image);
			}
		} catch (Exception e) {
			incoming.log.log(IndexLog.EntryType.CONTINUE, "Failed to load screenshot from archive", e);
		}

		return images;
	}

	default List<String> textContent(Incoming incoming) throws IOException {
		List<String> lines = new ArrayList<>();
		for (Incoming.IncomingFile f : incoming.files(Incoming.FileType.TEXT, Incoming.FileType.HTML)) {
			try (BufferedReader br = new BufferedReader(Channels.newReader(f.asChannel(), StandardCharsets.UTF_8.name()))) {
				lines.addAll(br.lines().collect(Collectors.toList()));
			} catch (UncheckedIOException e) {
				incoming.log.log(IndexLog.EntryType.INFO, "Could not read file as UTF-8, trying ISO-8859-1", e);
				try (BufferedReader br = new BufferedReader(Channels.newReader(f.asChannel(), StandardCharsets.ISO_8859_1.name()))) {
					lines.addAll(br.lines().collect(Collectors.toList()));
				} catch (UncheckedIOException ex) {
					incoming.log.log(IndexLog.EntryType.CONTINUE, "Failed to load text content from incoming package", e);
				}
			}
		}

		return lines;
	}

	public static class NoOpIndexHandlerFactory implements IndexHandlerFactory<Content> {

		@Override
		public IndexHandler<Content> get() {
			return (incoming, current, completed) -> completed.accept(new IndexResult<>(current, Collections.emptySet()));
		}
	}
}
