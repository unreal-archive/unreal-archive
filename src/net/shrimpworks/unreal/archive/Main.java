package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

	public static void main(String[] args) throws IOException {
		Files.list(Paths.get("/home/shrimp/tmp/maps/")).forEach(f -> {

			try {
				System.out.println("Inspect " + f);

				ContentSubmission sub = new ContentSubmission(f);

				Incoming incoming = new Incoming(sub);

				ContentClassifier.ContentType type = ContentClassifier.classify(incoming);

				if (type != null) {
					type.indexer.get().index(incoming, c -> {
						try {
							System.out.println(YAML.toString(c));
						} catch (IOException e) {
							e.printStackTrace();
						}
					});
				} else {
					System.err.println("File " + f + " cannot be classified.");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
}
