package org.unrealarchive.indexing;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.unrealarchive.common.YAML;
import org.unrealarchive.content.FileType;
import org.unrealarchive.content.addons.Addon;

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

		Path yaml = Files.writeString(Files.createTempFile(content.hash, ".yml"), YAML.toString(content),
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

	public void set(String hash, String attribute, String... newValue) throws ReflectiveOperationException, IOException {
		if (attribute.equalsIgnoreCase("attach")) {
			attach(hash, newValue);
			return;
		}

		Addon content = checkoutContent(hash);

		ContentEditor.applyAttribute(content, attribute, newValue);

		if (contentManager.checkin(new IndexResult<>(content, Collections.emptySet()), null)) {
			System.out.println("Stored changes!");
		} else {
			System.out.println("Failed to apply");
		}
	}

	public static void applyAttribute(Object content, String attribute, String... newValue) throws ReflectiveOperationException {
		Field field = content.getClass().getField(attribute);
		Object old = field.get(content);

		System.out.printf("Setting field %s from value %s to %s%n", field.getName(), old == null ? "<null>" : old,
						  Arrays.toString(newValue));

		String firstVal = newValue == null || newValue.length == 0 ? null : newValue[0];

		if (firstVal == null || firstVal.equalsIgnoreCase("null")) {
			field.set(content, null);
		} else if (field.getType().equals(boolean.class) || field.getType().equals(Boolean.class)) {
			field.setBoolean(content, Boolean.parseBoolean(firstVal));
		} else if (field.getType().equals(long.class) || field.getType().equals(Long.class)) {
			field.set(content, Long.parseLong(firstVal));
		} else if (field.getType().equals(int.class) || field.getType().equals(Integer.class)) {
			field.set(content, Integer.parseInt(firstVal));
		} else if (field.getType().equals(short.class) || field.getType().equals(Short.class)) {
			field.set(content, Short.parseShort(firstVal));
		} else if (field.getType().equals(double.class) || field.getType().equals(Double.class)) {
			field.set(content, Double.parseDouble(firstVal));
		} else if (field.getType().equals(String.class)) {
			field.set(content, firstVal);
		} else if (isStringCollection(field) && old != null) {
			Method m = Arrays.stream(old.getClass().getMethods()).filter(a -> a.getName().equals("add")).findFirst().orElse(null);
			if (m != null) m.invoke(old, firstVal.trim());
		} else if (isStringMap(field) && old != null && newValue.length == 2) {
			Method m = Arrays.stream(old.getClass().getMethods()).filter(a -> a.getName().equals("put")).findFirst().orElse(null);
			if (m != null) m.invoke(old, newValue[0].trim(), newValue[1].trim());
		} else if (field.getType().isEnum()) {
			Method m = field.getType().getMethod("valueOf", String.class);
			field.set(content, m.invoke(field, firstVal.trim()));
		}
	}

	public static boolean isStringCollection(Field field) {
		return Collection.class.isAssignableFrom(field.getType())
			   && (field.getGenericType() instanceof ParameterizedType pType
				   && pType.getActualTypeArguments()[0].equals(String.class));
	}

	public static boolean isStringMap(Field field) {
		return Map.class.isAssignableFrom(field.getType())
			   && (field.getGenericType() instanceof ParameterizedType pType
				   && pType.getActualTypeArguments()[0].equals(String.class)
				   && pType.getActualTypeArguments()[1].equals(String.class));
	}

	public void attach(String hash, String... attachment) throws IOException {
		Addon content = checkoutContent(hash);

		Set<IndexResult.NewAttachment> attachments = Arrays.stream(attachment).map(attach -> {
			Path attfile = Paths.get(attach);

			if (!Files.exists(attfile)) {
				System.err.printf("Attachment file \"%s\" does not exist!%n", attach);
				System.exit(5);
			}

			if (!FileType.IMAGE.matches(attach)) {
				System.err.printf("Attachment file \"%s\" is not an image!%n", attach);
				System.exit(6);
			}

			return new IndexResult.NewAttachment(Addon.AttachmentType.IMAGE, attfile.getFileName().toString(), attfile);
		}).collect(Collectors.toSet());

		if (contentManager.checkin(new IndexResult<>(content, attachments), null)) {
			System.out.println("Stored changes!");
		} else {
			System.out.println("Failed to apply");
		}
	}
}
