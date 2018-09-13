# Unreal Archiver

Scans, categorises and produces metadata for Unreal and Unreal Tournament 
content.

## TODO

- content categorisation
  1. support Unreal and UT maps
  2. support skins
  3. support models
  4. support mods
- Metadata is to be stored in YAML format, with a format and file structure
  appropriate to each content type
- Each piece of content to be stored in its own directory, named after its
  Filename? Each directory would contain:
  - Metadata definition
  - One or more images
  - Not the content, since content would be retrieved from a URL
- Could non-zip content be repackaged if possible (should .rar, 7z, exe 
  content be repackaged as zips)?
  - Makes management easier in the future, perhaps
  - see maps notes below
- Original release dates (stick to Month and Year) can be gleaned from the
  contents of archives where possible
- Generate and store SHA1 hashes for archives, as well as individual content
  (only for packages such as maps, textures, sounds, not readmes and config
  files)


## Maps

### Content Identification

- Loose, archive (zip, 7z, rar, self-extracting, etc), or Umod
- Must have a single `.unr` or `.ut2` file present, and optionally `.umx`,
  `.ogg`, `.uax`, `.utx`, `.usx`, `.u` files as well.
  - `.u` may indicate a mod, though if it only has a single `.unr` or `.ut2`,
    it is probably a map with some custom code.
- Gametype identification? UT2003/4 has a default gametype identified stored
  within LevelInfo, might not be reliable? If using prefixes, need a dataset
  of prefixes to mods to match against.
  - v1 just support standard gametypes

### Metadata

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


## Skins

### Content Identification

- Loose, archive (zip, 7z, rar, self-extracting, etc), or Umod
- One or more `.utx` files present
- UT (and Unreal?):
  - One or more `.int` files present, containing the skin manifest format
- UT2003 and UT2004:
  - One or more `.upl` files present
- No other content

