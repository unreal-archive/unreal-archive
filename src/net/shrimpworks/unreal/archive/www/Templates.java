package net.shrimpworks.unreal.archive.www;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.Normalizer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.SimpleNumber;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

import net.shrimpworks.unreal.archive.Util;

public class Templates {

	static final int PAGE_SIZE = 150;

	private static final Map<String, String> HOST_REMAP = new HashMap<>();

	private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
	private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

	private static final Configuration TPL_CONFIG = new Configuration(Configuration.VERSION_2_3_27);

	static {
		TPL_CONFIG.setClassForTemplateLoading(Maps.class, "");
		DefaultObjectWrapper ow = new DefaultObjectWrapper(TPL_CONFIG.getIncompatibleImprovements());
		ow.setExposeFields(true);
		TPL_CONFIG.setObjectWrapper(ow);
		TPL_CONFIG.setOutputEncoding(StandardCharsets.UTF_8.name());

		HOST_REMAP.put("f002.backblazeb2.com", "Unreal Archive");
	}

	public static Tpl template(String name) throws IOException {
		return new Tpl(TPL_CONFIG.getTemplate(name));
	}

	public static boolean unpackResources(String resourceBase, Path destination) throws IOException {
		try (InputStream in = Templates.class.getResourceAsStream(resourceBase);
			 BufferedReader br = new BufferedReader(new InputStreamReader(in))) {

			String resource;
			while ((resource = br.readLine()) != null) {
				Path destPath = destination.resolve(resource);
				if (!resource.contains(".")) {
					// we naively assume this to be a directory... we can't support directories with dots in their names
					Files.createDirectories(destPath);
					unpackResources(resourceBase + "/" + resource, destPath);
				} else {
					Files.copy(Templates.class.getResourceAsStream(resourceBase + "/" + resource), destPath,
							   StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}

		return true;
	}

	public static boolean unpackResourceZip(String zipFile, Path destination) throws IOException {
		Path tmpZip = Files.createTempFile("resource-" + zipFile, ".zip");
		try {
			Files.copy(Templates.class.getResourceAsStream(zipFile), tmpZip, StandardCopyOption.REPLACE_EXISTING);
			URI zipUri = URI.create("jar:file:" + tmpZip.toUri().getPath());
			FileSystem fs = FileSystems.newFileSystem(zipUri, Collections.emptyMap());
			Files.walkFileTree(fs.getPath("/"), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					String relDir = fs.getPath("/").relativize(dir).toString();
					if (!Files.exists(destination.resolve(relDir))) Files.createDirectories(destination.resolve(relDir));
					return super.preVisitDirectory(dir, attrs);
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					String relFile = fs.getPath("/").relativize(file).toString();
					Files.copy(Files.newInputStream(file), destination.resolve(relFile), StandardCopyOption.REPLACE_EXISTING);
					return super.visitFile(file, attrs);
				}
			});
		} finally {
			Files.deleteIfExists(tmpZip);
		}

		return true;
	}

	public static String slug(String input) {
		String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
		String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
		String slug = NONLATIN.matcher(normalized).replaceAll("");
		return slug.toLowerCase(Locale.ENGLISH).replaceAll("(-)\\1+", "-");
	}

	public static class Tpl {

		private static final Map<String, Object> TPL_VARS = new HashMap<>();

		static {
			TPL_VARS.put("relUrl", new RelUrlMethod());
			TPL_VARS.put("urlEncode", new UrlEncodeMethod());
			TPL_VARS.put("urlHost", new UrlHostMethod());
			TPL_VARS.put("fileSize", new FileSizeMethod());
			TPL_VARS.put("fileName", new FileNameMethod());
		}

		private final Template template;
		private final Map<String, Object> vars;

		public Tpl(Template template) {
			this.template = template;
			this.vars = new HashMap<>();
			this.vars.putAll(TPL_VARS);
		}

		public Tpl put(String var, Object val) {
			vars.put(var, val);
			return this;
		}

		public Tpl write(Path output) throws IOException {
			try (Writer writer = templateOut(output)) {
				template.process(vars, writer);
			} catch (TemplateException e) {
				throw new IOException("Template output failed", e);
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

	private static class UrlEncodeMethod implements TemplateMethodModelEx {

		public Object exec(@SuppressWarnings("rawtypes") List args) throws TemplateModelException {
			if (args.size() != 1) {
				throw new TemplateModelException("Wrong arguments");
			}

			String url = args.get(0).toString();

			if (!url.startsWith("http")) return url;

			try {
				return Util.toUriString(args.get(0).toString());
			} catch (IllegalArgumentException e) {
				throw new TemplateModelException("Invalid URL: " + args.get(0).toString(), e);
			}
		}
	}

	private static class FileSizeMethod implements TemplateMethodModelEx {

		private static final String[] SIZES = { "B", "KB", "MB", "GB", "TB" };

		public Object exec(@SuppressWarnings("rawtypes") List args) throws TemplateModelException {
			if (args.size() != 1) {
				throw new TemplateModelException("Wrong arguments");
			}

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
			if (args.size() != 1) {
				throw new TemplateModelException("Wrong arguments");
			}

			return Util.fileName(args.get(0).toString());
		}
	}

	private static class UrlHostMethod implements TemplateMethodModelEx {

		public Object exec(@SuppressWarnings("rawtypes") List args) throws TemplateModelException {
			if (args.size() != 1) {
				throw new TemplateModelException("Wrong arguments");
			}

			try {
				String host = new URL(args.get(0).toString()).getHost().replaceFirst("www\\.", "");
				return HOST_REMAP.getOrDefault(host, host);
			} catch (MalformedURLException e) {
				throw new TemplateModelException("Invalid URL: " + args.get(0).toString(), e);
			}
		}
	}

}
