package org.unrealarchive.www;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.unrealarchive.content.Download;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

	@Test
	public void urlEncoding() throws IOException {
		final String url = "https://unreal-archive-files-eu.s3.de.io.cloud.ovh.net/Unreal%20Tournament/Maps/Monster%20Hunt/"
						   + "G/a/f/a4fc7c/MH-GiranTown+SBFix2.7z";

		Path tempDirectory = Files.createTempDirectory("www-encoding");
		Templates.template("test-encoding.ftl", SiteMap.Page.monthly(0))
				 .put("staticRoot", tempDirectory)
				 .put("mirrors", List.of(new Download(url, true, Download.DownloadState.OK)))
				 .put("title", "MH-GiranTown+SBFix2 & friends")
				 .write(tempDirectory.resolve("encoding.html"));
		String html = Files.readString(tempDirectory.resolve("encoding.html"));

		// a '+' within a download path must be encoded, since S3 hosts read it as a space
		assertTrue(html.contains("MH-GiranTown%2BSBFix2.7z"), html);
		assertFalse(html.contains("MH-GiranTown+SBFix2.7z"), html);

		// issue report links encode their query values, while retaining the query's own separators
		assertTrue(html.contains("issues/new?title=MH-GiranTown%2BSBFix2+%26+friends&amp;labels=&amp;body="), html);

		// the report script locates the body's '---' separator to inject the page URL
		assertTrue(html.contains("---"), html);
	}
}
