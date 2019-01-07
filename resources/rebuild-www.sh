#!/bin/sh

## Triggers a rebuild of the `unreal-archive-data` project

BODY='{
  "request": {
    "branch":"master"
  }
}'

echo "Triggering www content rebuild"

curl -s -X POST \
   -H "Content-Type: application/json" \
   -H "Accept: application/json" \
   -H "Travis-API-Version: 3" \
   -H "Authorization: token ${TRAVIS_TOKEN}" \
   -d "${BODY}" \
   https://api.travis-ci.org/repo/unreal-archive%2Funreal-archive-data/requests
