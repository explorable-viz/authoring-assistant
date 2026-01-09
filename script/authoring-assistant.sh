#!/bin/bash
set -xe

# Ensure JAVA_HOME is set correctly
if [[ $OSTYPE == 'darwin'* ]]; then
  export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-22.0.2.jdk/Contents/Home"
fi
java --version
command="java --enable-preview -jar target/authoringAssistant-0.1-jar-with-dependencies.jar $1 $2 $3"
output=$(eval "$command")
