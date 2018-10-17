package net.shrimpworks.unreal.archive.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

import net.shrimpworks.unreal.archive.CLI;
import net.shrimpworks.unreal.archive.Util;

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
			String accId = cli.option("b2-acc-" + type.name().toLowerCase(), System.getenv("B2_ACC_" + type.name()));
			if (accId == null || accId.isEmpty()) accId = cli.option("b2-acc", System.getenv("B2_ACC"));
			if (accId == null || accId.isEmpty()) throw new IllegalArgumentException("Missing Account ID for B2 store; --b2-acc or B2_ACC");

			String key = cli.option("b2-key-" + type.name().toLowerCase(), System.getenv("B2_KEY_" + type.name()));
			if (key == null || key.isEmpty()) key = cli.option("b2-key", System.getenv("B2_KEY"));
			if (key == null || key.isEmpty()) throw new IllegalArgumentException("Missing App Key for B2 store; --b2-key or B2_KEY");

			String bucket = cli.option("b2-bucket-" + type.name().toLowerCase(), System.getenv("B2_BUCKET_" + type.name()));
			if (bucket == null || bucket.isEmpty()) bucket = cli.option("b2-bucket", System.getenv("B2_BUCKET"));
			if (bucket == null || bucket.isEmpty()) {
				throw new IllegalArgumentException("Missing bucket for B2 store; --b2-bucket or B2_BUCKET");
			}

			return new B2Store(accId, key, bucket);
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
	public void store(Path path, String name, Consumer<String> stored) throws IOException {
		if (Files.size(path) > MAX_SIZE) throw new IllegalArgumentException(path + " exceeds maximum size " + MAX_SIZE);

		try {
			checkAccount();

			// first, check if file exists; if it does, just return existing file
			try {
				B2FileVersion fileInfo = client.getFileInfoByName(bucketInfo.getBucketName(), name);
				stored.accept(String.format(DOWNLOAD_URL, account.getDownloadUrl(), bucketInfo.getBucketName(), fileInfo.getFileName()));
				return;
			} catch (B2Exception ex) {
				if (ex.getStatus() != 404) {
					throw new IOException("File existence check failed", ex);
				}
			}

			final B2FileVersion upload = this.client.uploadSmallFile(
					B2UploadFileRequest.builder(bucket, name, Util.mimeType(Util.extension(path)),
												B2FileContentSource.build(path.toFile())).build()
			);

			stored.accept(String.format(DOWNLOAD_URL, account.getDownloadUrl(), bucketInfo.getBucketName(), upload.getFileName()));
		} catch (B2Exception e) {
			throw new IOException("Failed to process Backblaze upload", e);
		}
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
			throw new IOException("Failed to process Backblaze download", e);
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
}
