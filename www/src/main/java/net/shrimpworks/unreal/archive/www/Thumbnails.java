package net.shrimpworks.unreal.archive.www;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

import net.shrimpworks.unreal.archive.common.Util;

public class Thumbnails {

	public static Path thumbnail(Path source, Path dest, int maxWidth) throws IOException {
		if (Files.exists(dest)) return dest;

		BufferedImage image = ImageIO.read(source.toFile());
		double scale = (double)maxWidth / image.getWidth();
		BufferedImage thumb = new BufferedImage((int)(image.getWidth() * scale),
												(int)(image.getHeight() * scale),
												BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = thumb.createGraphics();
		graphics.drawImage(image.getScaledInstance(thumb.getWidth(), thumb.getHeight(), Image.SCALE_SMOOTH), 0, 0, null);

		ImageIO.write(thumb, Util.extension(source), dest.toFile());

		// improve caching behaviour - set modified time to source file time
		Files.setLastModifiedTime(dest, Files.getLastModifiedTime(source));

		return dest;
	}

	public static Path thumbnail(Path source, Path outDirectory, ThumbConfig conf) throws IOException {
		// the source is already a thumbnail
		if (!Util.fileName(source).startsWith(String.format("%s_", conf.name))) return source;

		// create the destination path using the requested filename
		final Path dest = outDirectory.resolve(String.format("%s_%s", conf.name, Util.fileName(source)));

		return thumbnail(source, dest, conf.maxWidth);
	}

	public static class ThumbConfig {

		public final Path path;
		public String name;
		public int maxWidth;
		public int maxHeight;
		public boolean noSubDirectories;

		public ThumbConfig(Path path, String config) {
			this.path = path;

			for (String param : config.split(";")) {
				if (param.matches("[a-zA-Z]+?=\\d+x\\d+")) {
					String[] nameSize = param.split("=");
					String[] widthHeight = nameSize[1].split("x");
					this.name = nameSize[0];
					this.maxWidth = Integer.parseInt(widthHeight[0]);
					this.maxHeight = Integer.parseInt(widthHeight[1]);
				} else if (param.equalsIgnoreCase("nowalk")) {
					this.noSubDirectories = true;
				}
			}
		}
	}

}
