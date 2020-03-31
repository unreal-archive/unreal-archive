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
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import io.minio.errors.MinioException;

import net.shrimpworks.unreal.archive.CLI;
import net.shrimpworks.unreal.archive.Util;

public class S3Store implements DataStore {

	public static class Factory implements DataStoreFactory {

		@Override
		public DataStore newStore(StoreContent type, CLI cli) {
			String endpoint = cli.option("s3-endpoint-" + type.name().toLowerCase(), System.getenv("S3_ENDPOINT_" + type.name()));
			if (endpoint == null || endpoint.isEmpty()) endpoint = cli.option("s3-endpoint", System.getenv("S3_ENDPOINT"));
			if (endpoint == null || endpoint.isEmpty()) throw new IllegalArgumentException(
					"Missing endpoint for S3 store; --s3-endpoint or S3_ENDPOINT"
			);

			String keyId = cli.option("s3-key-id-" + type.name().toLowerCase(), System.getenv("S3_KEY_ID_" + type.name()));
			if (keyId == null || keyId.isEmpty()) keyId = cli.option("s3-key", System.getenv("S3_KEY"));
			if (keyId == null || keyId.isEmpty()) throw new IllegalArgumentException(
					"Missing access key ID for S3 store; --s3-key or S3_KEY"
			);

			String secret = cli.option("s3-secret-" + type.name().toLowerCase(), System.getenv("S3_SECRET_" + type.name()));
			if (secret == null || secret.isEmpty()) secret = cli.option("s3-secret", System.getenv("S3_SECRET"));
			if (secret == null || secret.isEmpty()) throw new IllegalArgumentException(
					"Missing secret key for S3 store; --s3-secret or S3_SECRET"
			);

			String bucket = cli.option("s3-bucket-" + type.name().toLowerCase(), System.getenv("S3_BUCKET_" + type.name()));
			if (bucket == null || bucket.isEmpty()) bucket = cli.option("s3-bucket", System.getenv("S3_BUCKET"));
			if (bucket == null || bucket.isEmpty()) {
				throw new IllegalArgumentException("Missing bucket for S3 store; --s3-bucket or S3_BUCKET");
			}

			String publicUrl = cli.option("s3-url-" + type.name().toLowerCase(), System.getenv("S3_URL_" + type.name()));
			if (publicUrl == null || publicUrl.isEmpty()) publicUrl = cli.option("s3-url", System.getenv("S3_URL"));
			if (publicUrl == null || publicUrl.isEmpty()) throw new IllegalArgumentException(
					"Missing public URL for S3 store; --s3-url or S3_URL"
			);

			try {
				return new S3Store(endpoint, keyId, secret, bucket, publicUrl);
			} catch (MinioException e) {
				throw new IllegalArgumentException(e.getMessage(), e);
			}
		}
	}

	private final MinioClient client;
	private final String bucket;
	private final String publicUrl;

	public S3Store(String endpointUrl, String accessKey, String secretKey, String bucket, String publicUrl)
			throws InvalidPortException, InvalidEndpointException {
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
			}
			try {
				client.putObject(bucket, name, stream, new PutObjectOptions(dataSize, -1));
				stored.accept(Util.toUriString(makePublicUrl(bucket, name)), null);
			} catch (Exception e) {
				stored.accept(null, new IOException("Failed to store file " + name, e));
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
