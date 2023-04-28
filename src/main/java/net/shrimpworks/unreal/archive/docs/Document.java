package net.shrimpworks.unreal.archive.docs;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import net.shrimpworks.unreal.archive.common.Util;

public class Document implements Comparable<Document> {

	public LocalDate createdDate;
	public LocalDate updatedDate;

	public String game = "General";
	public String name = "document.md";     // file name of the markdown document
	public String path = "";                // defines a path-like structure for navigation; "Editing/UnrealScript"
	public String title;                    // How to X and Y
	public String titleImage;               // "pic.png"
	public String author = "Unknown";       // Joe Soap
	public String description = "None";     // A cool document that shows you how to X and Y
	public boolean published = true;        // false will hide it

	public Path slugPath(Path root) {
		String game = Util.slug(this.game);
		String path = Arrays.stream(this.path.split("/")).map(Util::slug).collect(Collectors.joining("/"));
		String name = Util.slug(this.title);
		return root.resolve(game).resolve(path).resolve(name);
	}

	@Override
	public int compareTo(Document document) {
		return createdDate.compareTo(document.createdDate);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Document that)) return false;
		return Objects.equals(game, that.game)
			   && Objects.equals(path, that.path)
			   && Objects.equals(title, that.title)
			   && Objects.equals(titleImage, that.titleImage)
			   && Objects.equals(author, that.author)
			   && Objects.equals(description, that.description);
	}

	@Override
	public int hashCode() {
		return Objects.hash(game, path, title, titleImage, author, description);
	}
}
