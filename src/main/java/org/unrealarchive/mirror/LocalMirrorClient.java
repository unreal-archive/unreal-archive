package org.unrealarchive.mirror;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.unrealarchive.common.Util;
import org.unrealarchive.content.Download;
import org.unrealarchive.content.addons.Addon;
import org.unrealarchive.content.addons.SimpleAddonRepository;

/**
 * Simple multi-threaded mirror/downloader implementation.
 * <p>
 * Logging currently implemented via stderr, with a simple callback
 * to monitor progress (overall file counts, not individual file
 * progress).
 */
public class LocalMirrorClient implements Consumer<LocalMirrorClient.Downloader> {

	private static final int RETRY_LIMIT = 4;
	private Deque<Addon> content;
	private Deque<Addon> retryQueue;
	private final Path output;
	private final int concurrency;
	private final ExecutorService executor;

	private final Progress progress;

	private final long totalCount;

	private volatile CountDownLatch counter;
	private volatile Thread mirrorThread;

	public LocalMirrorClient(SimpleAddonRepository content, Path output, int concurrency, Progress progress) {
		this.content = new ConcurrentLinkedDeque<>(content.all());
		this.retryQueue = new ConcurrentLinkedDeque<>();
		this.output = output;
		this.concurrency = concurrency;

		this.progress = progress;

		this.totalCount = content.size();

		this.executor = Executors.newFixedThreadPool(concurrency);
	}

	public boolean mirror() {
		this.mirrorThread = Thread.currentThread();

		// limit number of retry cycles
		try {
			for (int retryCount = 0; retryCount <= RETRY_LIMIT && content.size() > 0; retryCount++) {
				if (retryCount > 0) {
					System.err.printf("%nA total of %d download(s) failed, retrying (%d/%d)...%n", content.size(), retryCount, RETRY_LIMIT);
				}

				// initialize counter
				this.counter = new CountDownLatch(content.size());

				// kick off the initial tasks, subsequent tasks will schedule as they complete
				for (int i = 0; i < concurrency; i++) next();

				// wait for all downloads to complete
				counter.await();

				if (!retryQueue.isEmpty()) {
					content = retryQueue;
					retryQueue = new ConcurrentLinkedDeque<>();
				}
			}

			if (!retryQueue.isEmpty()) {
				System.err.printf("%nA total of %d download(s) failed, giving up after %d retries.%n", retryQueue.size(), RETRY_LIMIT);
			}

			return true;
		} catch (InterruptedException e) {
			return false;
		} finally {
			this.mirrorThread = null;
		}
	}

	public void cancel() {
		executor.shutdownNow();

		if (mirrorThread != null) {
			mirrorThread.interrupt();
			mirrorThread = null;
		}
	}

	@Override
	public void accept(Downloader downloader) {
		progress.progress(totalCount, this.content.size(), downloader.content);

		// finally, countdown
		counter.countDown();

		// kick off next one
		next();
	}

	private void next() {
		final Addon c = this.content.poll();
		if (c != null) executor.submit(new Downloader(c, output, this, this.retryQueue));
	}

	public static class Downloader implements Runnable {

		public final Addon content;
		public final Path destination;
		private final Path output;
		private final Consumer<Downloader> done;
		private final Deque<Addon> retryQueue;

		public Downloader(Addon c, Path output, Consumer<Downloader> done) {
			this(c, output, done, null);
		}

		public Downloader(Addon c, Path output, Consumer<Downloader> done, Deque<Addon> retryQueue) {
			this.retryQueue = retryQueue;
			this.content = c;
			this.output = output;
			this.done = done;

			try {
				this.destination = Files.createDirectories(content.contentPath(output)).resolve(content.originalFilename);
			} catch (IOException e) {
				throw new RuntimeException("Failed to create directories for content " + content.originalFilename);
			}
		}

		@Override
		public void run() {
			try {
				// only consider "main" URLs
				// TODO if main 404s, try others
				Download dl = content.mainDownload();
				if (dl == null) return;

				// file already downloaded
				if (Files.exists(destination) && Files.size(destination) == content.fileSize) return;

				// download the stuff, hopefully
				Util.downloadTo(dl.url, destination);
			} catch (Throwable t) {
				System.err.printf("%nFailed to download content %s: %s (queued for retry)%n", content.contentPath(output), t.toString());
				if (retryQueue != null) {
					retryQueue.add(content);
				}
			} finally {
				if (done != null) done.accept(this);
			}
		}
	}
}
