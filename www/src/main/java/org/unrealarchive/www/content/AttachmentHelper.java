package org.unrealarchive.www.content;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.unrealarchive.common.Util;
import org.unrealarchive.content.addons.Addon;
import org.unrealarchive.www.SiteFeatures;

public interface AttachmentHelper {

	SiteFeatures features();

	/**
	 * Download and store image files locally.
	 * <p>
	 * This works by replacing in-memory image attachment URLs
	 * with local paths for the purposes of outputting HTML pages
	 * linking to local copies, rather than the original remotes.
	 * <p>
	 * The prefix provided is used to differentiate between different
	 * files which may share the same name, and should be specified as
	 * something like the content hash (or part thereof).
	 */
	default void localImages(List<Addon.Attachment> attachments, Path localPath, String prefix) {
		if (!features().localImages) return;

		// we're creating a sub-directory here, to create a nicer looking on-disk structure
		Path imgPath = localPath.resolve("images");
		try {
			if (!Files.exists(imgPath)) Files.createDirectories(imgPath);
		} catch (IOException e) {
			System.err.printf("\rFailed to download create output directory %s: %s%n", imgPath, e);
			return;
		}

		attachments.replaceAll(img -> {
			if (img.type != Addon.AttachmentType.IMAGE) return img;
			if (!img.url.startsWith("http")) return img;

			try {
				return downloadImage(img, localPath, imgPath, prefix);
			} catch (Throwable t) {
				System.err.printf("\rFailed to download image %s: %s%n", img.name, t);
				return img;
			}
		});
	}

	private Addon.Attachment downloadImage(Addon.Attachment img, Path localPath, Path imgPath, String prefix) throws IOException {
		// prepend filenames with the content hash, to prevent conflicts
		String hashName = String.join("_", prefix, img.name);
		Path outPath = imgPath.resolve(Util.safeFileName(hashName));

		// only download if it doesn't already exist locally
		if (!Files.exists(outPath)) {
			System.out.printf("\rDownloading image %-60s", img.name);
			Util.downloadTo(img.url, outPath);
		}
		return new Addon.Attachment(img.type, img.name, localPath.relativize(outPath).toString());
	}

	default void rewriteAttachmentsUrls(List<Addon.Attachment> attachments) {
		if (features().attachmentRewrites.isEmpty() || attachments.isEmpty()) return;

		attachments.replaceAll(this::applyRewrites);
	}

	private Addon.Attachment applyRewrites(Addon.Attachment attachment) {
		String newUrl = attachment.url;
		for (Map.Entry<String, String> entry : features().attachmentRewrites.entrySet()) {
			newUrl = newUrl.replace(entry.getKey(), entry.getValue());
		}
		return new Addon.Attachment(attachment.type, attachment.name, newUrl);
	}

}
