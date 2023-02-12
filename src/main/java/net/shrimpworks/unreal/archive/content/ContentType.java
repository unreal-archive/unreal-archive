package net.shrimpworks.unreal.archive.content;

import net.shrimpworks.unreal.archive.content.mappacks.MapPack;
import net.shrimpworks.unreal.archive.content.maps.Map;
import net.shrimpworks.unreal.archive.content.models.Model;
import net.shrimpworks.unreal.archive.content.mutators.Mutator;
import net.shrimpworks.unreal.archive.content.skins.Skin;
import net.shrimpworks.unreal.archive.content.voices.Voice;

public enum ContentType {
	MAP(Map.class),
	MAP_PACK(MapPack.class),
	SKIN(Skin.class),
	MODEL(Model.class),
	VOICE(Voice.class),
	MUTATOR(Mutator.class),
	MOD(UnknownContent.class),
	UNKNOWN(UnknownContent.class),
	;

	public final Class<? extends Content> contentClass;

	ContentType(Class<? extends Content> contentClass) {
		this.contentClass = contentClass;
	}

}
