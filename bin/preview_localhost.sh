#!/usr/bin/env bash
set -e
set -o pipefail
INPUT_FN=$1
OUTPUT_FN="${INPUT_FN}.jpg"
curl -v -F "tip=@${INPUT_FN}" http://localhost:8080/tips/preview --output "${OUTPUT_FN}"
open $OUTPUT_FN