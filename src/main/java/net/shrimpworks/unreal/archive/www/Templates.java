package net.shrimpworks.unreal.archive.www;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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
import java.nio.file.StandardOpenOption;
import java.text.DateFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import freemarker.core.Environment;
import freemarker.core.HTMLOutputFormat;
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
	private static final String SITE_URL = System.getenv().getOrDefault("SITE_URL", "");
	private static final String STATIC_ROOT = System.getenv().getOrDefault("STATIC_ROOT", "");
	private static final String DATA_PROJECT_URL = System.getenv().getOrDefault("DATA_PROJECT_URL",
																				"https://github.com/unreal-archive/unreal-archive-data");

	private static final Map<String, String> HOST_REMAP = new HashMap<>();

	private static final Pattern LINK_PREFIX = Pattern.compile("https?://.*");

	private static final String[] MONTH_NAMES = new DateFormatSymbols().getMonths();

	private static final Configuration TPL_CONFIG = new Configuration(Configuration.VERSION_2_3_27);

	private static final MutableDataSet MD_OPTIONS = new MutableDataSet();
	private static final Parser MD_PARSER = Parser.builder(MD_OPTIONS).build();
	private static final HtmlRenderer MD_RENDERER = HtmlRenderer.builder(MD_OPTIONS).build();

	static {
		TPL_CONFIG.setClassForTemplateLoading(Templates.class, "");
		DefaultObjectWrapper ow = new DefaultObjectWrapper(TPL_CONFIG.getIncompatibleImprovements());
		ow.setExposeFields(true);
		ow.setMethodAppearanceFineTuner((in, out) -> {
			out.setReplaceExistingProperty(false);
			out.setMethodShadowsProperty(false);
			try {
				in.getContainingClass().getField(in.getMethod().getName());
				// this did not throw a NoSuchFieldException, so we know there is a property named after the method - do not expose the method
				out.setExposeMethodAs(null);
			} catch (NoSuchFieldException e) {
				try {
					// we got a NoSuchFieldException, which means there's no property named after the method, so we can expose it
					out.setExposeAsProperty(
							new PropertyDescriptor(in.getMethod().getName(), in.getContainingClass(), in.getMethod().getName(), null)
					);
				} catch (IntrospectionException ex) {
					// pass
				}
				// pass
			}
		});
		TPL_CONFIG.setObjectWrapper(ow);
		TPL_CONFIG.setOutputEncoding(StandardCharsets.UTF_8.name());
		TPL_CONFIG.setOutputFormat(HTMLOutputFormat.INSTANCE);

		HOST_REMAP.put("f002.backblazeb2.com", "Unreal Archive US");
		HOST_REMAP.put("unreal-archive-files.eu-central-1.linodeobjects.com", "Unreal Archive EU");
		HOST_REMAP.put("files.vohzd.com", "vohzd");
		HOST_REMAP.put("unrealarchiveusa.blob.core.windows.net", "Azure US");
	}

	public static class PageSet {

		public final String resourceRoot;
		public final Set<SiteMap.Page> pages;
		public final Map<String, Object> vars;

		public PageSet(String resourceRoot, SiteFeatures features, Path siteRoot, Path staticPath, Path sectionPath) {
			this.resourceRoot = resourceRoot;
			this.pages = ConcurrentHashMap.newKeySet();
			this.vars = Map.of(
					"siteRoot", siteRoot,
					"staticRoot", staticPath,
					"sectionPath", sectionPath,
					"features", features
			);
		}

		public Tpl add(String template, SiteMap.Page page, String title) {
			if (page != null) this.pages.add(page);
			try {
				return template(String.join("/", resourceRoot, template), page)
						.put("title", title)
						.putAll(vars);
			} catch (IOException e) {
				throw new RuntimeException(String.format("Failed to create template %s", resourceRoot + "/" + template), e);
			}
		}
	}

	public static Tpl template(String name) throws IOException {
		return new Tpl(TPL_CONFIG.getTemplate(name), SiteMap.DEFAULT_PAGE);
	}

	public static Tpl template(String name, SiteMap.Page page) throws IOException {
		return new Tpl(TPL_CONFIG.getTemplate(name), page);
	}

	public static boolean unpackResources(String resourceList, Path destination) throws IOException {
		final Set<ThumbConfig> thumbConfig = new HashSet<>();

		try (InputStream in = Templates.class.getResourceAsStream(resourceList);
			 BufferedReader br = new BufferedReader(new InputStreamReader(in))) {

			String resource;
			while ((resource = br.readLine()) != null) {
				try (InputStream res = Templates.class.getResourceAsStream(resource)) {
					Path destPath = destination.resolve(resource);
					if (destPath.getFileName().toString().equals("thumbs")) {
						try (BufferedReader thumbDef = new BufferedReader(new InputStreamReader(res))) {
							thumbConfig.add(new ThumbConfig(destPath.getParent(), thumbDef.readLine()));
						}
					} else {
						Files.createDirectories(destPath.getParent());
						Files.copy(res, destPath, StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}
		}

		for (ThumbConfig conf : thumbConfig) {
			Files.walk(conf.path)
				 .filter(Util::image)
				 .forEach(f -> {
					 try {
						 Util.thumbnail(f, f.getParent().resolve(String.format("%s_%s", conf.name, Util.fileName(f))),
										conf.maxWidth);
					 } catch (IOException e) {
						 throw new RuntimeException("Failed to generate thumbnail for file " + f.toAbsolutePath().toString(), e);
					 }
				 });
		}

		return true;
	}

	public static String renderMarkdown(ReadableByteChannel document) throws IOException {
		try (Reader reader = Channels.newReader(document, StandardCharsets.UTF_8.name())) {
			Node node = MD_PARSER.parseReader(reader);
			return MD_RENDERER.render(node);
		}
	}

	private static class ThumbConfig {

		public final Path path;
		public final String name;
		public final int maxWidth;
		public final int maxHeight;

		public ThumbConfig(Path path, String config) {
			String[] nameSize = config.split("=");
			String[] widthHeight = nameSize[1].split("x");

			this.path = path;
			this.name = nameSize[0];
			this.maxWidth = Integer.parseInt(widthHeight[0]);
			this.maxHeight = Integer.parseInt(widthHeight[1]);
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
			TPL_VARS.put("plainName", new PlainNameMethod());
			TPL_VARS.put("staticPath", new StaticPathMethod());
			TPL_VARS.put("dateFmt", new FormatLocalDateMethod(false));
			TPL_VARS.put("dateFmtShort", new FormatLocalDateMethod(true));
			TPL_VARS.put("trunc", new TruncateStringMethod());
			TPL_VARS.put("slug", new SlugMethod());
			TPL_VARS.put("authorSlug", new AuthorSlugMethod());
			TPL_VARS.put("siteName", SITE_NAME);
			TPL_VARS.put("siteUrl", SITE_URL);
			TPL_VARS.put("dataProjectUrl", DATA_PROJECT_URL);
			TPL_VARS.put("monthNames", MONTH_NAMES);
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

		public SiteMap.Page write(Path output) {
			try (Writer writer = templateOut(output)) {
				vars.put("pagePath", output.getParent().toAbsolutePath());
				template.process(vars, writer);
			} catch (TemplateException | IOException e) {
				throw new RuntimeException("Template output failed", e);
			}

			return page.withPath(output);
		}

		private Writer templateOut(Path target) throws IOException {
			if (!Files.exists(target.getParent())) Files.createDirectories(target.getParent());
			return Channels.newWriter(
					Files.newByteChannel(
							target, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
					), StandardCharsets.UTF_8);
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

			return args.get(0).toString().replaceAll("\n", "%0A");
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

	private static class TruncateStringMethod implements TemplateMethodModelEx {

		private static final String ELLIPSIS = "…";

		public Object exec(@SuppressWarnings("rawtypes") List args) throws TemplateModelException {
			if (args.size() < 2) throw new TemplateModelException("Wrong arguments, expecting a string to truncate and maximum size");

			String string = args.get(0).toString();
			int maxLength = Integer.parseInt(args.get(1).toString());
			string = string.length() <= maxLength
					? string
					: string.substring(0, Math.min(maxLength, string.length())) + ELLIPSIS;

			return string;
		}
	}

	private static class SlugMethod implements TemplateMethodModelEx {

		public Object exec(@SuppressWarnings("rawtypes") List args) throws TemplateModelException {
			if (args.size() != 1) throw new TemplateModelException("Wrong arguments, expecting a string");

			String string = args.get(0).toString();
			return Util.slug(string);
		}
	}

	private static class AuthorSlugMethod implements TemplateMethodModelEx {

		public Object exec(@SuppressWarnings("rawtypes") List args) throws TemplateModelException {
			if (args.size() != 1) throw new TemplateModelException("Wrong arguments, expecting a string");

			String string = args.get(0).toString();
			return Util.authorSlug(string);
		}
	}

	private static class FormatLocalDateMethod implements TemplateMethodModelEx {

		private final boolean shortDate;

		private static final DateTimeFormatter IN_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		private static final DateTimeFormatter OUT_FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy");

		private static final DateTimeFormatter IN_FMT_SHORT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		private static final DateTimeFormatter OUT_FMT_SHORT = DateTimeFormatter.ofPattern("MMMM yyyy");

		private FormatLocalDateMethod(boolean shortDate) {
			this.shortDate = shortDate;
		}

		public Object exec(@SuppressWarnings("rawtypes") List args) throws TemplateModelException {
			if (args.isEmpty()) throw new TemplateModelException("Wrong arguments, expecting a date");

			if (shortDate) return OUT_FMT_SHORT.format(IN_FMT_SHORT.parse(args.get(0).toString() + "-01"));
			else return OUT_FMT.format(IN_FMT.parse(args.get(0).toString()));
		}
	}

	private static class FileNameMethod implements TemplateMethodModelEx {

		public Object exec(@SuppressWarnings("rawtypes") List args) throws TemplateModelException {
			if (args.size() != 1) throw new TemplateModelException("Wrong arguments, expecting a file path");

			return Util.fileName(args.get(0).toString());
		}
	}

	private static class PlainNameMethod implements TemplateMethodModelEx {

		public Object exec(@SuppressWarnings("rawtypes") List args) throws TemplateModelException {
			if (args.size() != 1) throw new TemplateModelException("Wrong arguments, expecting a file name");

			return Util.plainName(args.get(0).toString());
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
			if (!args.isEmpty()) System.err.printf("Deprecation warning: `staticPath` takes no arguments in %s.%n",
												   Environment.getCurrentEnvironment().getCurrentTemplate().getName());

			if (!STATIC_ROOT.isEmpty()) return STATIC_ROOT;

			TemplateModel pagePath = Environment.getCurrentEnvironment().getVariable("pagePath");
			TemplateModel staticRoot = Environment.getCurrentEnvironment().getVariable("staticRoot");
			if (pagePath == null) throw new TemplateModelException("A pagePath variable was not found");
			if (staticRoot == null) throw new TemplateModelException("A staticRoot variable was not found");

			return Paths.get(pagePath.toString()).relativize(Paths.get(staticRoot.toString()));
		}
	}

}
