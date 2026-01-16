#!/bin/bash
set -xe

./script/authoring-assistant.sh config=test-mock

python3 -m pip install --user -r script/requirements.txt --quiet --disable-pip-version-check
python3 script/generate-charts.py test-mock # degenerate but useful to validate script
