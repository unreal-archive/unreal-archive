package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class YAML {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd");
	private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");

	private static final ObjectMapper MAPPER;

	static {
		MAPPER = new ObjectMapper(new YAMLFactory());
		MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

		SimpleModule module = new SimpleModule();

		MAPPER.registerModule(module.addDeserializer(LocalDateTime.class, new DateTimeDeserializer()));
		MAPPER.registerModule(module.addSerializer(LocalDateTime.class, new DateTimeSerializer()));
		MAPPER.registerModule(module.addDeserializer(LocalDate.class, new DateDeserializer()));
		MAPPER.registerModule(module.addSerializer(LocalDate.class, new DateSerializer()));
	}

	public static String toString(Object object) throws IOException {
		return MAPPER.writeValueAsString(object);
	}

	public static <T> T fromFile(Path path, Class<T> type) throws IOException {
		return MAPPER.readValue(MAPPER.readTree(path.toFile()).traverse(), type);
	}

	public static <T> T fromString(String yaml, Class<T> type) throws IOException {
		return MAPPER.readValue(yaml, type);
	}

	private static class DateTimeSerializer extends JsonSerializer<LocalDateTime> {

		@Override
		public void serialize(LocalDateTime value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
			jgen.writeString(value.format(DATE_TIME_FORMAT));
		}
	}

	private static class DateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

		@Override
		public LocalDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
			jsonParser.setCodec(MAPPER);
			JsonNode node = jsonParser.readValueAsTree();
			return LocalDateTime.parse(node.asText(), DATE_TIME_FORMAT);
		}
	}

	private static class DateSerializer extends JsonSerializer<LocalDate> {

		@Override
		public void serialize(LocalDate value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
			jgen.writeString(value.format(DATE_FORMAT));
		}
	}

	private static class DateDeserializer extends JsonDeserializer<LocalDate> {

		@Override
		public LocalDate deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
			jsonParser.setCodec(MAPPER);
			JsonNode node = jsonParser.readValueAsTree();
			return LocalDate.parse(node.asText(), DATE_FORMAT);
		}
	}

}
