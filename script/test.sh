#!/bin/bash
set -xe

./script/authoring-assistant.sh config=test

python3 -m pip install --user -r script/requirements.txt --disable-pip-version-check
python3 script/generate-charts.py test
