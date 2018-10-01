package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

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

	public static String hash(Path path) throws IOException {
		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
			MessageDigest md = MessageDigest.getInstance("SHA-1");

			ByteBuffer buffer = ByteBuffer.allocate(4096);

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
}
