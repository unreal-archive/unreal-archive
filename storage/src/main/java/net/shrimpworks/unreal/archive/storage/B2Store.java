package net.shrimpworks.unreal.archive.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.contentSources.B2FileContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2AccountAuthorization;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2ListBucketsRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.webApiHttpClient.B2StorageHttpClientBuilder;

import net.shrimpworks.unreal.archive.common.CLI;
import net.shrimpworks.unreal.archive.common.Util;

/**
 * Backblaze B2 Cloud storage implementation.
 * <p>
 * This implementation only supports "small" files of up to 5GB.
 */
public class B2Store implements DataStore {

	private static final long MAX_SIZE = 5L * 1000L * 1000L * 1000L; // 5GB (docs say GB, not GiB)
	private static final String USER_AGENT = "unreal-archive/1.0";

	private static final String DOWNLOAD_URL = "%s/file/%s/%s";
	private static final Pattern DOWNLOAD_MATCHER = Pattern.compile(".+?/file/([^/]+)/(.+)");

	public static class Factory implements DataStoreFactory {

		@Override
		public DataStore newStore(StoreContent type, CLI cli) {
			// acc is actually the key id
			String accId = optionOrEnvVar("b2-acc", "B2_ACC", type, cli);
			String key = optionOrEnvVar("b2-key", "B2_KEY", type, cli);
			String bucket = optionOrEnvVar("b2-bucket", "B2_BUCKET", type, cli);

			return new B2Store(accId, key, bucket);
		}

		private String optionOrEnvVar(String option, String envVar, StoreContent type, CLI cli) {
			String value = cli.option(option + "-" + type.name().toLowerCase(), System.getenv(envVar + "_" + type.name()));
			if (value == null || value.isEmpty()) value = cli.option(option, System.getenv(envVar));
			if (value == null || value.isEmpty()) throw new IllegalArgumentException(
					String.format("Missing B2 store property; --%s or %s", option, envVar)
			);
			return value;
		}
	}

	private final B2StorageClient client;
	private final String bucket;

	private volatile B2AccountAuthorization account;
	private volatile B2Bucket bucketInfo;

	B2Store(String keyId, String appKey, String bucket) {
		this.client = B2StorageHttpClientBuilder.builder(keyId, appKey, USER_AGENT).build();
		this.bucket = bucket;
	}

	@Override
	public void close() {
		this.client.close();
	}

	@Override
	public void store(Path path, String name, BiConsumer<String, IOException> stored) throws IOException {
		if (Files.size(path) > MAX_SIZE) throw new IllegalArgumentException(path + " exceeds maximum size " + MAX_SIZE);

		// first, check if file exists; if it does, just return existing file
		exists(name, exists -> {
			if (exists instanceof B2FileVersion) {
				stored.accept(
						Util.toUriString(String.format(DOWNLOAD_URL,
													   account.getDownloadUrl(), bucketInfo.getBucketName(),
													   ((B2FileVersion)exists).getFileName())
						),
						null
				);
			} else {
				try {
					final B2FileVersion upload = this.client.uploadSmallFile(
							B2UploadFileRequest.builder(bucket, name, Util.mimeType(Util.extension(path)),
														B2FileContentSource.build(path.toFile())).build()
					);
					stored.accept(
							Util.toUriString(String.format(DOWNLOAD_URL,
														   account.getDownloadUrl(), bucketInfo.getBucketName(), upload.getFileName())
							),
							null);
				} catch (B2Exception e) {
					stored.accept(null, new IOException("Failed to process Backblaze upload", e));
				}
			}
		});
	}

	@Override
	public void store(InputStream stream, long dataSize, String name, BiConsumer<String, IOException> stored) {
		throw new UnsupportedOperationException("Uploading streams not supported yet");
	}

	@Override
	public void delete(String url, Consumer<Boolean> deleted) throws IOException {
		Matcher m = DOWNLOAD_MATCHER.matcher(url);
		if (!m.matches()) throw new IllegalArgumentException("URL does not seem to be a Backblaze URL");

		try {
			checkAccount();
			B2FileVersion fileInfo = client.getFileInfoByName(bucketInfo.getBucketName(), m.group(2));
			client.deleteFileVersion(m.group(2), fileInfo.getFileId());
			deleted.accept(true);
		} catch (B2Exception e) {
			throw new IOException("Failed to delete Backblaze file", e);
		}
	}

	@Override
	public void download(String url, Consumer<Path> downloaded) throws IOException {
		Matcher m = DOWNLOAD_MATCHER.matcher(url);
		if (!m.matches()) throw new IllegalArgumentException("URL does not seem to be a Backblaze URL");

		try {
			checkAccount();
			client.downloadByName(bucketInfo.getBucketName(), m.group(2), (b2Headers, inputStream) -> {
				Path outFile = Files.createTempFile("download-", b2Headers.getFileNameOrNull());
				Files.copy(inputStream, outFile, StandardCopyOption.REPLACE_EXISTING);
				downloaded.accept(outFile);
			});
		} catch (B2Exception e) {
			throw new IOException("Failed to process Backblaze download", e);
		}
	}

	@Override
	public void exists(String name, Consumer<Object> result) throws IOException {
		try {
			checkAccount();
			try {
				B2FileVersion fileInfo = client.getFileInfoByName(bucketInfo.getBucketName(), name);
				result.accept(fileInfo);
				return;
			} catch (B2Exception ex) {
				if (ex.getStatus() != 404) {
					throw new IOException("File existence check failed", ex);
				}
			}

			result.accept(null);
		} catch (B2Exception e) {
			throw new IOException("Failed to check Backblaze file", e);
		}
	}

	private void checkAccount() throws B2Exception {
		if (this.account == null) this.account = this.client.getAccountAuthorization();
		if (this.bucketInfo == null) {
			for (B2Bucket b : client.listBuckets(B2ListBucketsRequest.builder(account.getAccountId()).build()).getBuckets()) {
				if (b.getBucketId().equals(bucket)) {
					this.bucketInfo = b;
					break;
				}
			}
		}
	}

	@Override
	public String toString() {
		return String.format("B2Store [bucket=%s]", bucket);
	}
}
