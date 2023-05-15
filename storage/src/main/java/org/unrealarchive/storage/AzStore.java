package org.unrealarchive.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.StringBuilder;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.unrealarchive.common.CLI;
import org.unrealarchive.common.Util;

/**
 * Azure Cloud Blob storage implementation.
 */
public class AzStore implements DataStore {

	private static final int UPLOAD_CHUNK_SIZE_BYTES = 4 * 1024 * 1024;
	private static final String DEFAULT_ENDPOINT_SUFFIX = "blob.core.windows.net";

	public static class Factory implements DataStoreFactory {

		@Override
		public DataStore newStore(StoreContent type, CLI cli) {
			String accId = optionOrEnvVar("az-acc", "AZ_ACC", type, cli, null);
			String container = optionOrEnvVar("az-container", "AZ_CONTAINER", type, cli, null);
			String endpointSuffix = optionOrEnvVar("az-endpoint", "AZ_ENDPOINT", type, cli, DEFAULT_ENDPOINT_SUFFIX);

			String sharedAccessSignature = optionOrEnvVar("az-sas", "AZ_SAS", type, cli, null);
			sharedAccessSignature = sharedAccessSignature.startsWith("?") ? sharedAccessSignature.substring(1) :
				sharedAccessSignature;
		
			return new AzStore(accId, sharedAccessSignature, container, endpointSuffix);
		}

		private String optionOrEnvVar(String option, String envVar, StoreContent type, CLI cli, String defaultValue) {
			String value = cli.option(option + "-" + type.name().toLowerCase(),
					System.getenv(envVar + "_" + type.name()));

			if (value == null || value.isEmpty())
				value = cli.option(option, System.getenv(envVar));
			if ((value == null || value.isEmpty()) && defaultValue != null)
				value = defaultValue;
			if (value == null || value.isEmpty())
				throw new IllegalArgumentException(
						String.format("Missing AZ store property; --%s or %s", option, envVar));
			return value;
		}
	}

	// The Azure resource name for the storage account
	private final String storageAccount;

	// The container within the storage account to store files in
	private final String container;

	// The endpoint suffix of the storage account - will be default
	// (blob.core.windows.net) unless using a government Azure cloud
	// or sovereign cloud
	private final String endpointsuffix;

	// The shared access signature (SAS) for the container which will
	// give us read+write permissions
	private final String sasstring;

	AzStore(String storageAccount, String sharedAccessSignature, String container, String endpointSuffix) {

		this.container = container;
		this.storageAccount = storageAccount;
		this.endpointsuffix = endpointSuffix;
		this.sasstring = sharedAccessSignature;
	}

	@Override
	public void close() {
		// no-op
	}

	@Override
	public void store(Path path, String name, BiConsumer<String, IOException> stored) throws IOException {
		store(Files.newInputStream(path, StandardOpenOption.READ), Files.size(path), name, stored);
	}

	@Override
	public void store(InputStream stream, long dataSize, String name, BiConsumer<String, IOException> stored)
			throws IOException {
		try {
			exists(name, exists -> {
				if (exists instanceof URL) {
					stored.accept(exists.toString(), null);
				} else {
					try {
						// First, send all the blocks (chunks of the file)
						List<String> sentBlockIds = sendBlocks(name, stream);

						// Finally, commit all the blocks to complete the blob in storage
						commitBlocks(name, sentBlockIds);

						stored.accept(getBlobUrlBase(name).toString(), null);
					} catch (Exception e) {
						stored.accept(null,
								new IOException(String.format("Failed to process AZ upload: %s", e.getMessage()), e));
					}
				}
			});
		} catch (Exception e) {
			stored.accept(null, new IOException(String.format("Failed to process AZ upload: %s", e.getMessage()), e));
		}
	}

	@Override
	public void delete(String url, Consumer<Boolean> deleted) throws IOException {
		// As a safety check before we append the access token and send data,
		// ensure the base of the URL matches what we expect
		if (url.toLowerCase().startsWith(getBlobUrlBase("").toString().toLowerCase())) {
			throw new IllegalArgumentException("URL does not match the given Azure container name or storage account");
		}

		// Append SAS to URL
		url += "?" + this.sasstring;

		int returnCode = 0;

		try {
			HttpURLConnection httpCon = setupBasicConnection(new URL(url), "DELETE");
			httpCon.connect();

			returnCode = httpCon.getResponseCode();
		} catch (Exception e) {
			throw new IOException("Error deleting blob");
		}

		if (returnCode == 202) {
			deleted.accept(true);
			return;
		} else if (returnCode == 404) {
			deleted.accept(false);
			return;
		}

		throw new IOException(String.format("Error deleting blob - Unexpected response: %d", returnCode));
	}

	@Override
	public void download(String url, Consumer<Path> downloaded) throws IOException {
		Path tempFile = Files.createTempFile("dl_", url.substring(0, url.lastIndexOf("/") + 1));
		Util.downloadTo(Util.toUriString(url), tempFile);

		downloaded.accept(tempFile);
	}

	@Override
	public void exists(String name, Consumer<Object> result) throws IOException {
		URL url = getBlobUrl(name, true, null, null); // Use the SAS here to support uploading to a private container
		int returnCode = 0;

		try {
			HttpURLConnection httpCon = setupBasicConnection(url, "HEAD");
			httpCon.connect();

			returnCode = httpCon.getResponseCode();
		} catch (Exception e) {
			throw new IOException("Error checking blob");
		}

		if (returnCode == 200) {
			result.accept(getBlobUrlBase(name));
			return;
		} else if (returnCode == 404) {
			result.accept(false);
			return;
		}

		throw new IOException(String.format("Error checking blob - Unexpected response: %d", returnCode));
	}

	@Override
	public String toString() {
		return String.format("AzStore [container=%s]", this.container);
	}

	// Get the current time in a format Azure understands
	private String getCurrentTime() {
		Calendar calendar = Calendar.getInstance();

		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		return dateFormat.format(calendar.getTime());
	}

	// Get the base Azure storage account URL
	private URL getBlobUrlBase(String name) throws MalformedURLException, UnsupportedEncodingException {
		String requestedName = name.replaceAll("[ ]", "%20").replaceAll("[\\\\]", "/");

		return new URL(String.format("https://%s.%s/%s/%s", this.storageAccount, this.endpointsuffix, this.container,
				requestedName));
	}

	// Get the URL of the blob in Azure storage
	private URL getBlobUrl(String name, Boolean includeSas, String operation, String currentBlockId)
			throws MalformedURLException, UnsupportedEncodingException {
		String url = getBlobUrlBase(name).toString() + "?";

		if (operation != null) {
			url += String.format("&comp=%s", operation);
		}

		if (currentBlockId != null) {
			url += String.format("&blockid=%s", URLEncoder.encode(currentBlockId, StandardCharsets.UTF_8.toString()));
		}

		if (includeSas) {
			url += String.format("&%s", this.sasstring);
		}

		return new URL(url);
	}

	// For a given stream and blob name, read the stream in chunks and send each
	// chunkUrl as a 'block' to Azure storage. Returns a list of all sent block
	// ids for the stream
	private List<String> sendBlocks(String name, InputStream stream) throws IOException {
		long currentBlock = 0;
		int bytesRead = 0;
		List<String> sentBlockIds = new ArrayList<>();

		// Chunk the stream into blocks and send
		do {
			byte[] buffer = new byte[UPLOAD_CHUNK_SIZE_BYTES];
			bytesRead = stream.readNBytes(buffer, 0, UPLOAD_CHUNK_SIZE_BYTES);

			if (bytesRead <= 0) {
				break;
			}

			String currentBlockId = generateBlockId(currentBlock);
			URL chunkUrl = getBlobUrl(name, true, "block", currentBlockId);

			Boolean shouldRetry = false;
			int attempts = 3;

			// If a failure occurs during a single block upload, we can retry it alone. If
			// this succeeds, it will save bandwidth versus attempting the entire blob
			// again. If we reach all attempts, we will throw a hard error to do a full retry.
			do {
				HttpURLConnection httpCon = setupPutConnection(chunkUrl, bytesRead, true);
				httpCon.connect();

				try (OutputStream output = httpCon.getOutputStream()) {
					output.write(buffer, 0, bytesRead);
					output.flush();
				}

				int code = httpCon.getResponseCode();

				// We should retry again if we don't get HTTP 201
				shouldRetry = code != 201;

				if (shouldRetry && --attempts <= 0) {
					throw new IOException(
							String.format("Failed to upload block %s - Unexpected response: %d", currentBlockId, code));
				}
			} while (shouldRetry);

			sentBlockIds.add(currentBlockId);
			currentBlock++;
		} while (bytesRead > 0);

		return sentBlockIds;
	}

	// Generate a temporary reference id for the block to use during finalization
	private String generateBlockId(long currentBlock) {
		return Base64.getEncoder().encodeToString(String.format("%010d", currentBlock).getBytes());
	}

	// Given a list of all written block ids and name of the final blob, commit
	// the blocks to the blob by sending the XML manifest
	private void commitBlocks(String name, List<String> sentBlockIds) throws MalformedURLException, IOException {
		// Finalize the blob by sending a full block id manifest
		URL manifestSendUrl = getBlobUrl(name, true, "blocklist", null);
		byte[] blockManifest = generateBlockIdManifest(sentBlockIds);

		HttpURLConnection httpCon = setupPutConnection(manifestSendUrl, blockManifest.length, false);
		httpCon.connect();

		try (OutputStream output = httpCon.getOutputStream()) {
			output.write(blockManifest);
			output.flush();
		}

		int code = httpCon.getResponseCode();

		if (code != 201) {
			throw new IOException(String.format("Failed to commit blocks - Unexpected response: %d"));
		}
	}

	// Given a list of all written block ids, generate an XML manifest
	private byte[] generateBlockIdManifest(List<String> sentBlockIds) {
		StringBuilder blockManifest = new StringBuilder();

		blockManifest.append("<?xml version=\"1.0\" encoding=\"utf-8\"?><BlockList>");
		sentBlockIds.forEach((blockId) -> {
			blockManifest.append(String.format("<Latest>%s</Latest>", blockId));
		});
		blockManifest.append("</BlockList>");

		return blockManifest.toString().getBytes(StandardCharsets.UTF_8);
	}

	// Setup a request with the basic headers for Azure storage
	private HttpURLConnection setupBasicConnection(URL url, String method) throws IOException, ProtocolException {
		HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
		httpCon.setRequestMethod(method);
		httpCon.setRequestProperty("x-ms-version", "2020-04-08");
		httpCon.setRequestProperty("x-ms-date", this.getCurrentTime());
		httpCon.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(120));
		httpCon.setReadTimeout((int) TimeUnit.SECONDS.toMillis(300));
		return httpCon;
	}

	// Setup an HTTP connection for a PUT request with a known length
	private HttpURLConnection setupPutConnection(URL url, long bytesToSend, Boolean sendingContent)
			throws IOException, ProtocolException {
		HttpURLConnection httpCon = setupBasicConnection(url, "PUT");
		httpCon.setDoOutput(true);

		if (sendingContent) {
			// When sending binary content, use the BlockBlob method for
			// chunking large files
			httpCon.setRequestProperty("x-ms-blob-type", "BlockBlob");
		} else {
			// When sending the manifest and other metadata, we use XML
			httpCon.setRequestProperty("Content-Type", "application/xml");
		}

		httpCon.setRequestProperty("Content-Length", Long.toString(bytesToSend));
		return httpCon;
	}
}
