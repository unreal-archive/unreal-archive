# Unreal Archive

Scans, categorises and produces metadata for Unreal, Unreal Tournament, Unreal
Tournament 2003/4, and Unreal Tournament 3 content, and builds a static 
browsable website of the content, currently published at 
[https://unrealarchive.org/](https://unrealarchive.org/).

## Table of Contents

- [Requirements](#requirements)
- [Structure and Modules](#structure-and-modules)
- [Building](#building)
- [Usage and Functionality](#usage-and-functionality)
- [Content Submission and Indexing Pipeline](#content-submission-and-indexing-pipeline)
- [Author Management](#author-management)
- [Mirroring](#mirroring)
- [Storage Configuration](#storage-configuration)


## Requirements

- Java JDK 21 for building

Tested with both OpenJDK 21 on Linux and Azul's Zulu Java 21 on Windows.


## Structure and Modules

The project is arranged into several modules, and published as Java Modules so
components may be reused by other software projects.

These are:

- `common`: the usual assortment of shared boilerplate code and utility classes
  and functions.
- `content`: the definition of the content types, Maps, Map Packs, Skins, Game
  Types, etc. These are the elements which are serialiased to and from YAML
  format stored in the archive data repository, and projects wishing to work
  with the Unreal Archive data types should re-use these if possible.
  In addition, the code for loading, saving, and generally working with the
  data sets are here too.
- `storage`: the remote storage/mirror implementations, for S3 and Azure and
  associated interfaces.
- `indexing`: implements the primary "archiving" functionality, including 
  repositories for content, mods, gametypes, documents, wikis, etc. It's also
  used for data ingest, categorisation, storage, replication, and management 
  of repository content and files.
- `www`: the code and templates responsible for generating the static website
  output.
- root project: the content indexing and mirroring implementation and
  management. This will likely move into its own module(s) later.


## Building

The project is built with Gradle. The provided `gradlew` wrapper may be 
invoked as follows to build all modules and generate the `indexing` `www`
executables:

### On Linux

```shell
./gradlew jlink
```

To run, execute either:

```shell
# indexing
./indexing/build/unreal-archive-indexing/bin/indexing

# www
./www/build/unreal-archive-www/bin/www
```

### On Windows

```shell
gradlew.bat jlink
```

To run, execute:

```shell
# indexing
indexing\build\unreal-archive-indexing\bin\indexing.bat

# www
www\build\unreal-archive-www\bin\www.bat
```


## Usage and Functionality

> TODO: complete this section 

Run executables with no arguments to see input arguments and additional help for each command.

### Indexing Commands 

**Browsing and Information:**
- `ls`: List indexed content filtered by game, type or author.
- `filter`: List indexed content filtered by `attribute=value` pairs.
- `show`: Show data for the content items specified.
- `summary`: Show stats and counters for the content index.

**General Content Management**
- `scan`: Dry-run scan the contents of files or paths, comparing to known content where possible.
- `index`: Index the contents of files or paths, writing the results to the content path.
- `edit`: Edit the metadata for the <hash> provided.
- `set`: Convenience, set an attribute for the <hash> provided. Eg: `set <hash> author Bob`.
- `sync`: Sync managed files' local files to remote storage.
- `authors`: Manage authors and aliases.

**Gametype Management**
- All commands prefixed by `gametype`:
- `init <game> <gametype name>`: Create a skeleton gametype file structure.
- `locate <game> <gametype name>`: Show the local file path to the provided gametype.
- `index <game> <game type name> <release name>`: Indexes the content of the release specified.
- `add <game> <game type name> <release name> <file>`:
      Convenience, which adds a gametype if it does not yet exist, adds a release, and indexes 
      the release. A `sync` command afterwards is still required to sync download files to 
      mirrors.
- `addmirror <game> <game type name> <release name> <url>`: Adds a secondary mirror to the 
      gametype specified.

**Mirrors:**
- `local-mirror`: Create a local mirror of all file content.
- `mirror`: Create a remote mirror of all file content and add mirror links.

**Utilities and Tools:**
- `unpack`: Unpack the contents of a umod file to a directory.
- `install`: Unpack the contents of a file, URL or hash, and place within an Unreal
      game's standard directory layout (Maps, System, Textures etc).

### WWW Commands

**Website Build:**
- `www`: Generate the HTML website for browsing content.


## Author Management

Author names and aliases can be managed using the `indexing` binary. 
Specifically, this can be used to create an author record which can then be
further tweaked by hand.

Add an author and associated aliases:

```
./indexing authors add "Author Primary Name" "Alias 1" "Another Alias" 
```

This will generate an author file in the content directory, under path
`authors/author-primary-name/author.yml`. The file may be edited by hand, 
refer to the README in the authors directory for more information.

An author with aliases will cause any content set as created by either the
author's primary name, or any of the defined aliases, to be rolled up under
the primary name. All content will be shown as created by the primary name
defined, regardless of which alias the content was actually tagged as.

This allows a single author with different spellings, changing handles over
their career, or typos in names from long ago, to be grouped under a single
author profile.


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


## Mirroring

If you have storage capacity available and would like to contribute some of it
as a public mirror for archive content, the following steps should be taken.

1. Fork and clone the [`unreal-archive-data`](https://github.com/unreal-archive/unreal-archive-data)
   repository.
   - This dataset will be updated during the mirroring process.
2. Download or [build](#building) the `indexing` project binary
3. Execute `indexing mirror` with the following command-line options:
   - `--content-path=/path/to/unreal-archive-data`
   - `--store=[dav|s3|az]` and appropriate 
      [configuration and credentials](#storage-configuration).
   - `--concurrency=3` with an appropriate concurrency value for your bandwidth
     and processing power (3 is default)
   - `--since=yyyy-mm-dd` _[optional]_ - only mirror content added after the
     date specified
4. Wait while the mirror process completes. If you want to abort, just `Ctrl+C`
   the process and whatever content has been mirrored so far can be used as-is
   or resumed later.
5. Ensure that the URLs added to the data files are publicly accessible by 
   doing a spot-check on some of them. 
6. Once the mirror is complete (or partially complete if you're only doing a
   partial mirror) and some URLs have been eyeballed, `git commit` your
   changes to the `unreal-archive-data` repository and push to your fork,
   then use GitHub to open a Pull Request to the main repository `master`
   branch. 


## Storage Configuration

A storage backend must be configured in order to store content during indexing.

When interrogating, mirroring or downloading content, no store needs to be 
specified, the default no-op store (`--store=nop`) will be used. 

Environment variables may be used for configuration, eg.: replace 
`--s3-key-images=key` with `S3_KEY_IMAGES=key`.

### HTTP/DAV

This is mostly for testing purposes, but in some circumstances may be useful.

- `--store=dav`
  - `--store-[images|attachments|content]=dav`
- `--dav-url=http://hostname/path/`
  - `--dav-url-[images|attachments|content]=...`

### S3 Bucket Storage

Supports S3-compatible storage implementations.

- `--store=s3`
  - `--store-[images|attachments|content]=bs3`
- `--s3-key=key-id`
  - `--s3-key-[images|attachments|content]=key-id`
- `--s3-secret=secret`
  - `--s3-secret-[images|attachments|content]=secret`
- `--s3-bucket=bucket-id`
  - `--s3-bucket-[images|attachments|content]=bucket-id`
- `--s3-endpoint=https://s3.amazonaws.com/`
  -  (provide the root of your storage API)
  - `--s3-endpoint-[images|attachments|content]=https://s3.amazonaws.com/`
- `--s3-url=https://__BUCKET__.s3.eu-west-2.amazonaws.com/__NAME__`
  -  (provide the public URL of your storage bucket in the appropriate region. 
     `__BUCKET__` and `__NAME__`  will be replaced by the bucket and uploaded
     filenames respectively)

Note: Amazon S3 bucket policy to allow public downloads:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "PublicRead",
            "Effect": "Allow",
            "Principal": "*",
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::testing-unreal-archive-mirror/*"
        }
    ]
}
```

### Azure Blob Storage

- `--store=az`
  - `--store-[images|attachments|content]=az`
- `--az-acc=storage-account-name`
  - `--az-acc-[images|attachments|content]=storage-account-name`
- `--az-container`
  - `--az-container-[images|attachments|content]=container-name`
- `--az-sas`
  - `--az-sas-[images|attachments|content]="shared-access-signature"`
- `--az-endpoint` (Optional, default="blob.core.windows.net")
  - `--az-endpoint-[images|attachments|content]=endpoint-suffix`

Note: The shared access signature (SAS) should be created with full permissions to the specified container.
Use double quotes around the SAS when specifying it via the command line.
