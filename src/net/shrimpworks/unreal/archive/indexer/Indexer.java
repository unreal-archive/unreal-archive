package net.shrimpworks.unreal.archive.indexer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.imageio.ImageIO;

public interface Indexer<T extends Content> {

	public static final String UNKNOWN = "Unknown";

	public interface IndexerFactory<T extends Content> {

		public Indexer<T> get();
	}

	public void index(Incoming incoming, Content current, IndexLog log, Consumer<IndexResult<T>> completed);

	default void saveImages(String shotTemplate, Content content, List<BufferedImage> screenshots, Set<IndexResult.CreatedFile> files)
			throws IOException {
		for (int i = 0; i < screenshots.size(); i++) {
			String shotName = String.format(shotTemplate, content.name.replaceAll(" ", "_"), i + 1);
			Path out = Paths.get(shotName);
			ImageIO.write(screenshots.get(i), "png", out.toFile());
			content.screenshots.add(out.getFileName().toString());
			files.add(new IndexResult.CreatedFile(shotName, out));
		}
	}

	public static class NoOpIndexerFactory implements IndexerFactory<Content> {

		@Override
		public Indexer<Content> get() {
			return (incoming, current, log, completed) -> completed.accept(new IndexResult<>(current, Collections.emptySet()));
		}
	}
}
