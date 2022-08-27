#!/usr/bin/env bash
set -e
set -o pipefail
INPUT_FN=$1
OUTPUT_FN="${INPUT_FN}.jpg"
curl -v -F "tip=@${INPUT_FN}" http://35.223.119.51/tips/preview --output "${OUTPUT_FN}"
open $OUTPUT_FN