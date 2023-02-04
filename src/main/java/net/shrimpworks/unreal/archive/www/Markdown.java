package net.shrimpworks.unreal.archive.www;

import java.io.IOException;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class Markdown {

	public static String renderMarkdown(ReadableByteChannel document) throws IOException {
		try (Reader reader = Channels.newReader(document, StandardCharsets.UTF_8)) {
			Parser parser = Parser.builder().build();
			Node doc = parser.parseReader(reader);
			HtmlRenderer renderer = HtmlRenderer.builder().build();
			return renderer.render(doc);
		}
	}

}
