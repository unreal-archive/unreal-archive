package net.shrimpworks.unreal.archive.indexing;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.Set;

import net.shrimpworks.unreal.archive.common.YAML;
import net.shrimpworks.unreal.archive.content.addons.Addon;
import net.shrimpworks.unreal.archive.content.FileType;

/**
 * Utility for manipulating content data.
 */
public class ContentEditor {

	private final ContentManager contentManager;

	public ContentEditor(ContentManager contentManager) {
		this.contentManager = contentManager;
	}

	private Addon checkoutContent(String hash) {
		Addon content = contentManager.checkout(hash);
		if (content == null) {
			System.err.println("Content for provided hash does not exist!");
			System.exit(4);
		}

		return content;
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
		Addon content = checkoutContent(hash);

		Path yaml = Files.write(Files.createTempFile(content.hash, ".yml"), YAML.toString(content).getBytes(StandardCharsets.UTF_8),
								StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

		String editor = System.getenv().getOrDefault("UA_EDITOR", "sensible-editor");

		FileTime fileTime = Files.getLastModifiedTime(yaml);
		Process editorProcess = new ProcessBuilder(editor, yaml.toString()).inheritIO().start();
		int res = editorProcess.waitFor();
		if (res == 0) {
			if (!fileTime.equals(Files.getLastModifiedTime(yaml))) {
				Addon updated = YAML.fromFile(yaml, Addon.class);
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
		if (attribute.equalsIgnoreCase("attach")) {
			attach(hash, newValue);
			return;
		}

		Addon content = checkoutContent(hash);

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

	public void attach(String hash, String attachment) throws IOException {
		Addon content = checkoutContent(hash);

		Path attfile = Paths.get(attachment);

		if (!Files.exists(attfile)) {
			System.err.printf("Attachment file \"%s\" does not exist!%n", attachment);
			System.exit(5);
		}

		if (!FileType.IMAGE.matches(attachment)) {
			System.err.printf("Attachment file \"%s\" is not an image!%n", attachment);
			System.exit(6);
		}

		Set<IndexResult.NewAttachment> attachments = Set.of(
			new IndexResult.NewAttachment(Addon.AttachmentType.IMAGE, attfile.getFileName().toString(), attfile)
		);

		if (contentManager.checkin(new IndexResult<>(content, attachments), null)) {
			System.out.println("Stored changes!");
		} else {
			System.out.println("Failed to apply");
		}
	}
}
