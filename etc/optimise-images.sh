#!/bin/bash

if [ -z "$1" ]; then
  echo "Path argument is expected"
  exit 1;
fi

if [[ $1 == "new" ]]; then
  git diff --name-only --diff-filter=A HEAD "*.jpg" | xargs -n 1 jpegoptim -s -p -P -m85
  git diff --name-only --diff-filter=A HEAD "*.png" | xargs -n 1 pngcrush -brute -reduce -oldtimestamp -ow
else
  find $1 -name "*.jpg" -print0 | xargs -0 jpegoptim -s -p -P -m85
  find $1 -name "*.png" -print0 | xargs -0 pngcrush -brute -reduce -oldtimestamp -ow
fi
