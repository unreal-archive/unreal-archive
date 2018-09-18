package net.shrimpworks.unreal.archive.maps;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonSubTypes;

import net.shrimpworks.unreal.archive.Content;

@JsonSubTypes({ @JsonSubTypes.Type(value = Map.class, name = "MAP") })
public class Map extends Content {

	public String gametype = "Unknown";             // Deathmatch
	public String title = "Unknown";                // My Map
	public String playerCount = "Unknown";          // 2 - 4 Players

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		Map map = (Map)o;
		return Objects.equals(gametype, map.gametype)
			   && Objects.equals(title, map.title)
			   && Objects.equals(playerCount, map.playerCount);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), gametype, title, playerCount);
	}
}
