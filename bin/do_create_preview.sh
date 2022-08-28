#!/usr/bin/env bash
set -e
set -o pipefail
HOST=$1
INPUT_FN=$2
OUTPUT_FN="${INPUT_FN}.png"
curl -u ${SPRING_TIPS_BITES_USERNAME}:${SPRING_TIPS_BITES_PASSWORD} -v  -XPOST -H "Content-Type: application/octet-stream"  \
  --data-binary @"${INPUT_FN}" http://${HOST}/tips/previews --output "${OUTPUT_FN}"
open $OUTPUT_FN
