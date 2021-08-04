package net.shrimpworks.unreal.archive.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.minio.MinioClient;
import io.minio.ObjectStat;
import io.minio.PutObjectOptions;
import io.minio.Result;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import io.minio.errors.MinioException;
import io.minio.messages.Item;

import net.shrimpworks.unreal.archive.CLI;
import net.shrimpworks.unreal.archive.Util;

/**
 * Amazon S3 and compatible storage implementation.
 */
public class S3Store implements DataStore {

	public static class Factory implements DataStoreFactory {

		@Override
		public DataStore newStore(StoreContent type, CLI cli) {
			String endpoint = optionOrEnvVar("s3-endpoint", "S3_ENDPOINT", type, cli);
			String keyId = optionOrEnvVar("s3-key", "S3_KEY", type, cli);
			String secret = optionOrEnvVar("s3-secret", "S3_SECRET", type, cli);
			String bucket = optionOrEnvVar("s3-bucket", "S3_BUCKET", type, cli);
			String publicUrl = optionOrEnvVar("s3-url", "S3_URL", type, cli);

			try {
				return new S3Store(endpoint, keyId, secret, bucket, publicUrl);
			} catch (MinioException | IOException e) {
				throw new IllegalArgumentException(e.getMessage(), e);
			}
		}

		private String optionOrEnvVar(String option, String envVar, StoreContent type, CLI cli) {
			String value = cli.option(option + "-" + type.name().toLowerCase(), System.getenv(envVar + "_" + type.name()));
			if (value == null || value.isEmpty()) value = cli.option(option, System.getenv(envVar));
			if (value == null || value.isEmpty()) throw new IllegalArgumentException(
					String.format("Missing S3 store property; --%s or %s", option, envVar)
			);
			return value;
		}
	}

	private final MinioClient client;
	private final String bucket;
	private final String publicUrl;

	public S3Store(String endpointUrl, String accessKey, String secretKey, String bucket, String publicUrl)
			throws InvalidPortException, InvalidEndpointException, IOException {
		this.client = new MinioClient(endpointUrl, accessKey, secretKey);
		this.bucket = bucket;
		this.publicUrl = publicUrl;
	}

	private String makePublicUrl(String bucket, String name) {
		return publicUrl.replaceAll("__BUCKET__", bucket).replaceAll("__NAME__", name);
	}

	@Override
	public void store(Path path, String name, BiConsumer<String, IOException> stored) throws IOException {
		store(Files.newInputStream(path, StandardOpenOption.READ), Files.size(path), name, stored);
	}

	@Override
	public void store(InputStream stream, long dataSize, String name, BiConsumer<String, IOException> stored) throws IOException {
		exists(name, (exits) -> {
			if (exits instanceof ObjectStat) {
				stored.accept(Util.toUriString(makePublicUrl(((ObjectStat)exits).bucketName(), ((ObjectStat)exits).name())), null);
			} else {
				try {
					client.putObject(bucket, name, stream, new PutObjectOptions(dataSize, -1));
					stored.accept(Util.toUriString(makePublicUrl(bucket, name)), null);
				} catch (Exception e) {
					stored.accept(null, new IOException("Failed to store file " + name, e));
				}
			}
		});
	}

	@Override
	public void delete(String url, Consumer<Boolean> deleted) throws IOException {
		try {
			URI uri = URI.create(url);
			String object = uri.getPath();
			if (object.startsWith("/")) object = object.substring(1);
			client.removeObject(bucket, object);
			deleted.accept(true);
		} catch (Exception e) {
			throw new IOException("File download failed", e);
		}
	}

	@Override
	public void download(String url, Consumer<Path> downloaded) throws IOException {
		try {
			URI uri = URI.create(url);
			String object = uri.getPath();
			if (object.startsWith("/")) object = object.substring(1);

			InputStream inputStream = client.getObject(bucket, object);
			Path outFile = Files.createTempFile("download-", Util.fileName(url));
			Files.copy(inputStream, outFile, StandardCopyOption.REPLACE_EXISTING);
			downloaded.accept(outFile);
		} catch (Exception e) {
			throw new IOException("File download failed", e);
		}
	}

	@Override
	public void exists(String name, Consumer<Object> result) throws IOException {
		try {
			result.accept(client.statObject(bucket, name));
		} catch (ErrorResponseException e) {
			if (e.errorResponse().errorCode().code().equalsIgnoreCase("NoSuchKey")) {
				result.accept(null);
			} else {
				throw new IOException(e.getMessage());
			}
		} catch (Exception e) {
			throw new IOException("Failed to check S3 file", e);
		}
	}

	@Override
	public void close() throws IOException {
		// no-op
	}

	@Override
	public String toString() {
		return String.format("S3Store [bucket=%s]", bucket);
	}
}
