package net.shrimpworks.unreal.archive.content.gametypes;

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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jetbrains.annotations.NotNull;

import net.shrimpworks.unreal.archive.AuthorNames;
import net.shrimpworks.unreal.archive.ContentEntity;
import net.shrimpworks.unreal.archive.Platform;
import net.shrimpworks.unreal.archive.Util;
import net.shrimpworks.unreal.archive.content.Content;
import net.shrimpworks.unreal.archive.content.NameDescription;

/**
 * Although Game Types live within the Content tree and do share some attributes,
 * they don't share a common base class, since Content is designed for singular
 * pieces of released content, whereas a game type/mod/total conversion typically
 * contains much more detailed information, and will consist of several releases,
 * patches, and other downloads.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true, property = "contentType")
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
		if (!(o instanceof GameType)) return false;
		GameType gameType = (GameType)o;
		return deleted == gameType.deleted &&
			   Objects.equals(contentType, gameType.contentType) &&
			   Objects.equals(addedDate, gameType.addedDate) &&
			   Objects.equals(game, gameType.game) &&
			   Objects.equals(name, gameType.name) &&
			   Objects.equals(author, gameType.author) &&
			   Objects.equals(description, gameType.description) &&
			   Objects.equals(titleImage, gameType.titleImage) &&
			   Objects.equals(bannerImage, gameType.bannerImage) &&
			   Objects.equals(links, gameType.links) &&
			   Objects.equals(credits, gameType.credits) &&
			   Objects.equals(releases, gameType.releases);
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
		public int compareTo(@NotNull GameType.Release o) {
			return o.releaseDate.compareTo(releaseDate);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof Release)) return false;
			Release release = (Release)o;
			return deleted == release.deleted
				   && Objects.equals(title, release.title)
				   && Objects.equals(version, release.version)
				   && Objects.equals(releaseDate, release.releaseDate)
				   && Objects.equals(description, release.description);
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
		public List<Content.Download> downloads = new ArrayList<>();

		public String originalFilename;             // dm-mymap.zip
		public String hash;
		public long fileSize = 0;                   // filesize, we'll determine when synced
		public boolean synced = false;              // if false, localFile will be uploaded and turned into a download upon sync
		public boolean deleted = false;             // if deleted, prevents from syncing and will not publish

		// deeper detail about the download, useful for dependency resolution and package locating
		public List<Content.ContentFile> files = new ArrayList<>(); // [DM-MyMap.unr, MyTex.utx]
		public int otherFiles;                      // count of non-content files (readme, html, etc)
		public Map<String, List<Content.Dependency>> dependencies = new HashMap<>();// packages this content depends on

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof ReleaseFile)) return false;
			ReleaseFile that = (ReleaseFile)o;
			return fileSize == that.fileSize
				   && synced == that.synced
				   && deleted == that.deleted
				   && otherFiles == that.otherFiles
				   && Objects.equals(title, that.title)
				   && Objects.equals(localFile, that.localFile)
				   && Objects.equals(downloads, that.downloads)
				   && Objects.equals(originalFilename, that.originalFilename)
				   && Objects.equals(hash, that.hash)
				   && platform == that.platform
				   && Objects.equals(files, that.files)
				   && Objects.equals(dependencies, that.dependencies);
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
		public final Content.Attachment screenshot;

		@ConstructorProperties({ "name", "title", "author", "screenshot" })
		public GameTypeMap(String name, String title, String author, Content.Attachment screenshot) {
			this.name = name;
			this.title = title;
			this.author = author;
			this.screenshot = screenshot;
		}
	}
}
