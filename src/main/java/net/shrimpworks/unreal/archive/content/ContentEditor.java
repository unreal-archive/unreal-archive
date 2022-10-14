package net.shrimpworks.unreal.archive.content;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.Collections;

import net.shrimpworks.unreal.archive.YAML;

/**
 * Utility for manipulating content data.
 */
public class ContentEditor {

	private final ContentManager contentManager;

	public ContentEditor(ContentManager contentManager) {
		this.contentManager = contentManager;
	}

	/**
	 * Spawns a text editor, loading the YAML document of the content specified,
	 * which may be modified and then written back to the repository.
	 *
	 * @param hash content to edit
	 * @throws IOException          file operations failed
	 * @throws InterruptedException failed to wait for text editor result
	 */
	public void edit(String hash) throws IOException, InterruptedException {
		Content content = contentManager.forHash(hash);
		if (content == null) {
			System.err.println("Content for provided hash does not exist!");
			System.exit(4);
		}

		Path yaml = Files.write(Files.createTempFile(content.hash, ".yml"), YAML.toString(content).getBytes(StandardCharsets.UTF_8),
								StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

		String editor = System.getenv().getOrDefault("UA_EDITOR", "sensible-editor");

		FileTime fileTime = Files.getLastModifiedTime(yaml);
		Process editorProcess = new ProcessBuilder(editor, yaml.toString()).inheritIO().start();
		int res = editorProcess.waitFor();
		if (res == 0) {
			if (!fileTime.equals(Files.getLastModifiedTime(yaml))) {
				Content updated = YAML.fromFile(yaml, Content.class);
				if (contentManager.checkin(new IndexResult<>(updated, Collections.emptySet()), null)) {
					System.out.println("Stored changes!");
				} else {
					System.out.println("Failed to apply");
				}
			} else {
				System.out.println("No changes!");
			}
		}
	}

	public void set(String hash, String attribute, String newValue) throws ReflectiveOperationException, IOException {
		Content content = contentManager.checkout(hash);
		if (content == null) {
			System.err.println("Content for provided hash does not exist!");
			System.exit(4);
		}

		Field field = content.getClass().getField(attribute);
		Object old = field.get(content);

		System.out.printf("Setting field %s from value %s to %s%n", field.getName(), old == null ? "<null>" : old, newValue);

		if (newValue.equalsIgnoreCase("null")) {
			field.set(content, null);
		} else if (field.getType().equals(boolean.class) || field.getType().equals(Boolean.class)) {
			field.setBoolean(content, Boolean.parseBoolean(newValue));
		} else if (field.getType().equals(long.class) || field.getType().equals(Long.class)) {
			field.set(content, Long.parseLong(newValue));
		} else if (field.getType().equals(int.class) || field.getType().equals(Integer.class)) {
			field.set(content, Integer.parseInt(newValue));
		} else if (field.getType().equals(short.class) || field.getType().equals(Short.class)) {
			field.set(content, Short.parseShort(newValue));
		} else if (field.getType().equals(double.class) || field.getType().equals(Double.class)) {
			field.set(content, Double.parseDouble(newValue));
		} else if (field.getType().equals(String.class)) {
			field.set(content, newValue);
		} else if (field.getType().isEnum()) {
			Method m = field.getType().getMethod("valueOf", String.class);
			field.set(content, m.invoke(field, newValue.trim()));
		}

		if (contentManager.checkin(new IndexResult<>(content, Collections.emptySet()), null)) {
			System.out.println("Stored changes!");
		} else {
			System.out.println("Failed to apply");
		}
	}
}
