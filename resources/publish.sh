#!/bin/bash

###############################################################################
## Simple artefact publication script
##
## Usage:
##
##   publish.sh <publish-url> <file1[, file2, ...]>
##
##     publish-url: the url to publish artefacts to, must be a DAV-enabled HTTP
##                  service, artefacts will be PUT to this url
##     files      : collection of files to publish
##
## Environment Variables:
##
##   ARTEFACTS_USER     : basic auth username, if required
##   ARTEFACTS_PASSWORD : basic auth password, if required
##   TRAVIS_BUILD_NUMBER: artefact build number, will be appended to publish-
##                        url, or SNAPSHOT will be used if not set
##
###############################################################################

set -e

if [ -z ${TRAVIS_BUILD_NUMBER+x} ]; then
	BUILD="SNAPSHOT"
else
	BUILD=${TRAVIS_BUILD_NUMBER}
fi

if [ -z ${ARTEFACTS_USER+x} ]; then
  USER=""
else
	USER="-u ${ARTEFACTS_USER}:${ARTEFACTS_PASSWORD}"
fi

for VERSION in ${BUILD} "latest"
do
  for ARTEFACT in "${@:2}"
  do
    FNAME="$(basename ${ARTEFACT})"
		REMOTE_PATH="${1}/${VERSION}/${FNAME}"

		echo "Publishing ${REMOTE_PATH}"

		curl --fail ${USER} --upload-file ${ARTEFACT} "${REMOTE_PATH}"
	done
done
