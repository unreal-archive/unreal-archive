package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Main {

	public static void main(String[] args) throws IOException {

		final List<IndexLog> indexLogs = new ArrayList<>();

		Files.list(Paths.get("/home/shrimp/tmp/maps2/")).sorted().forEach(f -> {

			if (f.toString().endsWith("tmp")) return;

			ContentSubmission sub = new ContentSubmission(f);
			IndexLog log = new IndexLog(sub);
			indexLogs.add(log);

			try (Incoming incoming = new Incoming(sub, log)) {
				ContentClassifier.ContentType type = ContentClassifier.classify(incoming, log);

				if (type != ContentClassifier.ContentType.UNKNOWN) { // TODO later support a generic dumping ground for unknown content

					type.indexer.get().index(incoming, type.newContent(incoming), log, c -> {
						try {
							c.lastIndex = LocalDateTime.now();
							if (sub.sourceUrls != null && sub.sourceUrls.length > 0) {
								for (String url : sub.sourceUrls) {
									c.downloads.add(new Download(url, LocalDate.now(), false));
								}
							}

							// TODO upload file to our storage, and add to downloads url set

//							Path repack = incoming.getRepack(c.name);

							YAML.toString(c);
						} catch (IOException e) {
							System.out.println("Failed to output " + f.toString());
							e.printStackTrace();
						}
					});
				} else {
					log.log(IndexLog.EntryType.FATAL, "File " + f + " cannot be classified.");
				}
			} catch (Throwable e) {
				log.log(IndexLog.EntryType.FATAL, e.getMessage(), e);
				System.out.println("Failed processing " + f.toString());
				e.printStackTrace();
			}
		});

		int err = 0;

		for (IndexLog l : indexLogs) {
			if (!l.ok()) {
				err++;
				System.out.println(l);
			}
		}

		System.out.printf("%nCompleted indexing %d files, with %d errors%n", indexLogs.size(), err);
	}
}
