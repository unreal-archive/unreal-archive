package org.unrealarchive.indexing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.unrealarchive.common.YAML;
import org.unrealarchive.content.Download;
import org.unrealarchive.content.addons.Addon;
import org.unrealarchive.content.addons.SimpleAddonRepository;
import org.unrealarchive.storage.DataStore;

public class ContentManager {

	private final SimpleAddonRepository repo;

	private final DataStore contentStore;
	private final DataStore imageStore;

	private final Set<String> changes;

	public ContentManager(SimpleAddonRepository repo, DataStore contentStore, DataStore imageStore) {
		this.repo = repo;

		this.contentStore = contentStore;
		this.imageStore = imageStore;

		this.changes = new HashSet<>();
	}

	public SimpleAddonRepository repo() {
		return repo;
	}

	/*
	 intent: when some content is going to be worked on, a clone is checked out.
	 when its checked out, its hash (immutable) is stored in the out collection.
	 after its been modified or left alone, the clone is checked in.
	 during check-in, if the the clone is no longer equal to the original, something changed.
	 if something changed, the content will be written out, within a new directory structure if needed
	 and the old file will be removed
	 */
	public Addon checkout(String hash) {
		Addon out = repo.forHash(hash);
		if (out != null) {
			try {
				return YAML.fromString(YAML.toString(out), Addon.class);
			} catch (IOException e) {
				throw new IllegalStateException("Cannot clone content " + out);
			}
		}
		return null;
	}

	public boolean checkin(IndexResult<? extends Addon> indexed, Submission submission) throws IOException {
		Addon current = repo.forHash(indexed.content.hash);

		if (current == null || (!indexed.content.equals(current) || !indexed.files.isEmpty())) {
			// lets store the content \o/
			Path next = indexed.content.contentPath(repo.path());

			for (IndexResult.NewAttachment file : indexed.files) {
				// use same path structure as per contentPath
				try {
					String uploadPath = repo.path().relativize(next.resolve(file.name)).toString();
					if (file.type == Addon.AttachmentType.IMAGE) {
						imageStore.store(file.path, uploadPath, (fileUrl, ex) ->
							indexed.content.attachments.add(new Addon.Attachment(file.type, file.name, fileUrl)));
					}
				} finally {
					// cleanup file once uploaded
					Files.deleteIfExists(file.path);
				}
			}

			// TODO KW 20181015 - don't do this - any updates not involving a re-index will wipe attachments out
			// delete removed attachments from remote
//			if (current != null) {
//				for (Content.Attachment had : current.content.attachments) {
//					if (!indexed.content.attachments.contains(had)) {
//						switch (had.type) {
//							case IMAGE:
//								imageStore.delete(had.url, d -> {
//								});
//								break;
//							default:
//								attachmentStore.delete(had.url, d -> {
//								});
//						}
//					}
//				}
//			}

			if (submission != null && indexed.content.downloads.stream().noneMatch(d -> d.direct)) {
				String uploadPath = repo.path().relativize(next.resolve(submission.filePath.getFileName())).toString();
				contentStore.store(submission.filePath, uploadPath, (fileUrl, ex) ->
					indexed.content.downloads.add(new Download(fileUrl, true, Download.DownloadState.OK))
				);
			}

			repo.put(indexed.content);

			this.changes.add(indexed.content.hash);

			return true;
		}
		return false;
	}
}
