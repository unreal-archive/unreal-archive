package org.unrealarchive.www;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.text.DateFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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

import org.unrealarchive.common.Util;
import org.unrealarchive.common.Version;
import org.unrealarchive.content.FileType;

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

	private static final Map<String, Object> TPL_VARS = new HashMap<>();

	static {
		TPL_VARS.put("relPath", new RelPathMethod());
		TPL_VARS.put("rootPath", new RootPathMethod());
		TPL_VARS.put("relUrl", new RelUrlMethod());
		TPL_VARS.put("urlEncode", new UrlEncodeMethod());
		TPL_VARS.put("urlHost", new UrlHostMethod());
		TPL_VARS.put("fileType", new FileTypeMethod());
		TPL_VARS.put("fileSize", new FileSizeMethod());
		TPL_VARS.put("fileName", new FileNameMethod());
		TPL_VARS.put("plainName", new PlainNameMethod());
		TPL_VARS.put("staticPath", new StaticPathMethod());
		TPL_VARS.put("dateFmt", new FormatLocalDateMethod(false));
		TPL_VARS.put("dateFmtShort", new FormatLocalDateMethod(true));
		TPL_VARS.put("trunc", new TruncateStringMethod());
		TPL_VARS.put("slug", new SlugMethod());
		TPL_VARS.put("authorSlug", new AuthorSlugMethod());
		TPL_VARS.put("version", Version.version());
		TPL_VARS.put("siteName", SITE_NAME);
		TPL_VARS.put("siteUrl", SITE_URL);
		TPL_VARS.put("dataProjectUrl", DATA_PROJECT_URL);
		TPL_VARS.put("monthNames", MONTH_NAMES);

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
		TPL_CONFIG.setOutputFormat(HTMLOutputFormat.INSTANCE); // force, don't attempt to auto detect
		TPL_CONFIG.setWhitespaceStripping(true); // remove whitespace from some directives
		try {
			TPL_CONFIG.setSharedVariables(TPL_VARS);
		} catch (Exception e) {
			e.printStackTrace();
		}
		TPL_CONFIG.setTagSyntax(Configuration.ANGLE_BRACKET_TAG_SYNTAX); // force - don't bother with auto-detection
		TPL_CONFIG.setTemplateUpdateDelayMilliseconds(Long.MAX_VALUE); // do not bother looking for updated templates

		HOST_REMAP.put("f002.backblazeb2.com", "Unreal Archive US [B2]");
		HOST_REMAP.put("unreal-archive-files-s3.s3.us-west-002.backblazeb2.com", "Unreal Archive US");
		HOST_REMAP.put("unreal-archive-files.eu-central-1.linodeobjects.com", "Unreal Archive EU");
		HOST_REMAP.put("files.vohzd.com", "vohzd");
		HOST_REMAP.put("medor.no-ip.org", "medor");
		HOST_REMAP.put("ut-files.com", "UT-Files");
		HOST_REMAP.put("ut2004.ut-files.com", "UT-Files");
		HOST_REMAP.put("unrealarchiveusa.blob.core.windows.net", "Azure US");
		HOST_REMAP.put("unrealarchivesgp.blob.core.windows.net", "Azure Singapore");
		HOST_REMAP.put("moddb.com", "ModDB");
		HOST_REMAP.put("utzone.de", "UTzone.de");
		HOST_REMAP.put("gamebanana.com", "GameBanana");
	}

	public static class PageSet {

		private static final int PAGES_INITIAL_SIZE = 150000;

		public final String resourceRoot;
		public final Set<SiteMap.Page> pages;
		public final Map<String, Object> vars;

		public PageSet(String resourceRoot, SiteFeatures features, Path siteRoot, Path staticPath) {
			this.resourceRoot = resourceRoot;
			this.pages = ConcurrentHashMap.newKeySet(PAGES_INITIAL_SIZE);
			this.vars = Map.of(
				"siteRoot", siteRoot,
				"staticRoot", staticPath,
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

	public static Tpl template(String name, SiteMap.Page page) throws IOException {
		return new Tpl(TPL_CONFIG.getTemplate(name), page);
	}

	public static void unpackResources(String resourceList, Path destination) throws IOException {
		final Set<Thumbnails.ThumbConfig> thumbConfig = new HashSet<>();

		try (InputStream in = Templates.class.getResourceAsStream(resourceList);
			 BufferedReader br = new BufferedReader(new InputStreamReader(in))) {

			String line;
			while ((line = br.readLine()) != null) {
				String[] nameAndDate = line.split("\t");
				String resource = nameAndDate[0];
				long lastModified = Long.parseLong(nameAndDate[1]);
				try (InputStream res = Templates.class.getResourceAsStream(resource)) {
					assert res != null;
					Path destPath = destination.resolve(resource);
					if (destPath.getFileName().toString().equals("generate_thumbs")) {
						try (BufferedReader thumbDef = new BufferedReader(new InputStreamReader(res))) {
							thumbConfig.add(new Thumbnails.ThumbConfig(destPath.getParent(), thumbDef.readLine()));
						}
					} else {
						Files.createDirectories(destPath.getParent());
						Files.copy(res, destPath, StandardCopyOption.REPLACE_EXISTING);
						Files.setLastModifiedTime(destPath, FileTime.fromMillis(lastModified));
					}
				}
			}
		}

		for (Thumbnails.ThumbConfig conf : thumbConfig) {
			try (Stream<Path> files = Files.walk(conf.path, conf.noSubDirectories ? 1 : 5)) {
				files
					.filter(Util::image)
					.forEach(f -> {
						try {
							Thumbnails.thumbnail(f, f.getParent(), conf);
						} catch (IOException e) {
							throw new RuntimeException("Failed to generate thumbnail for file " + f.toAbsolutePath(), e);
						}
					});
			}
		}
	}

	public static class Tpl {

		private final Template template;
		private final Map<String, Object> vars;
		private final SiteMap.Page page;

		public Tpl(Template template, SiteMap.Page page) {
			this.template = template;
			this.vars = new HashMap<>();
			this.vars.put("timestamp", new Date());
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

	private static class RelPathMethod implements TemplateMethodModelEx {

		public Object exec(List args) throws TemplateModelException {
			if (args.size() != 1) throw new TemplateModelException("Wrong arguments, expecting a path");
			TemplateModel pagePath = Environment.getCurrentEnvironment().getVariable("pagePath");
			if (pagePath == null) throw new TemplateModelException("A pagePath variable was not found");

			return Paths.get(pagePath.toString()).relativize(Paths.get(args.getFirst().toString()));
		}
	}

	private static class RootPathMethod implements TemplateMethodModelEx {

		public Object exec(List args) throws TemplateModelException {
			if (args.size() != 1) throw new TemplateModelException("Wrong arguments, expecting a path");
			TemplateModel pagePath = Environment.getCurrentEnvironment().getVariable("pagePath");
			if (pagePath == null) throw new TemplateModelException("A pagePath variable was not found");
			TemplateModel siteRoot = Environment.getCurrentEnvironment().getVariable("siteRoot");
			if (siteRoot == null) throw new TemplateModelException("A pagePath variable was not found");

			return Paths.get(pagePath.toString()).relativize(Paths.get(siteRoot.toString()).resolve(Paths.get(args.getFirst().toString())));
		}
	}

	private static class RelUrlMethod implements TemplateMethodModelEx {

		public Object exec(List args) throws TemplateModelException {
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

		public Object exec(List args) throws TemplateModelException {
			if (args.size() != 1) throw new TemplateModelException("Wrong arguments, expecting a URL to encode");

			return args.getFirst().toString().replaceAll("\n", "%0A");
		}
	}

	private static class FileSizeMethod implements TemplateMethodModelEx {

		private static final String[] SIZES = { "B", "KB", "MB", "GB", "TB" };

		public Object exec(List args) throws TemplateModelException {
			if (args.size() != 1) throw new TemplateModelException("Wrong arguments, expecting a file size");

			float size = ((SimpleNumber)args.getFirst()).getAsNumber().floatValue();

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

		public Object exec(List args) throws TemplateModelException {
			if (args.size() < 2) throw new TemplateModelException("Wrong arguments, expecting a string to truncate and maximum size");

			String string = args.get(0).toString();
			int maxLength = Integer.parseInt(args.get(1).toString());
			string = string.length() <= maxLength
				? string
				: string.substring(0, maxLength) + ELLIPSIS;

			return string;
		}
	}

	private static class SlugMethod implements TemplateMethodModelEx {

		public Object exec(List args) throws TemplateModelException {
			if (args.size() != 1) throw new TemplateModelException("Wrong arguments, expecting a string");

			String string = args.getFirst().toString();
			return Util.slug(string);
		}
	}

	private static class AuthorSlugMethod implements TemplateMethodModelEx {

		public Object exec(List args) throws TemplateModelException {
			if (args.size() != 1) throw new TemplateModelException("Wrong arguments, expecting a string");

			String string = args.getFirst().toString();
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

		public Object exec(List args) throws TemplateModelException {
			if (args.isEmpty()) throw new TemplateModelException("Wrong arguments, expecting a date");

			String arg = args.getFirst().toString();

			if (arg.equalsIgnoreCase("Unknown")) return arg;

			TemporalAccessor date;
			if (arg.matches("\\d{4}-\\d{2}-\\d{2}")) date = IN_FMT.parse(arg);
			else date = IN_FMT_SHORT.parse(arg + "-01");

			if (shortDate) return OUT_FMT_SHORT.format(date);
			else return OUT_FMT.format(date);
		}
	}

	private static class FileTypeMethod implements TemplateMethodModelEx {

		public Object exec(List args) throws TemplateModelException {
			if (args.size() != 1) throw new TemplateModelException("Wrong arguments, expecting a file path");

			return FileType.forFile(args.getFirst().toString());
		}
	}

	private static class FileNameMethod implements TemplateMethodModelEx {

		public Object exec(List args) throws TemplateModelException {
			if (args.size() != 1) throw new TemplateModelException("Wrong arguments, expecting a file path");

			return Util.fileName(args.getFirst().toString());
		}
	}

	private static class PlainNameMethod implements TemplateMethodModelEx {

		public Object exec(List args) throws TemplateModelException {
			if (args.size() != 1) throw new TemplateModelException("Wrong arguments, expecting a file name");

			return Util.plainName(args.getFirst().toString());
		}
	}

	private static class UrlHostMethod implements TemplateMethodModelEx {

		public Object exec(List args) throws TemplateModelException {
			if (args.size() != 1) throw new TemplateModelException("Wrong arguments, expecting a URL");

			try {
				String host = Util.url(args.getFirst().toString()).getHost().replaceFirst("www\\.", "");
				return HOST_REMAP.getOrDefault(host, host);
			} catch (MalformedURLException e) {
				throw new TemplateModelException("Invalid URL: " + args.getFirst().toString(), e);
			}
		}
	}

	private static class StaticPathMethod implements TemplateMethodModelEx {

		public Object exec(List args) throws TemplateModelException {
			if (!STATIC_ROOT.isEmpty()) return STATIC_ROOT;

			TemplateModel pagePath = Environment.getCurrentEnvironment().getVariable("pagePath");
			TemplateModel staticRoot = Environment.getCurrentEnvironment().getVariable("staticRoot");
			if (pagePath == null) throw new TemplateModelException("A pagePath variable was not found");
			if (staticRoot == null) throw new TemplateModelException("A staticRoot variable was not found");

			return Paths.get(pagePath.toString()).relativize(Paths.get(staticRoot.toString()));
		}
	}

}
