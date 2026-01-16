#!/bin/bash
set -xe

./script/authoring-assistant.sh config=test

python3 -m pip install --user -r script/requirements.txt
python3 script/generate-charts.py test
