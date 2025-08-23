#!/bin/bash
# Build the project package with maven.
set -xe

yarn bundle-website authoring-assistant
yarn website-test authoring-assistant
yarn bundle-website ar6-wg1-spm
yarn website-test ar6-wg1-spm
