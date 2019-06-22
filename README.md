# Unreal Archive

Scans, categorises and produces metadata for Unreal, Unreal Tournament, 
and Unreal Tournament 2003/4 content, and builds a static browsable website
of the content, currently published at 
[https://unrealarchive.org/](https://unrealarchive.org/).


## TODO

- More Docs
- Support mods/gametypes, definitions and browsable output.


## Requirements

- Java JRE 11 for running
- Java JDK 11 for building

Tested with both OpenJDK 11 on Linux and Azul's Zulu Java 11 on Windows.


## Building

The project is build with Gradle. The provided `gradlew` wrapper may be 
invoked as follows to produce an executable Jar file:

### On Linux

```
./gradlew execJar
```

To run, execute:

```
./build/libs/unreal-archive
```

### On Windows

```
gradlew.bat execJar
```

To run, execute:

```
java -jar build\libs\unreal-archive-exec.jar
```

## Usage and Functionality

> TODO: complete this section 

**Browsing and Information:**
- `ls`: List indexed content filtered by game, type or author.
- `show`: Show data for the content items specified.
- `summary`: Show stats and counters for the content index.

**Content Management**
- `scan`: Dry-run scan the contents of files or paths, comparing to known content where possible.
- `index`: Index the contents of files or paths, writing the results to the content path.
- `edit`: Edit the metadata for the <hash> provided.
- `sync`: Sync managed files' local files to remote storage.

**Utilities and Tools:**
- `unpack`: Unpack the contents of a umod file to a directory.

**Website Build:**
- `www`: Generate the HTML website for browsing content.


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
parallel behaviour is not quite worth the implementation time.


## Storage Configuration

A storage backend must be configured in order to store content during indexing.

When interrogating, mirroring or downloading content, no store needs to be 
specified, the default no-op store (`--store=nop`) will be used. 

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
