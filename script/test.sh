#!/bin/bash
set -xe

./script/authoring-assistant.sh config=test

python3 -m pip install -r script/requirements.txt --quiet --disable-pip-version-check
python3 script/generate-charts.py test
