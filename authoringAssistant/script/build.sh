#!/bin/bash
# Build the project package with maven.
set -xe

# Ensure JAVA_HOME is set correctly
if [[ $OSTYPE == 'darwin'* ]]; then
  export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-22.0.2.jdk/Contents/Home"
fi
mvn --version

mvn --batch-mode clean package
