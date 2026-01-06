#!/bin/bash
set -xe

if [[ $OSTYPE == 'darwin'* ]]; then
  export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-22.0.2.jdk/Contents/Home"
fi

python3 ./script/scigen-gen/scigen-gen.py
mvn --batch-mode clean package
