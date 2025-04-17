#!/bin/bash
set -xe

#create a virtual-env for charts
python3 -m venv .venv-chart

source .venv-chart/bin/activate
pip install pandas seaborn matplotlib
python ./script/chart-generation.py

#delete the virtual-env
rm -r .venv-chart
