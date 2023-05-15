package org.unrealarchive.content.addons;

public enum SimpleAddonType {
	MAP(Map.class),
	MAP_PACK(MapPack.class),
	SKIN(Skin.class),
	MODEL(Model.class),
	VOICE(Voice.class),
	MUTATOR(Mutator.class),
	MOD(UnknownAddon.class),
	UNKNOWN(UnknownAddon.class),
	;

	public final Class<? extends Addon> addonClass;

	SimpleAddonType(Class<? extends Addon> addonClass) {
		this.addonClass = addonClass;
	}

}
