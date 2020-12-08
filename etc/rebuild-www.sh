#!/bin/bash

## Triggers a rebuild of the `unreal-archive-data` project

echo "Triggering www content rebuild"

curl -s -X POST \
   -H "Authorization: Bearer ${DRONE_TOKEN}"
   https://build.shrimpworks.za.net/api/repos/unreal-archive/unreal-archive-data/builds
