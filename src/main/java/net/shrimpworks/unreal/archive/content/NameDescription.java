package net.shrimpworks.unreal.archive.content;

import java.beans.ConstructorProperties;
import java.util.Objects;

/**
 * Small class used for containing elements with a name and description.
 */
public class NameDescription {

	public final String name;
	public final String description;

	@ConstructorProperties({ "name", "description" })
	public NameDescription(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public NameDescription(String source) {
		if (source.contains(",")) {
			name = source.substring(0, source.indexOf(","));
			description = source.substring(source.indexOf(",") + 1);
		} else {
			name = source;
			description = "";
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof NameDescription)) return false;
		NameDescription that = (NameDescription)o;
		return Objects.equals(name, that.name) &&
			   Objects.equals(description, that.description);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, description);
	}
}
