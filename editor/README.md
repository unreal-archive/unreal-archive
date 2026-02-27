# Unreal Archive Editor

A simple JavaFX-based GUI editor for browsing and editing Unreal Archive content entities.

## Prerequisites

- Java 21 or higher.
- A local clone of the [unreal-archive-data](https://github.com/unreal-archive/unreal-archive-data) repository.

## Getting Started

### 1. Clone the Data Repository
The editor works by modifying YAML files in the `unreal-archive-data` repository. 

```bash
git clone https://github.com/unreal-archive/unreal-archive-data.git
```

By default, the editor looks for a directory named `unreal-archive-data` in the current working directory. It is recommended to clone it into the root of this project.

### 2. Building

The editor can be built as a standalone modular Java image using `jlink`.

#### Linux / macOS
```bash
./gradlew :editor:jlink
```

#### Windows
```cmd
gradlew.bat :editor:jlink
```

The resulting build will be located in `editor/build/unreal-archive-editor/`.

To create a distributable tarball (Linux):
```bash
./gradlew :editor:jlinkTar
```
This produces `editor/build/unreal-archive-editor-bin.tgz`.

### 3. Running

#### Using Gradle
The easiest way to run the editor during development:
```bash
./gradlew :editor:run
```

#### Running the Build
If you have built the editor using `jlink`:
```bash
# Linux / macOS
./editor/build/unreal-archive-editor/bin/editor

# Windows
editor\build\unreal-archive-editor\bin\editor.bat
```

**Note:** If your `unreal-archive-data` folder is not in the default location, you can pass the path via an argument:
`--content-path=/path/to/unreal-archive-data`

## Usage

The editor provides several tabs for different types of content:

- **Authors**: Browse all defined authors. Select an author from the list to edit their aliases, links, and other text items.
- **Content**: Search for and edit "Addons" (Maps, Skins, Models, Voices, Mutators, etc.).
    - Use the filter panel on the left to find content by fields like name, author, or game (* wildcard search supported).
    - Alternatively, enter a full or partial hash in the "Edit Hash" field at the top of the editor area.
- **GameTypes**: Browse gametypes by selecting a Game and then the gametype to edit..
- **Managed & Collections**: These sections are currently placeholders and do not yet support editing through the GUI.

### Limitations
- **No Creation**: The editor currently only supports editing *existing* content. It cannot be used to create new authors, addons, or gametypes from scratch.
- **No validation**: Nothing is validated, incorrect or invalid data may be saved which breaks content loading.

## Saving & Contributing Changes

When you click **Save** in the editor, it writes the updated entity directly to the corresponding `.yml` file in your local `unreal-archive-data` clone.

To contribute your changes back to the main Unreal Archive project:
1. Create a new branch for your changes.
2. Commit and Push the branch to your fork on GitHub.
3. Open a **Pull Request** against the main [unreal-archive-data](https://github.com/unreal-archive/unreal-archive-data) repository.
