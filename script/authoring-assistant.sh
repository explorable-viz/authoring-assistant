#!/bin/bash
set -xe

# Ensure JAVA_HOME is set correctly
if [[ $OSTYPE == 'darwin'* ]]; then
  export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-22.0.2.jdk/Contents/Home"
fi
java --version
command="java --enable-preview -Djava.util.logging.config.file=logging.properties -jar target/authoringAssistant-0.1-jar-with-dependencies.jar $1"
output=$(eval "$command")
