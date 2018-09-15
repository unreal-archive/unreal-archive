package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Util {

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

	public static String sha1(Path original) throws IOException {
		try (FileChannel channel = FileChannel.open(original, StandardOpenOption.READ)) {
			MessageDigest md = MessageDigest.getInstance("SHA-1");

			ByteBuffer buffer = ByteBuffer.allocate(4096);

			while (channel.read(buffer) > 0) {
				buffer.flip();
				md.update(buffer);
				buffer.clear();
			}

			byte[] digest = md.digest();
			StringBuilder sb = new StringBuilder();
			for (byte b : digest) {
				sb.append(Integer.toHexString((0xFF & b)));
			}

			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
}
