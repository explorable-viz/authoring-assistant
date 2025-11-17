#!/bin/bash
set -xe

NAME=preprint.zip
ARCHIVE="../$NAME"

. build.sh

zip -r - . > $ARCHIVE
zip -d $ARCHIVE \*.{DS_Store,gitignore}
zip -d $ARCHIVE *.pdf .git arXiv.sh build.bat build.sh
