package org.unrealarchive.www;

import java.io.IOException;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.commonmark.Extension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.ext.gfm.tables.TablesExtension;


public class Markdown {
	private static List<Extension> extensions = List.of(TablesExtension.create());
	private static Parser parser = Parser.builder().extensions(extensions).build();
	private static HtmlRenderer renderer = HtmlRenderer.builder().extensions(extensions).build();

	public static String renderMarkdown(ReadableByteChannel document) throws IOException {
		try (Reader reader = Channels.newReader(document, StandardCharsets.UTF_8)) {
			Node doc = parser.parseReader(reader);
			return renderer.render(doc);
		}
	}

}
