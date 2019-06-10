package net.shrimpworks.unreal.archive.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Disabled
public class B2StoreTest {

	@Test
	public void uploadDownload() throws IOException {
		String keyId = System.getenv("B2_ACC");
		String appId = System.getenv("B2_KEY");
		String bucket = System.getenv("B2_BUCKET");

		if (keyId == null || appId == null || bucket == null) {
			fail("This test requires B2 bucket properties set as environment variables");
		}

		Path file = Files.write(Files.createTempFile("upload", ".tmp"), Long.toString(System.nanoTime()).getBytes());
		try (B2Store b2 = new B2Store(keyId, appId, bucket)) {
			b2.store(file, file.getFileName().toString(), s -> {
				try {
					b2.download(s, dl -> {
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
						b2.delete(s, Assertions::assertTrue);
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
