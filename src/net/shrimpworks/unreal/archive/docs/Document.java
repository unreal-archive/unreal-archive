package net.shrimpworks.unreal.archive.docs;

import java.time.LocalDate;
import java.util.Objects;

public class Document {

	public LocalDate createdDate;
	public LocalDate updatedDate;

	public String game = "General";
	public String name = "document.md";		// file name of the markdown document
	public String path = "";                // defines a path-like structure for navigation; "Editing/UnrealScript"
	public String title;                    // How to X and Y
	public String titleImage;               // "pic.png"
	public String author = "Unknown";       // Joe Soap
	public String description = "None";     // A cool document that shows you how to X and Y
	public boolean published = true;		// false will hide it

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Document document = (Document)o;
		return Objects.equals(game, document.game)
			   && Objects.equals(path, document.path)
			   && Objects.equals(title, document.title)
			   && Objects.equals(titleImage, document.titleImage)
			   && Objects.equals(author, document.author)
			   && Objects.equals(description, document.description);
	}

	@Override
	public int hashCode() {
		return Objects.hash(game, path, title, titleImage, author, description);
	}
}
