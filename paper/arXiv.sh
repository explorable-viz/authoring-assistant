#!/bin/bash
set -xe

NAME=arXiv.zip
ARCHIVE="../$NAME"

. build.sh

zip -r - . > $ARCHIVE
zip -d $ARCHIVE *.pdf \*.{DS_Store,gitignore}
