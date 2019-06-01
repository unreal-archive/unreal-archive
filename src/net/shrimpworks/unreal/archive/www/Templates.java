package net.shrimpworks.unreal.archive.www;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.github.rjeschke.txtmark.Processor;
import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.SimpleNumber;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

import net.shrimpworks.unreal.archive.Util;

public class Templates {

	public static final int PAGE_SIZE = 150;

	private static final String SITE_NAME = System.getenv().getOrDefault("SITE_NAME", "Unreal Archive");
	private static final String STATIC_ROOT = System.getenv().getOrDefault("STATIC_ROOT", "");

	private static final Map<String, String> HOST_REMAP = new HashMap<>();

	private static final Pattern LINK_PREFIX = Pattern.compile("https?://.*");

	private static final Configuration TPL_CONFIG = new Configuration(Configuration.VERSION_2_3_27);

	private static final com.github.rjeschke.txtmark.Configuration MD_CONFIG = com.github.rjeschke.txtmark.Configuration
			.builder()
			.forceExtentedProfile()
			.setEncoding(StandardCharsets.UTF_8.name())
			.build();

	static {
		TPL_CONFIG.setClassForTemplateLoading(Templates.class, "");
		DefaultObjectWrapper ow = new DefaultObjectWrapper(TPL_CONFIG.getIncompatibleImprovements());
		ow.setExposeFields(true);
		TPL_CONFIG.setObjectWrapper(ow);
		TPL_CONFIG.setOutputEncoding(StandardCharsets.UTF_8.name());

		HOST_REMAP.put("f002.backblazeb2.com", SITE_NAME);
	}

	public static class PageSet {

		public final String resourceRoot;
		public final Set<SiteMap.Page> pages;
		public final Map<String, Object> vars;

		public PageSet(String resourceRoot, Path siteRoot, Path staticPath, Path sectionPath) {
			this.resourceRoot = resourceRoot;
			this.pages = new HashSet<>();
			this.vars = Map.of(
					"siteRoot", siteRoot,
					"staticRoot", staticPath,
					"sectionPath", sectionPath
			);
		}

		public Tpl add(String template, SiteMap.Page page, String title) throws IOException {
			return template(String.join("/", resourceRoot, template), page)
					.put("title", title)
					.putAll(vars);
		}
	}

	public static Tpl template(String name) throws IOException {
		return new Tpl(TPL_CONFIG.getTemplate(name), SiteMap.DEFAULT_PAGE);
	}

	public static Tpl template(String name, SiteMap.Page page) throws IOException {
		return new Tpl(TPL_CONFIG.getTemplate(name), page);
	}

	public static boolean unpackResources(String resourceList, Path destination) throws IOException {
		try (InputStream in = Templates.class.getResourceAsStream(resourceList);
			 BufferedReader br = new BufferedReader(new InputStreamReader(in))) {

			String resource;
			while ((resource = br.readLine()) != null) {
				Path destPath = destination.resolve(resource);
				Files.createDirectories(destPath.getParent());
				Files.copy(Templates.class.getResourceAsStream(resource), destPath, StandardCopyOption.REPLACE_EXISTING);
			}
		}

		return true;
	}

	public static String renderMarkdown(ReadableByteChannel document) throws IOException {
		try (InputStream is = Channels.newInputStream(document)) {
			return Processor.process(is, MD_CONFIG);
		}
	}

	public static class Tpl {

		private static final Map<String, Object> TPL_VARS = new HashMap<>();

		static {
			TPL_VARS.put("relPath", new RelPageMethod());
			TPL_VARS.put("relUrl", new RelUrlMethod());
			TPL_VARS.put("urlEncode", new UrlEncodeMethod());
			TPL_VARS.put("urlHost", new UrlHostMethod());
			TPL_VARS.put("fileSize", new FileSizeMethod());
			TPL_VARS.put("fileName", new FileNameMethod());
			TPL_VARS.put("staticPath", new StaticPathMethod());
			TPL_VARS.put("siteName", SITE_NAME);
		}

		private final Template template;
		private final Map<String, Object> vars;

		private final SiteMap.Page page;

		public Tpl(Template template, SiteMap.Page page) {
			this.template = template;
			this.vars = new HashMap<>();
			this.vars.put("timestamp", new Date());
			this.vars.putAll(TPL_VARS);
			this.page = page;
		}

		public Tpl put(String var, Object val) {
			this.vars.put(var, val);
			return this;
		}

		public Tpl putAll(Map<String, Object> vars) {
			this.vars.putAll(vars);
			return this;
		}

		public SiteMap.Page write(Path output) throws IOException {
			try (Writer writer = templateOut(output)) {
				vars.put("pagePath", output.getParent().toAbsolutePath());
				template.process(vars, writer);
			} catch (TemplateException e) {
				throw new IOException("Template output failed", e);
			}

			return page.withPath(output);
		}

		private Writer templateOut(Path target) throws IOException {
			if (!Files.exists(target.getParent())) Files.createDirectories(target.getParent());
			return new BufferedWriter(new FileWriter(target.toFile()));
		}

	}

	private static class RelPageMethod implements TemplateMethodModelEx {

		public Object exec(@SuppressWarnings("rawtypes") List args) throws TemplateModelException {
			if (args.size() != 1) throw new TemplateModelException("Wrong arguments, expecting a path");
			TemplateModel pagePath = Environment.getCurrentEnvironment().getVariable("pagePath");
			if (pagePath == null) throw new TemplateModelException("A pagePath variable was not found");

			return Paths.get(pagePath.toString()).relativize(Paths.get(args.get(0).toString()));
		}
	}

	private static class RelUrlMethod implements TemplateMethodModelEx {

		public Object exec(@SuppressWarnings("rawtypes") List args) throws TemplateModelException {
			if (args.size() != 2) throw new TemplateModelException("Wrong arguments, expecting two paths");

			String one = args.get(0).toString();
			String two = args.get(1).toString();

			if (LINK_PREFIX.matcher(one).matches()) return one;
			if (LINK_PREFIX.matcher(two).matches()) return two;

			// simply join already relative paths
			if (one.startsWith(".")) return String.join("/", one, two);

			return Paths.get(one).relativize(Paths.get(two));
		}
	}

	private static class UrlEncodeMethod implements TemplateMethodModelEx {

		public Object exec(@SuppressWarnings("rawtypes") List args) throws TemplateModelException {
			if (args.size() != 1) throw new TemplateModelException("Wrong arguments, expecting a URL to encode");

			return args.get(0).toString();
		}
	}

	private static class FileSizeMethod implements TemplateMethodModelEx {

		private static final String[] SIZES = { "B", "KB", "MB", "GB", "TB" };

		public Object exec(@SuppressWarnings("rawtypes") List args) throws TemplateModelException {
			if (args.size() != 1) throw new TemplateModelException("Wrong arguments, expecting a file size");

			float size = ((SimpleNumber)args.get(0)).getAsNumber().floatValue();

			int cnt = 0;
			while (size > 1024) {
				size = size / 1024f;
				cnt++;
			}

			return String.format("%.1f %s", size, SIZES[cnt]);
		}
	}

	private static class FileNameMethod implements TemplateMethodModelEx {

		public Object exec(@SuppressWarnings("rawtypes") List args) throws TemplateModelException {
			if (args.size() != 1) throw new TemplateModelException("Wrong arguments, expecting a file path");

			return Util.fileName(args.get(0).toString());
		}
	}

	private static class UrlHostMethod implements TemplateMethodModelEx {

		public Object exec(@SuppressWarnings("rawtypes") List args) throws TemplateModelException {
			if (args.size() != 1) throw new TemplateModelException("Wrong arguments, expecting a URL");

			try {
				String host = new URL(args.get(0).toString()).getHost().replaceFirst("www\\.", "");
				return HOST_REMAP.getOrDefault(host, host);
			} catch (MalformedURLException e) {
				throw new TemplateModelException("Invalid URL: " + args.get(0).toString(), e);
			}
		}
	}

	private static class StaticPathMethod implements TemplateMethodModelEx {

		public Object exec(@SuppressWarnings("rawtypes") List args) throws TemplateModelException {
			if (!args.isEmpty()) System.err.println("Deprecation warning: `staticPath` takes no arguments.");

			if (!STATIC_ROOT.isEmpty()) return STATIC_ROOT;

			TemplateModel pagePath = Environment.getCurrentEnvironment().getVariable("pagePath");
			TemplateModel staticRoot = Environment.getCurrentEnvironment().getVariable("staticRoot");
			if (pagePath == null) throw new TemplateModelException("A pagePath variable was not found");
			if (staticRoot == null) throw new TemplateModelException("A staticRoot variable was not found");

			return Paths.get(pagePath.toString()).relativize(Paths.get(staticRoot.toString()));
		}
	}

}
