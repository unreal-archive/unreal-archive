package org.unrealarchive.mirror;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unrealarchive.common.Reflect;
import org.unrealarchive.common.Util;
import org.unrealarchive.content.Download;
import org.unrealarchive.content.addons.Addon;

/**
 * Simple multi-threaded mirror/downloader implementation.
 * <p>
 * Logging currently implemented via stderr, with a simple callback
 * to monitor progress (overall file counts, not individual file
 * progress).
 */
public class LocalMirrorClient implements Consumer<LocalMirrorClient.Downloader> {

	private static final int RETRY_LIMIT = 4;
	private final int concurrency;
	private final ExecutorService executor;

	private final Progress progress;

	private Deque<Addon> mirrorQueue;
	private Deque<Addon> retryQueue;
	private Path output;
	private long totalCount;
	private volatile CountDownLatch counter;
	private volatile Thread mirrorThread;

	public LocalMirrorClient(int concurrency, Progress progress) {
		this.concurrency = concurrency;

		this.progress = progress;

		this.executor = Executors.newFixedThreadPool(concurrency);
	}

	public synchronized boolean mirror(Collection<Addon> mirrorContent, Path output) {
		this.mirrorThread = Thread.currentThread();
		this.mirrorQueue = new ConcurrentLinkedDeque<>(mirrorContent);
		this.retryQueue = new ConcurrentLinkedDeque<>();
		this.output = output;
		this.totalCount = mirrorContent.size();

		// limit number of retry cycles
		try {
			for (int retryCount = 0; retryCount <= RETRY_LIMIT && mirrorQueue.size() > 0; retryCount++) {
				if (retryCount > 0) {
					System.err.printf("%nA total of %d download(s) failed, retrying (%d/%d)...%n", mirrorQueue.size(), retryCount,
									  RETRY_LIMIT);
				}

				// initialize counter
				this.counter = new CountDownLatch(mirrorQueue.size());

				// kick off the initial tasks, subsequent tasks will schedule as they complete
				for (int i = 0; i < concurrency; i++) next();

				// wait for all downloads to complete
				counter.await();

				if (!retryQueue.isEmpty()) {
					mirrorQueue = retryQueue;
					retryQueue = new ConcurrentLinkedDeque<>();
				}
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
		progress.progress(totalCount, this.mirrorQueue.size(), downloader.content);

		// finally, countdown
		counter.countDown();

		// kick off next one
		next();
	}

	private void next() {
		final Addon c = this.mirrorQueue.poll();
		if (c != null) executor.submit(new Downloader(c, outputPath(c, output), this, this.retryQueue));
	}

	protected Path outputPath(Addon c, Path output) {
		String outPath = output.toString();

		final Pattern p = Pattern.compile("\\{([A-Za-z]+)}");
		Matcher m = p.matcher(outPath);

		// it seems to be just a normal path with no template strings
		if (!m.find()) return c.contentPath(output);

		Map<String, Field> fields = Reflect.classLowercaseFields(c);

		// rewind matcher
		m.reset();

		while (m.find()) {
			String strVal = "unknown";
			try {
				Field field = fields.get(m.group(1).toLowerCase());
				if (field != null) {
					Object val = field.get(c);
					if (val != null) {
						strVal = val.toString();
						if (strVal == null) strVal = "unknown";
					}
				}
			} catch (IllegalAccessException | ClassCastException e) {
				strVal = "unknown";
			}

			outPath = outPath.replace("{" + m.group(1) + "}", Util.safeFileName(strVal));
		}

		return output.resolve(outPath);
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
				this.destination = Files.createDirectories(output).resolve(content.originalFilename);
			} catch (IOException e) {
				throw new RuntimeException("Failed to create directories for content " + content.originalFilename);
			}
		}

		@Override
		public void run() {
			try {
				Download dl = content.directDownload();
				if (dl == null) return;

				// file already downloaded
				if (Files.exists(destination) && Files.size(destination) == content.fileSize) return;

				// download the stuff, hopefully
				Util.downloadTo(dl.url, destination);
			} catch (Throwable t) {
				if (retryQueue != null) {
					System.err.printf("%nFailed to download content %s: %s (queued for retry)%n", output, t);
					retryQueue.add(content);
				}
			} finally {
				if (done != null) done.accept(this);
			}
		}
	}
}
