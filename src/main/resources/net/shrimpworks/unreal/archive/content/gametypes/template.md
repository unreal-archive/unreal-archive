## Game Type Additional Information

Feel free to add any additional write-up, documentation, installation help, 
etc. to this document. It's a regular 
[Markdown](https://www.markdownguide.org/cheat-sheet/) document, and will be
formatted and rendered as HTML, along with the game type metadata from the
accompanying YAML file.

## Next Steps

A template `gametype.yml` file has been generated, you can populate it with all
the relevant information.

### Graphics

To use a custom title graphic on the games browser and at the top of the 
gametype information page, place a 800 x 204 pixel image in any format in the 
game type directory and set the `titleImage` option in `gametype.yml` to the 
image file name.

To use a custom graphic at the top of the information page, place a 1200 x 150
pixel image in the game type directory and set the `bannerImage` option in 
`gametype.yml` to the image file name.

If screenshots or other graphics are to be included, they should be placed into
a directory called `gallery`. This directory will be turned into a thumbnail
gallery on the final page.

### Releases and Files

The downloads for a mod or game type are broken into releases, and releases
are broken into files. For example, the latest release of a game type may 
include multiple formats of files - a UMOD, a setup EXE, and a plain ZIP
archive. All of these files may contain the same content, but are variations
of the same release.

