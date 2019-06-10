package net.shrimpworks.unreal.archive.www;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TemplatesTest {

	@Test
	public void staticResources() throws IOException {
		Path tempDirectory = Files.createTempDirectory("www-static");
		Templates.unpackResources("static.list", tempDirectory);
		boolean[] foundCss = { false };
		Files.walkFileTree(tempDirectory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (file.getFileName().toString().equals("style.css") && file.getParent().getFileName().toString().equals("css")) {
					foundCss[0] = true;
				}
				return super.visitFile(file, attrs);
			}
		});

		assertTrue(foundCss[0]);
	}

}
