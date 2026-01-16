#!/bin/bash
set -xe

./script/authoring-assistant.sh test-case-folder=$1 authoring-agent-class=authoringassistant.llm.interpretation.OpenAIGpt5Agent

python3 -m pip install --user -r script/requirements.txt
python3 script/generate-charts.py
