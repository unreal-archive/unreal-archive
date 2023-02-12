package net.shrimpworks.unreal.archive.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JSON {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd");
	private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");

	private static final ObjectMapper MAPPER;

	static {
		MAPPER = JsonMapper.builder(new JsonFactory())
						   .addModule(new JavaTimeModule())
						   .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
						   .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
						   .serializationInclusion(JsonInclude.Include.NON_NULL)
						   .addModule(new SimpleModule()
										  .addDeserializer(LocalDateTime.class, new DateTimeDeserializer())
										  .addSerializer(LocalDateTime.class, new DateTimeSerializer())
										  .addDeserializer(LocalDate.class, new DateDeserializer())
										  .addSerializer(LocalDate.class, new DateSerializer())
										  .addDeserializer(Path.class, new PathDeserializer())
										  .addSerializer(Path.class, new PathSerializer())
						   )
						   .build();
	}

	public static byte[] toBytes(Object object) throws IOException {
		return MAPPER.writeValueAsBytes(object);
	}

	public static String toString(Object object) throws IOException {
		return MAPPER.writeValueAsString(object);
	}

	public static <T> T fromFile(Path path, Class<T> type) throws IOException {
		try {
			return MAPPER.readValue(Files.newInputStream(path), type);
		} catch (Exception e) {
			throw new IOException("Failed to read file " + path.toString(), e);
		}
	}

	public static <T> T fromFile(Path path, TypeReference<T> type) throws IOException {
		try {
			return MAPPER.readValue(Files.newInputStream(path), type);
		} catch (Exception e) {
			throw new IOException("Failed to read file " + path.toString(), e);
		}
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
			return LocalDateTime.parse(jsonParser.readValueAs(String.class), DATE_TIME_FORMAT);
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
			return LocalDate.parse(jsonParser.readValueAs(String.class), DATE_FORMAT);
		}
	}

	private static class PathSerializer extends JsonSerializer<Path> {

		@Override
		public void serialize(Path value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
			jgen.writeString(value.toAbsolutePath().toString());
		}
	}

	private static class PathDeserializer extends JsonDeserializer<Path> {

		@Override
		public Path deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
			jsonParser.setCodec(MAPPER);
			return Paths.get(jsonParser.readValueAs(String.class));
		}
	}

}
