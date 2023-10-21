package org.unrealarchive.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.minio.StatObjectResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class S3StoreTest {

	@Test
	public void uploadDownload() throws IOException {
		String endpoint = System.getenv("S3_ENDPOINT");
		String key = System.getenv("S3_KEY");
		String secret = System.getenv("S3_SECRET");
		String bucket = System.getenv("S3_BUCKET");
		String publicUrl = System.getenv("S3_URL");

		if (endpoint == null || key == null || secret == null || bucket == null || publicUrl == null) {
			fail("This test requires S3 bucket properties set as environment variables");
		}

		Path file = Files.write(Files.createTempFile("upload", ".tmp"), Long.toString(System.nanoTime()).getBytes());
		try (S3Store s3 = new S3Store(endpoint, key, secret, bucket, publicUrl)) {
			s3.store(file, file.getFileName().toString(), (s, ex) -> {
				try {
					s3.exists(file.getFileName().toString(), r -> {
						if (r instanceof StatObjectResponse res) {
							assertTrue(res.size() > 0);
						} else {
							fail("an exists check result was expected");
						}
					});
					s3.download(file.getFileName().toString(), dl -> {
						try {
							assertEquals(Files.readString(file), Files.readString(dl));
						} catch (IOException e) {
							fail(e.toString());
						} finally {
							try {
								Files.deleteIfExists(dl);
							} catch (IOException e) {
								// holy bleeping shit
							}
						}
					});
				} catch (IOException e) {
					fail(e.toString());
				} finally {
					try {
						s3.delete(s, Assertions::assertTrue);
					} catch (IOException e) {
						fail(e.getMessage());
					}
				}
			});
		} finally {
			Files.deleteIfExists(file);
		}
	}
}
