package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {

	public static void main(String[] args) throws IOException {

		final List<IndexLog> indexLogs = new ArrayList<>();

		Files.list(Paths.get("/home/shrimp/tmp/maps/")).sorted().forEach(f -> {

			if (f.toString().endsWith("tmp")) return;

			ContentSubmission sub = new ContentSubmission(f);
			IndexLog log = new IndexLog(sub);
			indexLogs.add(log);

			try {
				Incoming incoming = new Incoming(sub, log);

				ContentClassifier.ContentType type = ContentClassifier.classify(incoming, log);

				if (type != ContentClassifier.ContentType.UNKNOWN) { // TODO later support a generic dumping ground for unknown content
					type.indexer.get().index(incoming, log, c -> {
						try {
							System.out.println(YAML.toString(c));
						} catch (IOException e) {
							e.printStackTrace();
						}
					});
				} else {
					log.log(IndexLog.EntryType.FATAL, "File " + f + " cannot be classified.");
				}
			} catch (Exception e) {
				log.log(IndexLog.EntryType.FATAL, e.getMessage(), e);
				e.printStackTrace();
			}
		});

		for (IndexLog log : indexLogs) {
			System.out.println(log);
		}
	}
}
