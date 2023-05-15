package org.unrealarchive.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Version {

	private static String VERSION = "unknown";

	public static void setVersion(Class<?> relativeTo) {
		try (InputStream in = relativeTo.getResourceAsStream("VERSION")) {
			if (in != null) VERSION = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			else VERSION = "unknown";
		} catch (IOException e) {
			VERSION = "unknown";
		}
	}

	public static String version() {
		return VERSION;
	}

}
