package net.shrimpworks.unreal.archive.mirror;

import java.io.IOException;
import java.nio.file.Files;
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
import java.util.stream.Stream;

import net.shrimpworks.unreal.archive.ContentEntity;
import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.ContentManager;
import net.shrimpworks.unreal.archive.content.GameTypeManager;
import net.shrimpworks.unreal.archive.content.IndexResult;
import net.shrimpworks.unreal.archive.content.gametypes.GameType;
import net.shrimpworks.unreal.archive.managed.ManagedContentManager;
import net.shrimpworks.unreal.archive.storage.DataStore;

public class Mirror implements Consumer<Mirror.Transfer> {

	private static final int RETRY_LIMIT = 4;

	private final ContentManager cm;
	private final GameTypeManager gm;
	private final ManagedContentManager mm;
	private final DataStore mirrorStore;

	private Deque<ContentEntity<?>> content;
	private Deque<ContentEntity<?>> retryQueue;
	private final int concurrency;
	private final ExecutorService executor;

	private final Progress progress;

	private final long totalCount;

	private volatile CountDownLatch counter;
	private volatile Thread mirrorThread;

	public Mirror(ContentManager cm, GameTypeManager gm, ManagedContentManager mm,
				  DataStore mirrorStore, int concurrency, LocalDate since, Progress progress) {
		this.cm = cm;
		this.gm = gm;
		this.mm = mm;
		this.mirrorStore = mirrorStore;

		final LocalDate sinceFilter = since.minusDays(1);

		this.content = Stream.concat(
									 cm.search(null, null, null, null).stream(),
									 Stream.concat(
											 gm.all().stream(), mm.all().stream()
									 )
							 )
							 .filter(c -> !c.deleted())
							 .filter(c -> c.addedDate().toLocalDate().isAfter(sinceFilter))
							 .collect(Collectors.toCollection(ConcurrentLinkedDeque::new));

		this.retryQueue = new ConcurrentLinkedDeque<>();
		this.concurrency = concurrency;

		this.progress = progress;

		this.totalCount = content.size();

		this.executor = Executors.newFixedThreadPool(concurrency);
	}

	public boolean mirror() {
		return mirrorContent();
	}

	public void cancel() {
		executor.shutdownNow();

		if (mirrorThread != null) {
			mirrorThread.interrupt();
			mirrorThread = null;
		}
	}

	private boolean mirrorContent() {
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

	@Override
	public void accept(Mirror.Transfer transfer) {
		progress.progress(totalCount, this.content.size(), transfer.content);

		// finally, countdown
		counter.countDown();

		// kick off next one
		next();
	}

	private void next() {
		final ContentEntity<?> c = this.content.poll();
		if (c != null) executor.submit(new Transfer(c, this.mirrorStore, this, this.retryQueue));
	}

	private static class MirrorFailedException extends Exception {

		public final String filename;
		public final ContentEntity<?> content;

		public MirrorFailedException(String message, Throwable cause, String filename, ContentEntity<?> content) {
			super(message, cause);
			this.filename = filename;
			this.content = content;
		}
	}

	protected class Transfer implements Runnable {

		private final ContentEntity<?> content;
		private final DataStore mirrorStore;
		private final Consumer<Transfer> done;
		private final Deque<ContentEntity<?>> retryQueue;

		public Transfer(ContentEntity<?> c, DataStore mirrorStore, Consumer<Transfer> done, Deque<ContentEntity<?>> retryQueue) {
			this.content = c;
			this.mirrorStore = mirrorStore;
			this.retryQueue = retryQueue;
			this.done = done;
		}

		@Override
		public void run() {
			try {
				if (content instanceof Content) mirrorContent((Content)content);
				else if (content instanceof GameType) mirrorGameType((GameType)content);
				else System.out.printf("%nContent mirroring not yet supported for type %s: %s%n",
									   content.getClass().getSimpleName(), content.name());
			} catch (MirrorFailedException t) {
				System.err.printf("%nFailed to transfer content %s: %s (queued for retry)%n", t.filename, t);
				retryQueue.add(t.content);
			} finally {
				done.accept(this);
			}
		}

		private void mirrorGameType(GameType gameType) throws MirrorFailedException {
			for (GameType.Release release : gameType.releases) {
				for (GameType.ReleaseFile releaseFile : release.files) {
					try {
						Content.Download dl = releaseFile.downloads.stream().filter(d -> d.main).findFirst().get();
						Path localFile = Util.downloadTo(
								dl.url,
								Files.createTempDirectory("ua-mirror").resolve(releaseFile.originalFilename)
						);

						try {
							boolean[] success = { false };
							gm.syncReleaseFile(mirrorStore, gameType, releaseFile, localFile, success);
							if (!success[0])
								throw new MirrorFailedException("Mirror of gametype failed", null, releaseFile.originalFilename, gameType);
						} finally {
							Files.deleteIfExists(localFile);
						}
					} catch (Exception ex) {
						throw new MirrorFailedException(ex.getMessage(), ex, releaseFile.originalFilename, gameType);
					}
				}
			}
		}

		private void mirrorContent(Content content) throws MirrorFailedException {
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
												  content.originalFilename, ex);
								retryQueue.add(content);
							}
							if (newUrl != null && content.downloads.stream().noneMatch(d -> d.url.equalsIgnoreCase(newUrl))) {
								Content updated = cm.checkout(content.hash);
								updated.downloads.add(new Content.Download(newUrl, false));
								try {
									cm.checkin(new IndexResult<>(updated, Collections.emptySet()), null);
								} catch (IOException e) {
									System.err.printf("%nFailed to record new download for %s: %s (queued for retry)%n",
													  content.originalFilename, e);
									retryQueue.add(content);
								}
							}
						});
					} catch (IOException e) {
						System.err.printf("%nFailed to transfer content %s: %s (queued for retry)%n",
										  content.originalFilename, e);
						retryQueue.add(content);
					}
				});
			} catch (Throwable t) {
				throw new MirrorFailedException(t.getMessage(), t, content.originalFilename, content);
			}
		}
	}
}
