package net.shrimpworks.unreal.archive.maps;

import com.fasterxml.jackson.annotation.JsonSubTypes;

import net.shrimpworks.unreal.archive.Content;

@JsonSubTypes({ @JsonSubTypes.Type(value = Map.class, name = "MAP") })
public class Map extends Content {

	public String gametype = "Unknown";             // Deathmatch
	public String title = "Unknown";                // My Map
	public String playerCount = "Unknown";          // 2 - 4 Players

}
