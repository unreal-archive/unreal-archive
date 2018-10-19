package net.shrimpworks.unreal.archive.www;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class Templates {

	private static final Configuration TPL_CONFIG = new Configuration(Configuration.VERSION_2_3_27);

	static {
		TPL_CONFIG.setClassForTemplateLoading(Maps.class, "");
		DefaultObjectWrapper ow = new DefaultObjectWrapper(TPL_CONFIG.getIncompatibleImprovements());
		ow.setExposeFields(true);
		TPL_CONFIG.setObjectWrapper(ow);
	}

	public static Tpl template(String name) throws IOException {
		return new Tpl(TPL_CONFIG.getTemplate(name));
	}

	public static class Tpl {

		private final Template template;
		private final Map<String, Object> vars;

		public Tpl(Template template) {
			this.template = template;
			this.vars = new HashMap<>();
			this.vars.put("relUrl", new RelUrlMethod());
		}

		public Tpl put(String var, Object val) {
			vars.put(var, val);
			return this;
		}

		public Tpl write(Path output) throws IOException {
			try (Writer writer = templateOut(output)) {
				template.process(vars, writer);
			} catch (TemplateException e) {
				throw new IOException("Template outout failed", e);
			}

			return this;
		}

		private Writer templateOut(Path target) throws IOException {
			if (!Files.exists(target.getParent())) Files.createDirectories(target.getParent());
			return new BufferedWriter(new FileWriter(target.toFile()));
		}

	}

	private static class RelUrlMethod implements TemplateMethodModelEx {

		public Object exec(@SuppressWarnings("rawtypes") List args) throws TemplateModelException {
			if (args.size() != 2) {
				throw new TemplateModelException("Wrong arguments");
			}
			return Paths.get(args.get(0).toString()).relativize(Paths.get(args.get(1).toString()));
		}
	}
}
