#!/bin/bash
set -xe

./script/authoring-assistant.sh config=test-mock

python3 -m venv .venv
.venv/bin/python -m pip install -r script/requirements.txt \
  --quiet --disable-pip-version-check
.venv/bin/python script/generate-charts.py test-mock # degenerate but useful to validate script
