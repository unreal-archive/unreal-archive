package net.shrimpworks.unreal.archive.www;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import net.shrimpworks.unreal.archive.indexer.ContentManager;
import net.shrimpworks.unreal.archive.indexer.maps.Map;

public class Maps {

	private static final Configuration TPL_CONFIG = new Configuration(Configuration.VERSION_2_3_27);

	static {
		TPL_CONFIG.setClassForTemplateLoading(Maps.class, "");
		DefaultObjectWrapper ow = new DefaultObjectWrapper(TPL_CONFIG.getIncompatibleImprovements());
		ow.setExposeFields(true);
		TPL_CONFIG.setObjectWrapper(ow);
	}

	private final ContentManager content;
	private final Path output;

	private final Collection<Map> allMaps;

	public Maps(ContentManager content, Path output) {
		this.content = content;
		this.allMaps = content.get(Map.class);
		this.output = output;
	}

	public void generate() {
		try {
			Path mapsPath = output.resolve("maps");
			try (Writer indexWriter = templateOut(mapsPath.resolve("index.html"))) {
				Template index = template("maps/index.ftl");
				java.util.Map<String, Object> vars = Collections.singletonMap("maps", allMaps);
				index.process(vars, indexWriter);
			}
		} catch (TemplateException | IOException e) {
			throw new RuntimeException("Failed to render page", e);
		}
	}

	private Template template(String name) throws IOException {
		return TPL_CONFIG.getTemplate(name);
	}

	private Writer templateOut(Path target) throws IOException {
		if (!Files.exists(target.getParent())) Files.createDirectories(target.getParent());
		return new BufferedWriter(new FileWriter(target.toFile()));
	}
}
