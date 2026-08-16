package org.unrealarchive.www;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

import org.unrealarchive.common.Util;

public class Thumbnails {

	public static Path thumbnail(Path source, Path dest, int maxWidth) throws IOException {
		if (Files.exists(dest)) return dest;

		BufferedImage image = ImageIO.read(source.toFile());

		// don't bother upscaling small images
		if (image.getWidth() <= maxWidth) {
			Files.copy(source, dest);
			return dest;
		}

		double scale = (double)maxWidth / image.getWidth();
		int w = (int)(image.getWidth() * scale), h = (int)(image.getHeight() * scale);

		BufferedImage cur = image;

		// halve until within 2x of target, then one final interpolated step - SCALE_SMOOTH quality but ~30x faster
		for (int cw = cur.getWidth(), ch = cur.getHeight(); cw / 2 > w; ) {
			cw = Math.max(w, cw / 2);
			ch = Math.max(h, ch / 2);
			cur = draw(cur, cw, ch);
		}

		ImageIO.write(draw(cur, w, h), Util.extension(source), dest.toFile());

		// improve caching behaviour - set modified time to source file time
		Files.setLastModifiedTime(dest, Files.getLastModifiedTime(source));

		return dest;
	}

	public static Path thumbnail(Path source, Path outDirectory, ThumbConfig conf) throws IOException {
		// create the destination path using the requested filename
		final Path dest = outDirectory.resolve(String.format("%s_%s", conf.name, Util.fileName(source)));

		return thumbnail(source, dest, conf.maxWidth);
	}

	private static BufferedImage draw(BufferedImage src, int w, int h) {
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.drawImage(src, 0, 0, w, h, null);
		g.dispose();          // current code never disposes
		return out;
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
