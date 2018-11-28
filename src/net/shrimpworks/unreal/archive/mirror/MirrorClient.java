package net.shrimpworks.unreal.archive.mirror;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Request;

import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.indexer.Content;
import net.shrimpworks.unreal.archive.indexer.ContentManager;

/**
 * Simple multi-threaded mirror/downloader implementation.
 * <p>
 * Logging currently implemented via stderr, with a simple callback
 * to monitor progress (overall file counts, not individual file
 * progress).
 */
public class MirrorClient {

	private final Deque<Content> content; // TODO this isn't really re-usable; a single call to mirror() renders the class a bit useless
	private final long totalCount;
	private final Path output;
	private final int concurrency;
	private final ExecutorService executor;

	private volatile Thread mirrorThread;

	public MirrorClient(ContentManager content, Path output, int concurrency) {
		this.content = new ArrayDeque<>(content.search(null, null, null, null));
		this.totalCount = content.size();
		this.output = output;
		this.concurrency = concurrency;
		this.executor = Executors.newFixedThreadPool(concurrency);
	}

	public boolean mirror(Progress progress) {
		this.mirrorThread = Thread.currentThread();

		try {
			final CountDownLatch counter = new CountDownLatch(content.size());

			// kick off the initial tasks, subsequent tasks will schedule as they complete
			for (int i = 0; i < concurrency; i++) next(counter, progress);

			// wait for all downloads to complete
			counter.await();

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

	private void next(CountDownLatch counter, Progress progress) {
		final Content c = MirrorClient.this.content.poll();
		if (c != null) executor.submit(new Downloader(c, counter, progress));
	}

	@FunctionalInterface
	public interface Progress {

		public void progress(long total, long remaining, String last);
	}

	private class Downloader implements Runnable {

		private final Content content;
		private final CountDownLatch counter;
		private final Progress progress;

		public Downloader(Content c, CountDownLatch counter, Progress progress) {
			this.content = c;
			this.counter = counter;
			this.progress = progress;
		}

		@Override
		public void run() {
			try {
				// only consider "main" URLs
				// TODO if main 404s, try others
				Content.Download dl = content.downloads.stream().filter(d -> d.main).findFirst().orElse(null);
				if (dl == null) return;

				// set up output path and file
				final Path dest = Files.createDirectories(content.contentPath(output)).resolve(content.originalFilename);

				// file already downloaded
				if (Files.exists(dest) && Files.size(dest) == content.fileSize) return;

				// download the stuff, hopefully
				Request.Get(Util.toUriString(dl.url)).execute().saveContent(dest.toFile());

			} catch (HttpResponseException e) {
				// note: e.message is always empty, thanks apache :thumbsup:
				System.err.printf("%nFailed to download content %s: HTTP %s%n", content.contentPath(output), e.getStatusCode());
			} catch (Throwable t) {
				System.err.printf("%nFailed to download content %s: %s%n", content.contentPath(output), t.toString());
			} finally {
				// attempt to submit next download if available
				next(counter, progress);

				if (progress != null) progress.progress(totalCount, MirrorClient.this.content.size(), content.originalFilename);

				// finally, countdown
				counter.countDown();
			}
		}
	}
}
