# Unreal Archive

Scans, categorises and produces metadata for Unreal, Unreal Tournament, 
and Unreal Tournament 2003/4 content.

## TODO

- Content categorisation
  1. ~~support Unreal and UT maps and map packs~~
  2. ~~support skins~~
  3. support models
  4. support mutators
  5. support mods/gametypes
- ~~Metadata is to be stored in YAML format, with a format and file structure
  appropriate to each content type~~
- ~~Original release dates (stick to Month and Year) can be gleaned from the
  contents of archives where possible~~
- ~~Generate and store SHA1 hashes for archives, as well as individual content
  (only for packages such as maps, textures, sounds, not readmes and config
  files)~~
- Support for documents and articles, stored in Markdown format with YAML
  metadata
- Support for generating the HTML output/presentation for the content database
  - ~~Maps~~
  - ~~Map Packs~~
  - Skins
  - Models
  - Mutators
  - Mods and Gametypes
  - Documents and Articles
  - Support for mirroring images so as to make offline use and mirrors feasible
    and to reduce reliance on continued existence of some hosting.
- Support to create a local mirror of the archive, complete or by game.
- Publish website
- Publish code publically


## Content Submission and Indexing Pipeline

- For any given file, a `Submission` instance is created.
  - If the file's directory contains an `_override.yml` file, that file is
    loaded as a `SubmissionOverride` instance and associated with the 
    `Submission`.
- The `Submission` is used to instantiate an `Incoming` instance, which
  contains additional information like the file's SHA1 hash.
- If the content is new (hash is not yet known), the file is unpacked onto
  disk (via system-installed archiving tools `7z` and `unrar`).
- The `Incoming` instance is passed to `ContentType.classify()` for content
  type classification, where the type of content is determined (see Content
  Identification).
- If the content type can be identified, the content type will be used as a
  factory to instantiate a new `Content` element of the appropriate type 
  with common content information and metadata.
- The content type will again be used as a factory to instantiate an 
  `IndexHandler` instance, which will take the new `Content` instance and
  populate it with more type-specific metadata extracted from the actual
  content being indexed (eg. extract author and screenshot information from
  map packages, or determine whether or not a skin has team colours defined).
- Once indexed, the result is returned via a `Consumer<>`, and this is then
  "checked into" the `ContentManager`, which will write the content metadata
  to disk in YAML format, as well as sync the package and any additional 
  files to the nominated data store.

Most of this process uses `Consumer<>`s for providing feedback, with the 
intention of eventually being able to make this process more parallelised for
better performance. Currently the performance is "good enough" that such
parallel behaviour is not quite worth the refactoring time.


## Content Identification

### Maps

- Loose, archive (zip, 7z, rar, self-extracting, etc), or Umod
- Must have a single `.unr` or `.ut2` file present; any other files present
  will be ignored.
  - `.u` may indicate a mod, though if it only has a single `.unr` or `.ut2`,
    it is probably a map with some custom code.
- Gametype is identified using a simple collection of known prefixes (`DM-`, 
  `MH-`, etc)
  - Single Player maps don't usually have a prefix, we investigate the 
    map's README to see if it says anything about being single player, though
    this is unreliable and requires manual tweaking later.

### Map Packs

- Archive (zip, 7z, rar, self-extracting, etc), or Umod
- Multiple `.unr` or `.ut2` files present
- May contain `.utx`, `.umx`, `.ogg` and other package content
- No `.u` or `.int` files should be present, as these could indicate the 
  content of a mod or gametype, which also contain maultiple maps

### Skins

- Archive (zip, 7z, rar, self-extracting, etc) or Umod
- One or more `.utx` files present
- UT (and Unreal?):
  - One or more `.int` files present, containing the skin manifest format
- UT2003 and UT2004:
  - One or more `.upl` files present
- No other content

### Models

- Archive (zip, 7z, rar, self-extracting, etc) or Umod
- At least one `.utx` file present (contains mesh)
- UT (and Unreal?):
  - One `.u` file present (contains mesh)
  - One or more `.int` files present, containing the player model manifest
- UT2003 and UT2004:
  - One or more `.upl` files present
  - One or more `.ukx` (animation) files present
- No other content

### Mods and Gametypes

- Manually?


## Storage

### HTTP/DAV

- `--store=dav`
  - `--store-[images|attachments|content]=dav`
- `--dav-url=http://hostname/path/`
  - `--dav-url-[images|attachments|content]=...`

### Backblaze B2

- `--store=b2`
  - `--store-[images|attachments|content]=b2`
- `--b2-acc=key-id`
  - `--b2-acc-[images|attachments|content]=key-id`
- `--b2-key=key`
  - `--b2-key-[images|attachments|content]=key`
- `--b2-bucket=bucket-id`
  - `--b2-bucket-[images|attachments|content]=bucket-id`

Environment variables may be used, eg.: replace `--b2-key-images=key` with 
`B2_KEY_IMAGES=key`.
