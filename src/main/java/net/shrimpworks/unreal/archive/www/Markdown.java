package net.shrimpworks.unreal.archive.www;

import java.io.IOException;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

//import com.vladsch.flexmark.html.HtmlRenderer;
//import com.vladsch.flexmark.parser.Parser;
//import com.vladsch.flexmark.util.ast.Node;
//import com.vladsch.flexmark.util.data.MutableDataSet;

public class Markdown {

//	private static final MutableDataSet MD_OPTIONS = new MutableDataSet();
//	private static final Parser MD_PARSER = Parser.builder(MD_OPTIONS).build();
//	private static final HtmlRenderer MD_RENDERER = HtmlRenderer.builder(MD_OPTIONS).build();

	public static String renderMarkdown(ReadableByteChannel document) throws IOException {
//		Parser parser = Parser.builder().build();
//		Node document = parser.parse("This is *Sparta*");
//		HtmlRenderer renderer = HtmlRenderer.builder().build();
//		renderer.render(document);

		try (Reader reader = Channels.newReader(document, StandardCharsets.UTF_8)) {
			Parser parser = Parser.builder().build();
			Node doc = parser.parseReader(reader);
			HtmlRenderer renderer = HtmlRenderer.builder().build();
			return renderer.render(doc);
		}
	}

}
