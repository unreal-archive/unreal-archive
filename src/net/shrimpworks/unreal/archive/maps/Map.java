package net.shrimpworks.unreal.archive.maps;

import net.shrimpworks.unreal.archive.Content;

public class Map extends Content {

	/*
		- Index date
		- Map name (DM-MyMap, as it appears in-game)
		- Gametype (also need to maintain gametype references somewhere)
		- Title (My Map)
		- Author
		- Estimated Release (based on file date within archives)
		- Ideal Player Count
		- Level Screenshots (list of names of files in this directory) as extracted
		  from the map
		- SHA1 of the file containing the map
		- File size
		- File list, names and SHA1 of files inside the package file
		- Download References (list of URLs for this file)
		  - If we do repackaging of odd formats into zips, flag reference as a repack
			- indexing will need to take into account that any file might have a repack
			  and exclude it from indexing
			- repacks also aren't likely to be widely available
		- Deleted flag, used to exclude this file from listings, but we want to keep a
		  record of it so we don't re-index it, etc.
	 */

	public String gametype;             // Deathmatch
	public String title;                // My Map
	public String playerCount;          // 2 - 4 Players

}
