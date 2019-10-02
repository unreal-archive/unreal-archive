package net.shrimpworks.unreal.archive.mirror;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.ContentManager;

/**
 * Simple multi-threaded mirror/downloader implementation.
 * <p>
 * Logging currently implemented via stderr, with a simple callback
 * to monitor progress (overall file counts, not individual file
 * progress).
 */
public class MirrorClient implements Consumer<MirrorClient.Downloader> {
	private final int retryLimit = 4;
	private Deque<Content> content;
	private Deque<Content> retryQueue;
	private final Path output;
	private final int concurrency;
	private final ExecutorService executor;

	private final Progress progress;

	private final long totalCount;
	private CountDownLatch counter;

	private volatile Thread mirrorThread;

	public MirrorClient(ContentManager content, Path output, int concurrency, Progress progress) {
		this.content = new ConcurrentLinkedDeque<>(content.search(null, null, null, null));
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
			for (int retryCount = 0; retryCount <= retryLimit && content.size() > 0; retryCount++) {
				if (retryCount > 0) {
					System.err.printf("%nA total of %d download(s) failed, retrying (%d/%d)...%n", content.size(), retryCount, retryLimit);
				}

				// initialize counter
				this.counter = new CountDownLatch(content.size());

				// kick off the initial tasks, subsequent tasks will schedule as they complete
				for (int i = 0; i < concurrency; i++) next();

				// wait for all downloads to complete
				counter.await();

				if (retryQueue.size() > 0) {
					content = retryQueue;
					retryQueue = new ConcurrentLinkedDeque<>();
				}
			}

			if (retryQueue.size() > 0) {
				System.err.printf("%nA total of %d download(s) failed, giving up after %d retries.%n", retryLimit);
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
		progress.progress(totalCount, MirrorClient.this.content.size(), downloader.content);

		// finally, countdown
		counter.countDown();

		// kick off next one
		next();
	}

	private void next() {
		final Content c = MirrorClient.this.content.poll();
		if (c != null) executor.submit(new Downloader(c, output, this, this.retryQueue));
	}

	@FunctionalInterface
	public interface Progress {

		public void progress(long total, long remaining, Content last);
	}

	public static class Downloader implements Runnable {

		public final Content content;
		private final Path output;
		private final Consumer<Downloader> done;
		public final Path destination;
		private Deque<Content>retryQueue;

		public Downloader(Content c, Path output, Consumer<Downloader> done) {
			this(c, output, done, null);
		}

		public Downloader(Content c, Path output, Consumer<Downloader> done, Deque<Content>retryQueue) {
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
				Content.Download dl = content.downloads.stream().filter(d -> d.main).findFirst().orElse(null);
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
