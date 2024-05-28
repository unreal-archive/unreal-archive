package org.unrealarchive.content.docs;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Objects;

import org.unrealarchive.common.Util;

public class Document implements Comparable<Document> {

	public LocalDate createdDate;
	public LocalDate updatedDate;

	public String game = "Unreal";
	public String group = "Reference";      // root level grouo
	public String subGroup = "UnrealEd";    // subgrouping
	public String name = "document.md";     // file name of the markdown document
	public String title;                    // How to X and Y
	public String titleImage;               // "pic.png"
	public String author = "Unknown";       // Joe Soap
	public String description = "None";     // A cool document that shows you how to X and Y
	public boolean published = true;        // false will hide it

	public Path slugPath(Path root) {
		String game = Util.slug(this.game);
		String group = Util.slug(this.group);
		String subGroup = Util.slug(this.subGroup);
		String name = Util.slug(this.title);
		return root.resolve(game).resolve(group).resolve(subGroup).resolve(name);
	}

	public Path pagePath(Path root) {
		return slugPath(root).resolve("index.html");
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
			   && Objects.equals(group, that.group)
			   && Objects.equals(subGroup, that.subGroup)
			   && Objects.equals(title, that.title)
			   && Objects.equals(titleImage, that.titleImage)
			   && Objects.equals(author, that.author)
			   && Objects.equals(description, that.description);
	}

	@Override
	public int hashCode() {
		return Objects.hash(game, group, subGroup, title, titleImage, author, description);
	}
}
