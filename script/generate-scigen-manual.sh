#!/bin/bash
set -xe

RAW_SCIGEN_DIR="./testCases/scigen-SuggestionAgent-raw/"
TARGET_DIR="./testCases/scigen-manual-2/"
RANDOM_SEED=2025 
TARGET_SIZE=20

mkdir -p "$TARGET_DIR"

# Find all json files, shuffle them and take up to target size
find "$RAW_SCIGEN_DIR" -maxdepth 1 -name "*.json" | \
awk -v RANDOM_SEED="$RANDOM_SEED" 'BEGIN {srand(RANDOM_SEED)} {print rand() "\t" $0}' | \
sort -k1,1n | \
cut -f2- | \
head -n "$TARGET_SIZE" | \
while read -r json_path; do
    
    # Copy json then .fld file
    cp "$json_path" "$TARGET_DIR/"
    fld_path="${json_path%.json}.fld"
    cp "$fld_path" "$TARGET_DIR/"
    
done
