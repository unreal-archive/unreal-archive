package org.unrealarchive.mirror;

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

import org.unrealarchive.common.Util;
import org.unrealarchive.content.ContentEntity;
import org.unrealarchive.content.Download;
import org.unrealarchive.content.addons.Addon;
import org.unrealarchive.content.addons.GameType;
import org.unrealarchive.content.addons.GameTypeRepository;
import org.unrealarchive.content.addons.SimpleAddonRepository;
import org.unrealarchive.content.managed.Managed;
import org.unrealarchive.content.managed.ManagedContentRepository;
import org.unrealarchive.indexing.ContentManager;
import org.unrealarchive.indexing.GameTypeManager;
import org.unrealarchive.indexing.IndexResult;
import org.unrealarchive.indexing.ManagedContentManager;
import org.unrealarchive.storage.DataStore;

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

	public Mirror(SimpleAddonRepository repo, ContentManager cm, GameTypeRepository gametypes, GameTypeManager gm,
				  ManagedContentRepository managed, ManagedContentManager mm,
				  DataStore mirrorStore, int concurrency, LocalDate since, LocalDate until, Progress progress) {
		this.cm = cm;
		this.gm = gm;
		this.mm = mm;
		this.mirrorStore = mirrorStore;

		final LocalDate sinceFilter = since.minusDays(1);
		final LocalDate untilFilter = until.plusDays(1);

		this.content = Stream.concat(
								 repo.all().stream(),
								 Stream.concat(
									 gametypes.all().stream(),
									 managed.all().stream()
								 )
							 )
							 .filter(c -> !c.deleted())
							 .filter(c -> c.addedDate().toLocalDate().isAfter(sinceFilter))
							 .filter(c -> c.addedDate().toLocalDate().isBefore(untilFilter))
							 .collect(Collectors.toCollection(ConcurrentLinkedDeque::new));

		this.retryQueue = new ConcurrentLinkedDeque<>();
		this.concurrency = concurrency;

		this.progress = progress;

		this.totalCount = content.size();

		this.executor = Executors.newFixedThreadPool(concurrency);
	}

	public void cancel() {
		executor.shutdownNow();

		if (mirrorThread != null) {
			mirrorThread.interrupt();
			mirrorThread = null;
		}
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
		if (c != null) executor.submit(new Transfer(c, this.mirrorStore, this));
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

		public Transfer(ContentEntity<?> c, DataStore mirrorStore, Consumer<Transfer> done) {
			this.content = c;
			this.mirrorStore = mirrorStore;
			this.done = done;
		}

		@Override
		public void run() {
			try {
				if (content instanceof Addon) mirrorContent((Addon)content);
				else if (content instanceof GameType) mirrorGameType((GameType)content);
				else if (content instanceof Managed) mirrorManaged((Managed)content);
				else System.out.printf("%nContent mirroring not yet supported for type %s: %s%n",
									   content.getClass().getSimpleName(), content.name());
			} catch (MirrorFailedException t) {
				System.err.printf("%nFailed to transfer content %s: %s (queued for retry)%n", t.filename, t);
				retryQueue.add(t.content);
			} finally {
				done.accept(this);
			}
		}

		private void mirrorManaged(Managed managed) throws MirrorFailedException {
			Managed clone = mm.checkout(managed);
			for (Managed.ManagedFile managedFile : clone.downloads) {
				try {
					Path localFile = Paths.get(managedFile.localFile);
					final boolean hasLocalFile = Files.exists(localFile);
					if (!hasLocalFile) {
						Download dl = managedFile.mainDownload();
						localFile = Util.downloadTo(
							dl.url.replaceAll(" ", "%20"),
							Files.createTempDirectory("ua-mirror").resolve(Util.fileName(managedFile.localFile))
						);
					}

					try {
						boolean[] success = { false };
						mm.storeDownloadFile(clone, managedFile, localFile, success);
						if (!success[0]) {
							throw new MirrorFailedException("Mirror of managed file failed", null, managedFile.originalFilename, clone);
						}
					} finally {
						if (!hasLocalFile) Files.deleteIfExists(localFile);
					}
				} catch (Exception ex) {
					throw new MirrorFailedException(ex.getMessage(), ex, managedFile.originalFilename, clone);
				}
			}
			mm.checkin(clone);
		}

		private void mirrorGameType(GameType gameType) throws MirrorFailedException {
			GameType clone = gm.checkout(gameType);
			for (GameType.Release release : clone.releases) {
				for (GameType.ReleaseFile releaseFile : release.files) {
					try {
						Path localFile = Paths.get(releaseFile.localFile);
						final boolean hasLocalFile = Files.exists(localFile);
						if (!hasLocalFile) {
							Download dl = releaseFile.mainDownload();
							localFile = Util.downloadTo(
								dl.url,
								Files.createTempDirectory("ua-mirror").resolve(releaseFile.originalFilename)
							);
						}

						try {
							boolean[] success = { false };
							gm.syncReleaseFile(clone, releaseFile, localFile, success);
							if (!success[0]) {
								throw new MirrorFailedException("Mirror of gametype failed", null, releaseFile.originalFilename, clone);
							}
						} finally {
							if (!hasLocalFile) Files.deleteIfExists(localFile);
						}
					} catch (Exception ex) {
						throw new MirrorFailedException(ex.getMessage(), ex, releaseFile.originalFilename, clone);
					}
				}
			}
			gm.checkin(clone);
		}

		private void mirrorContent(Addon content) throws MirrorFailedException {
			try {
				// only consider "main" URLs
				Download dl = content.mainDownload();
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
								Addon updated = cm.checkout(content.hash);
								updated.downloads.add(new Download(newUrl, false));
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
