package net.shrimpworks.unreal.archive.mirror;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.ContentManager;
import net.shrimpworks.unreal.archive.content.IndexResult;
import net.shrimpworks.unreal.archive.storage.DataStore;

public class Mirror implements Consumer<Mirror.Transfer> {

	private static final int RETRY_LIMIT = 4;

	private final ContentManager cm;
	private final DataStore mirrorStore;

	private Deque<Content> content;
	private Deque<Content> retryQueue;
	private final int concurrency;
	private final ExecutorService executor;

	private final Progress progress;

	private final long totalCount;

	private volatile CountDownLatch counter;
	private volatile Thread mirrorThread;

	public Mirror(ContentManager cm, DataStore mirrorStore, int concurrency, LocalDate since, Progress progress) {
		this.cm = cm;
		this.mirrorStore = mirrorStore;

		this.content = cm.search(null, null, null, null).stream()
						 .filter(c -> c.firstIndex.toLocalDate().isAfter(since.minusDays(1)))
						 .collect(Collectors.toCollection(ConcurrentLinkedDeque::new));
		this.retryQueue = new ConcurrentLinkedDeque<>();
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
					System.err.printf("%n%d mirror operations failed, retrying (%d/%d)...%n", content.size(), retryCount, RETRY_LIMIT);
				}

				// initialize counter
				this.counter = new CountDownLatch(content.size());

				// kick off the initial tasks, subsequent tasks will schedule as they complete
				for (int i = 0; i < concurrency; i++) next();

				// wait for all transfers to complete
				counter.await();

				// after everything is done, try to redo any failures
				if (!retryQueue.isEmpty()) {
					content = retryQueue;
					retryQueue = new ConcurrentLinkedDeque<>();
				}
			}

			if (!retryQueue.isEmpty()) {
				System.err.printf("%nA total of %d mirror operations failed, giving up after %d retries.%n",
								  retryQueue.size(), RETRY_LIMIT);
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
	public void accept(Mirror.Transfer transfer) {
		progress.progress(totalCount, this.content.size(), transfer.content);

		// finally, countdown
		counter.countDown();

		// kick off next one
		next();
	}

	private void next() {
		final Content c = this.content.poll();
		if (c != null) executor.submit(new Transfer(cm, c, this.mirrorStore, this, this.retryQueue));
	}

	protected static class Transfer implements Runnable {

		private final ContentManager cm;
		private final Content content;
		private final DataStore mirrorStore;
		private final Consumer<Transfer> done;
		private final Deque<Content> retryQueue;

		public Transfer(ContentManager cm, Content c, DataStore mirrorStore, Consumer<Transfer> done) {
			this(cm, c, mirrorStore, done, null);
		}

		public Transfer(ContentManager cm, Content c, DataStore mirrorStore, Consumer<Transfer> done, Deque<Content> retryQueue) {
			this.cm = cm;
			this.content = c;
			this.mirrorStore = mirrorStore;
			this.retryQueue = retryQueue;
			this.done = done;
		}

		@Override
		public void run() {
			try {
				// only consider "main" URLs
				Content.Download dl = content.mainDownload();
				if (dl == null) return;

				Util.urlRequest(dl.url, (httpConn) -> {
					try {
						Path base = Paths.get("");
						Path uploadPath = content.contentPath(base);
						String uploadName = base.relativize(uploadPath.resolve(Util.fileName(dl.url))).toString();
						long length = httpConn.getContentLength() > -1 ? httpConn.getContentLength() : content.fileSize;
						mirrorStore.store(httpConn.getInputStream(), length, uploadName, (newUrl, ex) -> {
							if (ex != null) {
								System.err.printf("%nFailed to transfer content %s: %s (queued for retry)%n",
												  content.originalFilename, ex.toString());
								if (retryQueue != null) retryQueue.add(content);
							}
							if (newUrl != null && content.downloads.stream().noneMatch(d -> d.url.equalsIgnoreCase(newUrl))) {
								Content updated = cm.checkout(content.hash);
								updated.downloads.add(new Content.Download(newUrl, false));
								try {
									cm.checkin(new IndexResult<>(updated, Collections.emptySet()), null);
								} catch (IOException e) {
									System.err.printf("%nFailed to record new download for %s: %s (queued for retry)%n",
													  content.originalFilename, e.toString());
									if (retryQueue != null) retryQueue.add(content);
								}
							}
						});
					} catch (IOException e) {
						System.err.printf("%nFailed to transfer content %s: %s (queued for retry)%n",
										  content.originalFilename, e.toString());
						if (retryQueue != null) retryQueue.add(content);
					}
				});
			} catch (Throwable t) {
				System.err.printf("%nFailed to transfer content %s: %s (queued for retry)%n", content.originalFilename, t.toString());
				if (retryQueue != null) retryQueue.add(content);
			} finally {
				if (done != null) done.accept(this);
			}
		}
	}
}
