package net.shrimpworks.unreal.archive;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

public final class Util {

	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

	private static final Map<String, String> MIME_TYPES = new HashMap<String, String>() {{
		put("bmp", "image/bmp");
		put("bz", "application/x-bzip");
		put("bz2", "application/x-bzip2");
		put("doc", "application/msword");
		put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		put("gif", "image/gif");
		put("gz", "application/gzip");
		put("htm", "text/html");
		put("html", "text/html");
		put("jar", "application/java-archive");
		put("jpeg", "image/jpeg");
		put("jpg", "image/jpeg");
		put("ods", "application/vnd.oasis.opendocument.spreadsheet");
		put("odt", "application/vnd.oasis.opendocument.text");
		put("ogg", "audio/ogg");
		put("png", "image/png");
		put("pdf", "application/pdf");
		put("rar", "application/x-rar-compressed");
		put("rtf", "application/rtf");
		put("svg", "image/svg+xml");
		put("tar", "application/x-tar");
		put("txt", "text/plain");
		put("ini", "text/plain");
		put("int", "text/plain");
		put("xhtml", "application/xhtml+xml");
		put("xls", "application/vnd.ms-excel");
		put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		put("zip", "application/zip");
		put("7z", "application/x-7z-compressed");
	}};

	private static final Set<String> IMGS = Set.of("png", "bmp", "gif", "jpg", "jpeg");

	private static final Pattern DISPOSITION_FILENAME = Pattern.compile(".*filename=\"?([^\"]*)\"?;?.*?");

	private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
	private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

	private static final Pattern UC_WORDS = Pattern.compile("\\b(.)(.*?)\\b");

	private static final int HASH_BUFFER_SIZE = 1024 * 50; // 50kb read buffer

	private Util() { }

	public static String extension(Path path) {
		return extension(path.toString());
	}

	public static String extension(String path) {
		return path.substring(path.lastIndexOf(".") + 1);
	}

	public static String fileName(Path path) {
		return fileName(path.toString());
	}

	public static String fileName(String path) {
		String tmp = path.replaceAll("\\\\", "/");
		return tmp.substring(Math.max(0, tmp.lastIndexOf("/") + 1));
	}

	public static String filePath(String path) {
		String tmp = path.replaceAll("\\\\", "/");
		return tmp.substring(0, Math.max(0, tmp.lastIndexOf("/")));
	}

	public static String plainName(Path path) {
		return plainName(path.toString());
	}

	public static String plainName(String path) {
		String tmp = fileName(path);
		return tmp.substring(0, tmp.lastIndexOf(".")).replaceAll("/", "").trim().replaceAll("[^\\x20-\\x7E]", "").trim();
	}

	public static String safeFileName(String name) {
		return name.trim().replaceAll("[\\/:*?\"<>|]", "_");
	}

	public static Path safeFileName(Path path) {
		return path.getParent().resolve(safeFileName(path.getFileName().toString()));
	}

	public static String slug(String input) {
		String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
		String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
		String slug = NONLATIN.matcher(normalized).replaceAll("");
		return slug.toLowerCase(Locale.ENGLISH).replaceAll("(-)\\1+", "-");
	}

	public static String authorSlug(String input) {
		String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
		String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
		String slug = NONLATIN.matcher(normalized).replaceAll("_");
		return slug.toLowerCase(Locale.ENGLISH).replaceAll("(-)\\1+", "-");
	}

	public static String capitalWords(String input) {
		return UC_WORDS.matcher(input).replaceAll(match -> match.group(1).toUpperCase() + match.group(2));
	}

	public static boolean image(Path path) {
		return IMGS.contains(extension(path).toLowerCase());
	}

	public static String hash(Path path) throws IOException {
		Path hashPath = path.resolveSibling(path.getFileName().toString() + ".sha1");
		if (Files.exists(hashPath)) {
			return Files.readString(hashPath).trim();
		}

		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
			MessageDigest md = MessageDigest.getInstance("SHA-1");

			ByteBuffer buffer = ByteBuffer.allocate(HASH_BUFFER_SIZE);

			while (channel.read(buffer) > 0) {
				buffer.flip();
				md.update(buffer);
				buffer.clear();
			}

			return bytesToHex(md.digest()).toLowerCase();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static String mimeType(String ext) {
		return MIME_TYPES.getOrDefault(ext, "application/octet-stream");
	}

	private static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int i = 0; i < bytes.length; i++) {
			int v = bytes[i] & 0xFF;
			hexChars[i * 2] = HEX_ARRAY[v >>> 4];
			hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static URI toUri(String s) {
		try {
			URL url = new URL(s);
			return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
		} catch (URISyntaxException | MalformedURLException e) {
			throw new IllegalArgumentException("Invalid URL: " + s, e);
		}
	}

	public static String toUriString(String s) {
		return toUri(s).toString()
					   .replaceAll("\\+", "%2B")
					   .replaceAll("#", "%23")
					   .replaceAll(",", "%2C")
					   .replaceAll("&", "%26");
	}

	public static Path downloadTo(String url, Path output) throws IOException {
		URL urlConnection = new URL(url);
		HttpURLConnection httpConn = (HttpURLConnection)urlConnection.openConnection();
		int responseCode = httpConn.getResponseCode();

		Path saveTo = output;

		// always check HTTP response code first
		if (responseCode == HttpURLConnection.HTTP_OK) {

			// if we're saving to a directory and not a specific file, try to determine the filename to save to
			if (Files.isDirectory(saveTo)) {
				String disposition = httpConn.getHeaderField("Content-Disposition");
				if (disposition != null && !disposition.trim().isEmpty()) {
					Matcher matcher = DISPOSITION_FILENAME.matcher(disposition);
					if (matcher.find()) {
						saveTo = saveTo.resolve(fileName(matcher.group(1)));
					}
				}

				// fallback, just use the filename from the url
				if (Files.isDirectory(saveTo)) saveTo = saveTo.resolve(fileName(httpConn.getURL().getPath()));
			}

			// opens input stream from the HTTP connection
			Files.copy(httpConn.getInputStream(), saveTo);
		} else {
			throw new IOException(responseCode + " Failed to download url " + url);
		}
		httpConn.disconnect();

		return saveTo;
	}

	public static void urlRequest(String url, Consumer<HttpURLConnection> onOK) throws IOException {
		URL urlConnection = new URL(url);
		HttpURLConnection httpConn = (HttpURLConnection)urlConnection.openConnection();
		int responseCode = httpConn.getResponseCode();

		if (responseCode == HttpURLConnection.HTTP_OK) {
			onOK.accept(httpConn);
		}

		httpConn.disconnect();
	}

	public static boolean uploadTo(Path localFile, String url) throws IOException {
		URL urlConnection = new URL(url);
		HttpURLConnection httpConn = (HttpURLConnection)urlConnection.openConnection();
		httpConn.setDoOutput(true);
		httpConn.setRequestMethod("PUT");
		httpConn.setRequestProperty("Content-Length", Long.toString(Files.size(localFile)));
		httpConn.connect();

		try (OutputStream output = httpConn.getOutputStream();
			 InputStream input = Files.newInputStream(localFile, StandardOpenOption.READ)) {
			byte[] buffer = new byte[4096];
			int length;
			while ((length = input.read(buffer)) > 0) {
				output.write(buffer, 0, length);
			}
			output.flush();

			return httpConn.getResponseCode() < 400;
		}
	}

	public static boolean deleteRemote(String url) throws IOException {
		URL urlConnection = new URL(url);
		HttpURLConnection httpConn = (HttpURLConnection)urlConnection.openConnection();
		httpConn.setRequestMethod("DELETE");
		httpConn.connect();
		return httpConn.getResponseCode() < 400;
	}

	public static void copyTree(Path source, Path dest) throws IOException {
		Files.walk(source, FileVisitOption.FOLLOW_LINKS)
			 .forEach(p -> {
				 if (Files.isRegularFile(p)) {
					 Path relPath = source.relativize(p);
					 Path copyPath = dest.resolve(relPath);

					 try {
						 if (!Files.isDirectory(copyPath.getParent())) Files.createDirectories(copyPath.getParent());
						 Files.copy(p, copyPath, StandardCopyOption.REPLACE_EXISTING);
					 } catch (IOException e) {
						 e.printStackTrace();
					 }
				 }
			 });
	}

	public static Path thumbnail(Path source, Path dest, int width) throws IOException {
		if (Files.exists(dest)) return dest;

		BufferedImage image = ImageIO.read(source.toFile());
		double scale = (double)width / image.getWidth();
		BufferedImage thumb = new BufferedImage((int)(image.getWidth() * scale),
												(int)(image.getHeight() * scale),
												BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = thumb.createGraphics();
		graphics.drawImage(image.getScaledInstance(thumb.getWidth(), thumb.getHeight(), Image.SCALE_SMOOTH), 0, 0, null);

		ImageIO.write(thumb, Util.extension(source), dest.toFile());
		Files.setLastModifiedTime(dest, Files.getLastModifiedTime(source));

		return dest;
	}
}
