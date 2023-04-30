package net.shrimpworks.unreal.archive.content;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import net.shrimpworks.unreal.archive.common.Util;

public enum FileType {
	CODE(true, "u"),
	MAP(true, "unr", "ut2", "ut3", "un2", "run"),
	PACKAGE(true, "upk"), /* UT3 catch-all format */
	TEXTURE(true, "utx"),
	MUSIC(true, "umx", "ogg"),
	SOUNDS(true, "uax"),
	ANIMATION(true, "ukx"),
	STATICMESH(true, "usx", "usm"), /* usm added to Unreal 227g */
	PREFAB(true, "upx"),
	PHYSICS(true, "ka"),
	PLAYER(true, "upl"),
	INT(false, "int"),
	INI(false, "ini"),
	UCL(false, "ucl"),
	UMOD(true, "umod", "ut2mod", "ut4mod", "rmod"),
	TEXT(false, "txt"),
	HTML(false, "html", "htm"),
	IMAGE(false, "jpg", "jpeg", "bmp", "png", "gif"),
	;

	public static final FileType[] PACKAGES = { CODE, MAP, TEXTURE, SOUNDS, ANIMATION, STATICMESH, PACKAGE, MUSIC };

	public static final FileType[] ALL = FileType.values();

	public final boolean important;
	public final Collection<String> ext;

	FileType(boolean important, String... ext) {
		this.important = important;
		this.ext = Collections.unmodifiableCollection(Arrays.asList(ext));
	}

	public boolean matches(String path) {
		return (ext.contains(Util.extension(path).toLowerCase()));
	}

	public static boolean important(Path path) {
		return important(path.toString());
	}

	public static boolean important(String path) {
		for (FileType type : values()) {
			if (type.important && type.ext.contains(Util.extension(path).toLowerCase())) return true;
		}
		return false;
	}

	public static FileType forFile(String path) {
		for (FileType type : values()) {
			if (type.ext.contains(Util.extension(path).toLowerCase())) return type;
		}
		return null;
	}
}
