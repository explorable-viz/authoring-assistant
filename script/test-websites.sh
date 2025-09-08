#!/bin/bash
# Build the project package with maven.
set -xe

yarn bundle-website authoring-assistant
yarn test-website authoring-assistant
