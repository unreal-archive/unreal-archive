package net.shrimpworks.unreal.archive.content;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import net.shrimpworks.unreal.archive.YAML;
import net.shrimpworks.unreal.archive.storage.DataStore;

public class ContentManager {

	private final ContentRepository repo;

	private final DataStore contentStore;
	private final DataStore imageStore;
	private final DataStore attachmentStore;

	private final Set<String> changes;

	public ContentManager(ContentRepository repo, DataStore contentStore, DataStore imageStore, DataStore attachmentStore) {
		this.repo = repo;

		this.contentStore = contentStore;
		this.imageStore = imageStore;
		this.attachmentStore = attachmentStore;

		this.changes = new HashSet<>();
	}

	public ContentRepository repo() {
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
	public Content checkout(String hash) {
		Content out = repo.forHash(hash);
		if (out != null) {
			try {
				return YAML.fromString(YAML.toString(out), Content.class);
			} catch (IOException e) {
				throw new IllegalStateException("Cannot clone content " + out);
			}
		}
		return null;
	}

	public boolean checkin(IndexResult<? extends Content> indexed, Submission submission) throws IOException {
		Content current = repo.forHash(indexed.content.hash);

		if (current == null || (!indexed.content.equals(current) || !indexed.files.isEmpty())) {
			// lets store the content \o/
			Path next = indexed.content.contentPath(repo.path());

			for (IndexResult.NewAttachment file : indexed.files) {
				// use same path structure as per contentPath
				try {
					String uploadPath = repo.path().relativize(next.resolve(file.name)).toString();
					if (file.type == Content.AttachmentType.IMAGE) {
						imageStore.store(file.path, uploadPath, (fileUrl, ex) ->
							indexed.content.attachments.add(new Content.Attachment(file.type, file.name, fileUrl)));
					} else {
						attachmentStore.store(file.path, uploadPath, (fileUrl, ex) ->
							indexed.content.attachments.add(new Content.Attachment(file.type, file.name, fileUrl)));
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

			if (submission != null && indexed.content.downloads.stream().noneMatch(d -> d.main)) {
				String uploadPath = repo.path().relativize(next.resolve(submission.filePath.getFileName())).toString();
				contentStore.store(submission.filePath, uploadPath, (fileUrl, ex) ->
					indexed.content.downloads.add(new Content.Download(fileUrl, true, false, Content.DownloadState.OK))
				);
			}

//			Path newYml = next.resolve(String.format("%s_[%s].yml", Util.slug(indexed.content.name), indexed.content.hash.substring(0, 8)));
//			Files.writeString(Util.safeFileName(newYml), YAML.toString(indexed.content),
//							  StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
//
//			if (current != null && !current.path.equals(newYml)) {
//				// remove old yml file if new file changed
//				Files.deleteIfExists(current.path);
//			}
//
//			this.content.put(indexed.content.hash, new ContentHolder(newYml, indexed.content));

			repo.put(indexed.content);

			this.changes.add(indexed.content.hash);

			return true;
		}
		return false;
	}
}
