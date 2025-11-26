#!/bin/bash
set -xe


SOURCE_DIR="${1:-./testCases/scigen-SuggestionAgent-raw/}"
TARGET_DIR="${2:-./testCases/scigen-manual-2/}"
TARGET_SIZE="${3:-20}"
RANDOM_SEED="${4:-2025}"


mkdir -p "$TARGET_DIR"

# Find all json files, shuffle them and take up to target size
find "$SOURCE_DIR" -maxdepth 1 -name "*.json" | \
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
