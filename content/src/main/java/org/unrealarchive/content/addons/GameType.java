package org.unrealarchive.content.addons;

import java.beans.ConstructorProperties;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.unrealarchive.common.Platform;
import org.unrealarchive.common.Util;
import org.unrealarchive.content.AuthorNames;
import org.unrealarchive.content.ContentEntity;
import org.unrealarchive.content.Download;
import org.unrealarchive.content.NameDescription;

/**
 * Although Game Types live within the Content tree and do share some attributes,
 * they don't share a common base class, since Content is designed for singular
 * pieces of released content, whereas a game type/mod/total conversion typically
 * contains much more detailed information, and will consist of several releases,
 * patches, and other downloads.
 */
public class GameType implements ContentEntity<GameType> {

	// Game/Type/GameType/A/[hash]
	private static final String PATH_STRING = "%s/%s/%s/%s/";

	public String contentType = "GameType";

	public LocalDate addedDate;

	public String game = "Unknown";                         // Unreal Tournament
	public String name;                                     // Bunny Hunt
	public String author = "Unknown";                       // Joe Soap
	public String description = "None";                     // Game mode for jumping and hunting
	public String titleImage = "title.png";                 // title.jpg - shown on browse page
	public String bannerImage = "banner.png";               // banner.jpg - shown on detail page

	public Map<String, String> links = new HashMap<>();
	public Map<String, List<String>> credits = new HashMap<>();

	public List<NameDescription> gameTypes = new ArrayList<>();
	public List<NameDescription> mutators = new ArrayList<>();
	public List<GameTypeMap> maps = new ArrayList<>();

	public List<Release> releases = new ArrayList<>();

	/**
	 * If true, will not show up in www output, and will be ignored in index passes.
	 */
	public boolean deleted = false;

	@Override
	public Path contentPath(Path root) {
		String namePrefix = subGrouping();
		return root.resolve(String.format(PATH_STRING,
										  game,
										  "GameTypes",
										  namePrefix,
										  Util.slug(name)
		));
	}

	@Override
	public Path slugPath(Path root) {
		String type = Util.slug(this.contentType.toLowerCase().replaceAll("_", "") + "s");
		String game = Util.slug(this.game);
		String name = Util.slug(this.name);
		return root.resolve(type).resolve(game).resolve(subGrouping()).resolve(name);
	}

	@Override
	public Path pagePath(Path root) {
		return slugPath(root).resolve("index.html");
	}

	/**
	 * Grouping for pagination and directory partitioning.
	 * <p>
	 * Generally the first letter of the content's name.
	 *
	 * @return grouping character(s)
	 */
	public String subGrouping() {
		char first = name.toUpperCase().replaceAll("[^A-Z0-9]", "").charAt(0);
		if (Character.isDigit(first)) first = '0';
		return Character.toString(first);
	}

	@Override
	public String game() {
		return game;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String author() {
		return author;
	}

	@Override
	public String authorName() {
		return AuthorNames.nameFor(author);
	}

	@Override
	public String releaseDate() {
		return releases.stream().map(r -> r.releaseDate).sorted().findFirst().orElse("Unknown");
	}

	@Override
	public String autoDescription() {
		return description;
	}

	@Override
	public LocalDateTime addedDate() {
		return addedDate.atStartOfDay();
	}

	@Override
	public String contentType() {
		return "GameType";
	}

	@Override
	public String friendlyType() {
		return "Game Type";
	}

	@Override
	public String leadImage() {
		return slugPath(Paths.get("")).resolve(titleImage).toString();
	}

	@Override
	public Map<String, String> links() {
		return links;
	}

	@Override
	public boolean deleted() {
		return deleted;
	}

	@JsonIgnore
	@Override
	public boolean isVariation() {
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof GameType other)) return false;
		return deleted == other.deleted &&
			   Objects.equals(contentType, other.contentType) &&
			   Objects.equals(addedDate, other.addedDate) &&
			   Objects.equals(game, other.game) &&
			   Objects.equals(name, other.name) &&
			   Objects.equals(author, other.author) &&
			   Objects.equals(description, other.description) &&
			   Objects.equals(titleImage, other.titleImage) &&
			   Objects.equals(bannerImage, other.bannerImage) &&
			   Objects.equals(links, other.links) &&
			   Objects.equals(credits, other.credits) &&
			   Objects.equals(releases, other.releases);
	}

	@Override
	public int hashCode() {
		return Objects.hash(contentType, addedDate, game, name, author, description, titleImage, bannerImage, links, credits,
							releases, deleted);
	}

	/**
	 * Represents a file/download for a game type.
	 * <p>
	 * These can be platform specific, and will feature their own collections
	 * of mirrors.
	 * <p>
	 * Aside from the title and version information, the properties will be
	 * automatically managed by the indexer.
	 */
	public static class Release implements Comparable<Release> {

		// manually configured properties
		public String title;
		public String version;
		public String releaseDate = "Unknown";      // 2001-05
		public String description = "";             // specific description for this release

		public List<ReleaseFile> files = new ArrayList<>();

		public boolean deleted = false;             // if deleted, prevents from syncing and will not publish

		@Override
		public int compareTo(GameType.Release o) {
			return o.releaseDate.compareTo(releaseDate);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof Release other)) return false;
			return deleted == other.deleted
				   && Objects.equals(title, other.title)
				   && Objects.equals(version, other.version)
				   && Objects.equals(releaseDate, other.releaseDate)
				   && Objects.equals(description, other.description);
		}

		@Override
		public int hashCode() {
			return Objects.hash(title, version, releaseDate, description, deleted);
		}
	}

	public static class ReleaseFile {

		public String title;
		public String localFile;                    // local path to the file to be synced
		public Platform platform = Platform.ANY;    // platform-specific files

		// the results of syncing will be appended to this collection, or manually added mirrors can be added here
		public List<Download> downloads = new ArrayList<>();

		public String originalFilename;             // dm-mymap.zip
		public String hash;
		public long fileSize = 0;                   // filesize, we'll determine when synced
		public boolean synced = false;              // if false, localFile will be uploaded and turned into a download upon sync
		public boolean deleted = false;             // if deleted, prevents from syncing and will not publish

		// deeper detail about the download, useful for dependency resolution and package locating
		public List<Addon.ContentFile> files = new ArrayList<>(); // [DM-MyMap.unr, MyTex.utx]
		public int otherFiles;                      // count of non-content files (readme, html, etc)
		public Map<String, List<Addon.Dependency>> dependencies = new HashMap<>();// packages this content depends on

		public Download directDownload() {
			return downloads.stream().filter(d -> d.direct).findAny().orElse(null);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof ReleaseFile other)) return false;
			return fileSize == other.fileSize
				   && synced == other.synced
				   && deleted == other.deleted
				   && otherFiles == other.otherFiles
				   && Objects.equals(title, other.title)
				   && Objects.equals(localFile, other.localFile)
				   && Objects.equals(downloads, other.downloads)
				   && Objects.equals(originalFilename, other.originalFilename)
				   && Objects.equals(hash, other.hash)
				   && platform == other.platform
				   && Objects.equals(files, other.files)
				   && Objects.equals(dependencies, other.dependencies);
		}

		@Override
		public int hashCode() {
			return Objects.hash(title, localFile, downloads, originalFilename, hash, fileSize, platform, synced, deleted, files, otherFiles,
								dependencies);
		}
	}

	public static class GameTypeMap {

		public final String name;
		public final String title;
		public final String author;
		public final Addon.Attachment screenshot;

		@ConstructorProperties({ "name", "title", "author", "screenshot" })
		public GameTypeMap(String name, String title, String author, Addon.Attachment screenshot) {
			this.name = name;
			this.title = title;
			this.author = author;
			this.screenshot = screenshot;
		}
	}
}
