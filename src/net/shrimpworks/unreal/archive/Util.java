package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Util {

	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

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
		return path.substring(Math.max(0, path.lastIndexOf("/") + 1));
	}

	public static String sha1(Path path) throws IOException {
		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
			MessageDigest md = MessageDigest.getInstance("SHA-1");

			ByteBuffer buffer = ByteBuffer.allocate(4096);

			while (channel.read(buffer) > 0) {
				buffer.flip();
				md.update(buffer);
				buffer.clear();
			}

			return bytesToHex(md.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}
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
