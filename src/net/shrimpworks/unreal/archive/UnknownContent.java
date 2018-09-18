package net.shrimpworks.unreal.archive;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({ @JsonSubTypes.Type(value = UnknownContent.class, name = "UNKNOWN") })
public class UnknownContent extends Content {
}
